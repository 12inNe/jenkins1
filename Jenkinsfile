pipeline {
    agent any
    environment {
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
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

        stage('Wait for Kafka Broker') {
            steps {
                script {
                    def maxRetries = 30
                    def retryCount = 0
                    def brokerReady = false

                    while (retryCount < maxRetries && !brokerReady) {
                        try {
                            sh '''
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T broker bash -c "echo 'Checking if broker is ready...'"
                            '''
                            brokerReady = true
                            echo "Broker is ready!"
                        } catch (Exception e) {
                            echo "Broker not ready yet, waiting... (attempt ${retryCount + 1}/${maxRetries})"
                            sleep(10)
                            retryCount++
                        }
                    }

                    if (!brokerReady) {
                        error("Broker failed to start after ${maxRetries} attempts")
                    }
                }
            }
        }

        stage('Create /tmp/client.properties in broker') {
            steps {
                sh '''
                echo "Creating client.properties file..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "cat > /tmp/client.properties << 'EOF'
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\\"admin\\" password=\\"admin-secret\\";
EOF"

                echo "Verifying client.properties was created..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "cat /tmp/client.properties"
                '''
            }
        }

        stage('Test Kafka Connection') {
            steps {
                sh '''
                echo "Testing Kafka connection..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "
                    export KAFKA_OPTS=''
                    unset JMX_PORT
                    unset KAFKA_JMX_OPTS
                    kafka-broker-api-versions --bootstrap-server localhost:9092 --command-config /tmp/client.properties
                "
                '''
            }
        }

        stage('List Kafka Topics') {
            steps {
                sh '''
                echo "Listing Kafka topics..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "
                    export KAFKA_OPTS=''
                    unset JMX_PORT
                    unset KAFKA_JMX_OPTS
                    kafka-topics --list --bootstrap-server localhost:9092 --command-config /tmp/client.properties
                "
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
            '''
        }
    }
}
