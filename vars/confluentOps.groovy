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
        RESPONSE=\\\$(curl -s http://localhost:8081/subjects)
        echo 'Raw subjects response:'
        echo \"\\\$RESPONSE\"
        if [ \"\\\$RESPONSE\" = '[]' ]; then
            echo 'No existing subjects found'
        else
            echo 'Subjects found in registry'
        fi
    "
    """
}

def registerAvroSchema(composeDir, topicName, schemaContent) {
    sh """
    echo "Registering Avro schema for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c 'curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \\
        --data '"'"'{
            "schema": "${schemaContent}"
        }'"'"' \\
        http://localhost:8081/subjects/${topicName}-value/versions'
    """
}

def verifySchemaRegistration(composeDir, topicName) {
    sh """
    echo "Verifying schema registration for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c 'echo "Schema registration response:" && \\
        curl -s http://localhost:8081/subjects/${topicName}-value/versions/latest && \\
        echo "" && \\
        echo "Schema verification completed"'
    """
}

def createTestData(composeDir, testData) {
    sh """
    echo "Creating test data file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'cat > /tmp/test-data.json << "DATA_EOF"
${testData}
DATA_EOF'

    echo "Verifying test data file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "cat /tmp/test-data.json"
    """
}

def produceMessagesWithSchema(composeDir, topicName, schemaRegistryUrl = "http://schema-registry:8081") {
    sh """
    echo "Producing messages with JSON Schema for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'export KAFKA_OPTS="" && \\
        export JMX_PORT="" && \\
        export KAFKA_JMX_OPTS="" && \\
        unset JMX_PORT && \\
        unset KAFKA_JMX_OPTS && \\
        cat > /tmp/producer.properties << "PRODUCER_EOF"
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
schema.registry.url=${schemaRegistryUrl}
PRODUCER_EOF
        echo "Producer config created:" && \\
        cat /tmp/producer.properties && \\
        echo "Producing messages..." && \\
        kafka-console-producer --bootstrap-server localhost:9092 --topic ${topicName} --producer.config /tmp/producer.properties < /tmp/test-data.json'
    """
}

def consumeMessagesWithSchema(composeDir, topicName, schemaRegistryUrl = "http://schema-registry:8081", consumerGroup = "test-consumer-group") {
    sh """
    echo "Consuming messages with schema validation for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'export KAFKA_OPTS="" && \\
        export JMX_PORT="" && \\
        export KAFKA_JMX_OPTS="" && \\
        unset JMX_PORT && \\
        unset KAFKA_JMX_OPTS && \\
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
        echo "Consumer config created:" && \\
        cat /tmp/consumer.properties && \\
        echo "Consuming messages for 15 seconds..." && \\
        timeout 15s kafka-console-consumer --bootstrap-server localhost:9092 --topic ${topicName} --consumer.config /tmp/consumer.properties --from-beginning || true'
    """
}

def listAllTopics(composeDir) {
    echo "📋 Listing all Kafka topics..."

    def topicsOutput = sh(
        script: """
            docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
            exec -T broker bash -c "
                export KAFKA_OPTS=''
                export JMX_PORT=''
                export KAFKA_JMX_OPTS=''
                unset JMX_PORT
                unset KAFKA_JMX_OPTS
                kafka-topics --list --bootstrap-server localhost:9092 --command-config /tmp/client.properties
            "
        """,
        returnStdout: true
    ).trim()

    def topicsList = topicsOutput.split('\n').findAll { it.trim() != '' }

    return topicsList
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

def deleteTopic(composeDir, topicName) {
    sh """
    echo "Deleting topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "
        export KAFKA_OPTS=''
        export JMX_PORT=''
        export KAFKA_JMX_OPTS=''
        unset JMX_PORT
        unset KAFKA_JMX_OPTS
        kafka-topics --delete --topic ${topicName} --bootstrap-server localhost:9092 --command-config /tmp/client.properties
    "
    """
}

def debugFileContents(composeDir) {
    sh """
    echo "🔍 Debug: Checking all created files..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'echo "=== Client Properties ===" && \\
        cat /tmp/client.properties && \\
        echo "" && \\
        echo "=== Test Data JSON ===" && \\
        cat /tmp/test-data.json && \\
        echo "" && \\
        echo "=== Producer Properties ===" && \\
        cat /tmp/producer.properties && \\
        echo "" && \\
        echo "=== Consumer Properties ===" && \\
        cat /tmp/consumer.properties && \\
        echo "" && \\
        echo "=== File sizes ===" && \\
        ls -la /tmp/*.properties /tmp/*.json'
    """
}

def createKafkaClientConfig(username, password) {
    def securityConfig = ""

    switch(params.SECURITY_PROTOCOL) {
        case 'SASL_PLAINTEXT':
        case 'SASL_SSL':
            securityConfig = """
security.protocol=${params.SECURITY_PROTOCOL}
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${username}" password="${password}";
"""
            break
        default:
            securityConfig = """
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${username}" password="${password}";
"""
            break
    }

    sh """
        docker compose --project-directory ${params.COMPOSE_DIR} -f ${params.COMPOSE_DIR}/docker-compose.yml \\
        exec -T broker bash -c 'cat > ${env.CLIENT_CONFIG_FILE} << "EOF"
bootstrap.servers=${params.KAFKA_BOOTSTRAP_SERVER}
${securityConfig}
EOF'
    """
}

def listKafkaTopics() {
    def topicsOutput = sh(
        script: """
            docker compose --project-directory ${params.COMPOSE_DIR} -f ${params.COMPOSE_DIR}/docker-compose.yml \\
            exec -T broker bash -c "
                export KAFKA_OPTS=''
                export JMX_PORT=''
                export KAFKA_JMX_OPTS=''
                unset JMX_PORT
                unset KAFKA_JMX_OPTS
                unset KAFKA_OPTS
                kafka-topics --list --bootstrap-server ${params.KAFKA_BOOTSTRAP_SERVER} --command-config ${env.CLIENT_CONFIG_FILE}
            " 2>/dev/null
        """,
        returnStdout: true
    ).trim()

    def allTopics = topicsOutput.split('\n').findAll { it.trim() != '' && !it.startsWith('WARNING') && !it.contains('FATAL') }
    return params.INCLUDE_INTERNAL ? allTopics : allTopics.findAll { !it.startsWith('_') }
}

def saveTopicsToFile(topics) {
    def timestamp = new Date().format('yyyy-MM-dd HH:mm:ss')
    def textContent = """# Kafka Topics List
# Generated: ${timestamp}
# Total topics: ${topics.size()}
# Include internal: ${params.INCLUDE_INTERNAL}
# Bootstrap server: ${params.KAFKA_BOOTSTRAP_SERVER}
# Security protocol: ${params.SECURITY_PROTOCOL}

"""
    topics.each { topic ->
        textContent += "${topic}\n"
    }

    writeFile file: env.TOPICS_LIST_FILE, text: textContent
}

def cleanupClientConfig() {
    try {
        sh """
            docker compose --project-directory ${params.COMPOSE_DIR} -f ${params.COMPOSE_DIR}/docker-compose.yml \\
            exec -T broker bash -c "rm -f ${env.CLIENT_CONFIG_FILE}" 2>/dev/null || true
        """
    } catch (Exception e) {
        // Ignore cleanup errors
    }
}

def describeKafkaTopic(topicName) {
    try {
        def describeOutput = sh(
            script: """
                docker compose --project-directory ${params.COMPOSE_DIR} -f ${params.COMPOSE_DIR}/docker-compose.yml \\
                exec -T broker bash -c "
                    export KAFKA_OPTS=''
                    export JMX_PORT=''
                    export KAFKA_JMX_OPTS=''
                    unset JMX_PORT
                    unset KAFKA_JMX_OPTS
                    unset KAFKA_OPTS
                    kafka-topics --describe --topic ${topicName} --bootstrap-server ${params.KAFKA_BOOTSTRAP_SERVER} --command-config ${env.CLIENT_CONFIG_FILE}
                " 2>/dev/null
            """,
            returnStdout: true
        ).trim()

        return describeOutput
    } catch (Exception e) {
        return "ERROR: Failed to describe topic '${topicName}' - ${e.getMessage()}"
    }
}

def saveTopicDescriptionsToFile(topicDescriptions) {
    def timestamp = new Date().format('yyyy-MM-dd HH:mm:ss')
    def textContent = """# Kafka Topics Description
# Generated: ${timestamp}
# Total topics described: ${topicDescriptions.size()}
# Bootstrap server: ${params.KAFKA_BOOTSTRAP_SERVER}
# Security protocol: ${params.SECURITY_PROTOCOL}
# Specific topic: ${params.TOPIC_NAME ?: 'All topics'}

"""

    topicDescriptions.each { topicName, description ->
        textContent += """
================================================================================
Topic: ${topicName}
================================================================================
${description}

"""
    }

    writeFile file: env.TOPICS_DESCRIBE_FILE, text: textContent
}

def createTopic(topicName, partitions = 3, replicationFactor = 1) {
    try {
        def createOutput = sh(
            script: """
                docker compose --project-directory '${params.COMPOSE_DIR}' -f '${params.COMPOSE_DIR}/docker-compose.yml' \\
                exec -T broker bash -c '
                    set -e
                    unset JMX_PORT KAFKA_JMX_OPTS KAFKA_OPTS
                    kafka-topics --create \\
                        --if-not-exists \\
                        --topic "${topicName}" \\
                        --bootstrap-server ${params.KAFKA_BOOTSTRAP_SERVER} \\
                        --command-config ${env.CLIENT_CONFIG_FILE} \\
                        --partitions ${partitions} \\
                        --replication-factor ${replicationFactor}
                '
            """,
            returnStdout: true
        ).trim()

        return "Topic '${topicName}' created or already exists.\n${createOutput}"
    } catch (Exception e) {
        return "ERROR: Failed to create topic '${topicName}' - ${e.getMessage()}"
    }
}

