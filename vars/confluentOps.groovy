
def checkServices(composeDir) {
    sh """
    echo "Checking if compose directory exists..."
    ls -la ${composeDir}

    echo "Checking if docker-compose.yml exists..."
    ls -la ${composeDir}/docker-compose.yml

    echo "Checking running containers..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml ps
    """
}

def waitForServices(composeDir, maxRetries = 30) {
    script {
        def retryCount = 0
        def servicesReady = false

        while (retryCount < maxRetries && !servicesReady) {
            try {
                sh """
                echo "Checking Kafka Broker..."
                docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
                exec -T broker bash -c "echo 'Broker is responsive'"

                echo "Checking Schema Registry..."
                docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
                exec -T schema-registry curl -f -s http://localhost:8081/subjects > /dev/null
                """
                servicesReady = true
                echo "All services are ready!"
            } catch (Exception e) {
                echo "Services not ready yet, waiting... (attempt ${retryCount + 1}/${maxRetries})"
                sleep(10)
                retryCount++
            }
        }

        if (!servicesReady) {
            error("Services failed to start after ${maxRetries} attempts")
        }
    }
}

def createKafkaClientConfig(composeDir) {
    sh """
    echo "Creating client.properties file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "cat > /tmp/client.properties << 'EOF'
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\\"admin\\" password=\\"admin-secret\\";
EOF"

    echo "Verifying client.properties..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "cat /tmp/client.properties"
    """
}

def schemaRegistryHealthCheck(composeDir) {
    sh """
    echo "📊 Schema Registry Health Check..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c "
        echo 'Registry URL: http://localhost:8081'
        echo 'Mode:' && curl -s http://localhost:8081/mode
        echo 'Compatibility Level:' && curl -s http://localhost:8081/config
        echo 'Current Subjects:' && curl -s http://localhost:8081/subjects
    "
    """
}

def listSchemaSubjects(composeDir) {
    sh """
    echo "📋 Listing existing schema subjects..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c "
        RESPONSE=\\$(curl -s http://localhost:8081/subjects)
        echo 'Raw subjects response:'
        echo \"\\$RESPONSE\"
        if [ \"\\$RESPONSE\" = '[]' ]; then
            echo 'No existing subjects found'
        else
            echo 'Subjects found in registry'
        fi
    "
    """
}

def createTopic(composeDir, topicName, partitions = 3, replicationFactor = 1) {
    sh """
    echo "Creating test topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "
        export KAFKA_OPTS=''
        export JMX_PORT=''
        export KAFKA_JMX_OPTS=''
        unset JMX_PORT
        unset KAFKA_JMX_OPTS
        kafka-topics --create --topic ${topicName} --bootstrap-server localhost:9092 --command-config /tmp/client.properties --partitions ${partitions} --replication-factor ${replicationFactor} --if-not-exists
    "
    """
}

def registerAvroSchema(composeDir, topicName, schemaContent) {
    sh """
    echo "Registering Avro schema for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c '
        curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \\
        --data '"'"'{
            "schema": "${schemaContent}"
        }'"'"' \\
        http://localhost:8081/subjects/${topicName}-value/versions
    '
    """
}

def verifySchemaRegistration(composeDir, topicName) {
    sh """
    echo "Verifying schema registration for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c '
        echo "Schema registration response:"
        curl -s http://localhost:8081/subjects/${topicName}-value/versions/latest
        echo ""
        echo "Schema verification completed"
    '
    """
}

def createTestData(composeDir, testData) {
    sh """
    echo "Creating test data file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c '
        cat > /tmp/test-data.json << "DATA_EOF"
${testData}
DATA_EOF
    '

    echo "Verifying test data file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "cat /tmp/test-data.json"
    """
}

def produceMessagesWithSchema(composeDir, topicName, schemaRegistryUrl = "http://schema-registry:8081") {
    sh """
    echo "Producing messages with JSON Schema for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c '
        export KAFKA_OPTS=""
        export JMX_PORT=""
        export KAFKA_JMX_OPTS=""
        unset JMX_PORT
        unset KAFKA_JMX_OPTS

        # Create producer config with schema registry
        cat > /tmp/producer.properties << "PRODUCER_EOF"
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
schema.registry.url=${schemaRegistryUrl}
PRODUCER_EOF

        echo "Producer config created:"
        cat /tmp/producer.properties

        echo "Producing messages..."
        kafka-console-producer --bootstrap-server localhost:9092 --topic ${topicName} --producer.config /tmp/producer.properties < /tmp/test-data.json
    '
    """
}

def consumeMessagesWithSchema(composeDir, topicName, schemaRegistryUrl = "http://schema-registry:8081", consumerGroup = "test-consumer-group") {
    sh """
    echo "Consuming messages with schema validation for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c '
        export KAFKA_OPTS=""
        export JMX_PORT=""
        export KAFKA_JMX_OPTS=""
        unset JMX_PORT
        unset KAFKA_JMX_OPTS

        # Create consumer config with schema registry
        cat > /tmp/consumer.properties << "CONSUMER_EOF"
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
value.deserializer=io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer
schema.registry.url=${schemaRegistryUrl}
group.id=${consumerGroup}
auto.offset.reset=earliest
CONSUMER_EOF

        echo "Consumer config created:"
        cat /tmp/consumer.properties

        # Consume messages (with timeout)
        echo "Consuming messages for 15 seconds..."
        timeout 15s kafka-console-consumer --bootstrap-server localhost:9092 --topic ${topicName} --consumer.config /tmp/consumer.properties --from-beginning || true
    '
    """
}

def listAllTopics(composeDir) {
    sh """
    echo "📋 Listing all Kafka topics..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "
        export KAFKA_OPTS=''
        export JMX_PORT=''
        export KAFKA_JMX_OPTS=''
        unset JMX_PORT
        unset KAFKA_JMX_OPTS
        kafka-topics --list --bootstrap-server localhost:9092 --command-config /tmp/client.properties
    "
    """
}

def getTopicDetails(composeDir, topicName) {
    sh """
    echo "📊 Topic details for ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "
        export KAFKA_OPTS=''
        export JMX_PORT=''
        export KAFKA_JMX_OPTS=''
        unset JMX_PORT
        unset KAFKA_JMX_OPTS
        kafka-topics --describe --topic ${topicName} --bootstrap-server localhost:9092 --command-config /tmp/client.properties
    "
    """
}

def debugFileContents(composeDir) {
    sh """
    echo "🔍 Debug: Checking all created files..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c '
        echo "=== Client Properties ==="
        cat /tmp/client.properties
        echo ""
        echo "=== Test Data JSON ==="
        cat /tmp/test-data.json
        echo ""
        echo "=== Producer Properties ==="
        cat /tmp/producer.properties
        echo ""
        echo "=== Consumer Properties ==="
        cat /tmp/consumer.properties
        echo ""
        echo "=== File sizes ==="
        ls -la /tmp/*.properties /tmp/*.json
    '
    """
}