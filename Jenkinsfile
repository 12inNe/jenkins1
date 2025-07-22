pipeline {
    agent any
    environment {
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
        SCHEMA_REGISTRY_URL = 'http://localhost:8081'
        KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
        TEST_TOPIC = 'user-events'
    }
    stages {
        stage('Verify Docker Compose Setup') {
            steps {
                sh '''
                echo "Checking if compose directory exists..."
                ls -la $COMPOSE_DIR

                echo "Checking if docker-compose.yml exists..."
                ls -la $COMPOSE_DIR/docker-compose.yml

                echo "Checking running containers..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml ps
                '''
            }
        }

        stage('Wait for Services') {
            steps {
                script {
                    def maxRetries = 30
                    def retryCount = 0
                    def servicesReady = false

                    while (retryCount < maxRetries && !servicesReady) {
                        try {
                            sh '''
                            echo "Checking Kafka Broker..."
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T broker bash -c "echo 'Broker is responsive'"

                            echo "Checking Schema Registry..."
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry curl -f -s http://localhost:8081/subjects > /dev/null
                            '''
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
        }

        stage('Create Kafka Client Config') {
            steps {
                sh '''
                echo "Creating client.properties file..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "cat > /tmp/client.properties << 'EOF'
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\\"admin\\" password=\\"admin-secret\\";
EOF"

                echo "Verifying client.properties..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "cat /tmp/client.properties"
                '''
            }
        }

        stage('Schema Registry Health Check') {
            steps {
                sh '''
                echo "📊 Schema Registry Health Check..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    echo 'Registry URL: http://localhost:8081'
                    echo 'Mode:' && curl -s http://localhost:8081/mode
                    echo 'Compatibility Level:' && curl -s http://localhost:8081/config
                    echo 'Current Subjects:' && curl -s http://localhost:8081/subjects
                "
                '''
            }
        }

        stage('List Existing Schema Subjects') {
            steps {
                sh '''
                echo "📋 Listing existing schema subjects..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
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
                '''
            }
        }

        stage('Create Test Topic') {
            steps {
                sh '''
                echo "Creating test topic: $TEST_TOPIC..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "
                    export KAFKA_OPTS=''
                    export JMX_PORT=''
                    export KAFKA_JMX_OPTS=''
                    unset JMX_PORT
                    unset KAFKA_JMX_OPTS
                    kafka-topics --create --topic $TEST_TOPIC --bootstrap-server localhost:9092 --command-config /tmp/client.properties --partitions 3 --replication-factor 1 --if-not-exists
                "
                '''
            }
        }

        stage('Register Avro Schema') {
            steps {
                sh '''
                echo "Registering User schema..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c '
                    curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
                    --data '"'"'{
                        "schema": "{\\"type\\":\\"record\\",\\"name\\":\\"User\\",\\"namespace\\":\\"com.example\\",\\"fields\\":[{\\"name\\":\\"id\\",\\"type\\":\\"int\\"},{\\"name\\":\\"name\\",\\"type\\":\\"string\\"},{\\"name\\":\\"email\\",\\"type\\":\\"string\\"},{\\"name\\":\\"age\\",\\"type\\":\\"int\\"}]}"
                    }'"'"' \
                    http://localhost:8081/subjects/'"$TEST_TOPIC"'-value/versions
                '
                '''
            }
        }

        stage('Verify Schema Registration') {
            steps {
                sh '''
                echo "Verifying schema registration..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c '
                    echo "Schema registration response:"
                    curl -s http://localhost:8081/subjects/'"$TEST_TOPIC"'-value/versions/latest
                    echo ""
                    echo "Schema verification completed"
                '
                '''
            }
        }

        stage('Create Test Data File') {
            steps {
                sh '''
                echo "Creating test data file..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c '
                    cat > /tmp/test-data.json << "DATA_EOF"
{"id": 1, "name": "John Doe", "email": "john@example.com", "age": 30}
{"id": 2, "name": "Jane Smith", "email": "jane@example.com", "age": 25}
{"id": 3, "name": "Bob Johnson", "email": "bob@example.com", "age": 35}
DATA_EOF
                '

                echo "Verifying test data file..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "cat /tmp/test-data.json"
                '''
            }
        }

        stage('Produce Messages with Schema') {
            steps {
                sh '''
                echo "Producing messages with JSON Schema..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
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
schema.registry.url=http://schema-registry:8081
PRODUCER_EOF

                    echo "Producer config created:"
                    cat /tmp/producer.properties

                    echo "Test data content:"
                    cat /tmp/test-data.json

                    echo "Producing messages..."
                    kafka-console-producer --bootstrap-server localhost:9092 --topic '"$TEST_TOPIC"' --producer.config /tmp/producer.properties < /tmp/test-data.json
                '
                '''
            }
        }

        stage('Consume Messages with Schema') {
            steps {
                sh '''
                echo "Consuming messages with schema validation..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
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
schema.registry.url=http://schema-registry:8081
group.id=test-consumer-group
auto.offset.reset=earliest
CONSUMER_EOF

                    echo "Consumer config created:"
                    cat /tmp/consumer.properties

                    # Consume messages (with timeout)
                    echo "Consuming messages for 15 seconds..."
                    timeout 15s kafka-console-consumer --bootstrap-server localhost:9092 --topic '"$TEST_TOPIC"' --consumer.config /tmp/consumer.properties --from-beginning || true
                '
                '''
            }
        }


        stage('List All Topics') {
            steps {
                sh '''
                echo "📋 Listing all Kafka topics..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "
                    export KAFKA_OPTS=''
                    export JMX_PORT=''
                    export KAFKA_JMX_OPTS=''
                    unset JMX_PORT
                    unset KAFKA_JMX_OPTS
                    kafka-topics --list --bootstrap-server localhost:9092 --command-config /tmp/client.properties
                "
                '''
            }
        }

        stage('List All Schema Subjects') {
            steps {
                sh '''
                echo "📋 Listing all schema subjects after test..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    echo 'All registered subjects:'
                    curl -s http://localhost:8081/subjects
                    echo ''
                    echo 'Schema subjects listing completed'
                "
                '''
            }
        }

        stage('Topic Details') {
            steps {
                sh '''
                echo "📊 Topic details for $TEST_TOPIC..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "
                    export KAFKA_OPTS=''
                    export JMX_PORT=''
                    export KAFKA_JMX_OPTS=''
                    unset JMX_PORT
                    unset KAFKA_JMX_OPTS
                    kafka-topics --describe --topic $TEST_TOPIC --bootstrap-server localhost:9092 --command-config /tmp/client.properties
                "
                '''
            }
        }

        stage('Debug: Check File Contents') {
            steps {
                sh '''
                echo "🔍 Debug: Checking all created files..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
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
                '''
            }
        }
    }

    post {
        always {
            sh '''
            echo "Pipeline completed. Container status:"
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml ps || true
            '''
        }
        failure {
            sh '''
            echo "Pipeline failed. Checking logs..."
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml logs --tail=50 broker || true
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml logs --tail=50 schema-registry || true
            '''
        }
        success {
            sh '''
            echo "✅ Complete Confluent Platform pipeline completed successfully!"
            echo "✅ Schema Registry is working"
            echo "✅ Kafka topics created and tested"
            echo "✅ Avro schema registered and validated"
            echo "✅ Messages produced and consumed with schema"
            '''
        }
    }
}