pipeline {
    agent any
    environment {
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
        SCHEMA_REGISTRY_URL = 'http://localhost:8081'
    }
    
    parameters {
        choice(
            name: 'SCHEMA_REGISTRY_ACTION',
            choices: ['LIST_SUBJECTS', 'CREATE_SCHEMA', 'DELETE_SUBJECT', 'GET_SCHEMA', 'UPDATE_SCHEMA'],
            description: 'Select Schema Registry action to perform'
        )
        string(
            name: 'SUBJECT_NAME',
            defaultValue: '',
            description: 'Schema subject name (required for CREATE/DELETE/GET/UPDATE operations)'
        )
        text(
            name: 'SCHEMA_DEFINITION',
            defaultValue: '',
            description: 'JSON schema definition (required for CREATE/UPDATE operations)'
        )
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

        stage('Wait for Schema Registry') {
            steps {
                script {
                    def maxRetries = 30
                    def retryCount = 0
                    def schemaRegistryReady = false

                    while (retryCount < maxRetries && !schemaRegistryReady) {
                        try {
                            sh '''
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry bash -c "curl -s http://localhost:8081/subjects || exit 1"
                            '''
                            schemaRegistryReady = true
                            echo "Schema Registry is ready!"
                        } catch (Exception e) {
                            echo "Schema Registry not ready yet, waiting... (attempt ${retryCount + 1}/${maxRetries})"
                            sleep(10)
                            retryCount++
                        }
                    }

                    if (!schemaRegistryReady) {
                        error("Schema Registry failed to start after ${maxRetries} attempts")
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

        stage('Test Schema Registry Connection') {
            steps {
                            sh '''
                            echo "Testing Schema Registry connection..."
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry bash -c "
                                curl -s ${SCHEMA_REGISTRY_URL}/subjects || echo 'Connection failed'
                            "
                            '''
            }
        }

        stage('Schema Registry Operations') {
            steps {
                script {
                    switch(params.SCHEMA_REGISTRY_ACTION) {
                        case 'LIST_SUBJECTS':
                            sh '''
                            echo "Listing all schema subjects..."
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry bash -c "
                                curl -s ${SCHEMA_REGISTRY_URL}/subjects | jq '.'
                            "
                            '''
                            break
                            
                        case 'CREATE_SCHEMA':
                            if (!params.SUBJECT_NAME || !params.SCHEMA_DEFINITION) {
                                error("SUBJECT_NAME and SCHEMA_DEFINITION are required for CREATE_SCHEMA operation")
                            }
                            sh '''
                            echo "Creating schema for subject: ${SUBJECT_NAME}"
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry bash -c "
                                curl -s \
                                -X POST \
                                -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
                                -d '{\"schema\": \"${SCHEMA_DEFINITION}\"}' \
                                ${SCHEMA_REGISTRY_URL}/subjects/${SUBJECT_NAME}/versions | jq '.'
                            "
                            '''
                            break
                            
                        case 'DELETE_SUBJECT':
                            if (!params.SUBJECT_NAME) {
                                error("SUBJECT_NAME is required for DELETE_SUBJECT operation")
                            }
                            sh '''
                            echo "Deleting subject: ${SUBJECT_NAME}"
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry bash -c "
                                curl -s \
                                -X DELETE \
                                ${SCHEMA_REGISTRY_URL}/subjects/${SUBJECT_NAME} | jq '.'
                            "
                            '''
                            break
                            
                        case 'GET_SCHEMA':
                            if (!params.SUBJECT_NAME) {
                                error("SUBJECT_NAME is required for GET_SCHEMA operation")
                            }
                            sh '''
                            echo "Getting latest schema for subject: ${SUBJECT_NAME}"
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry bash -c "
                                curl -s \
                                ${SCHEMA_REGISTRY_URL}/subjects/${SUBJECT_NAME}/versions/latest | jq '.'
                            "
                            '''
                            break
                            
                        case 'UPDATE_SCHEMA':
                            if (!params.SUBJECT_NAME || !params.SCHEMA_DEFINITION) {
                                error("SUBJECT_NAME and SCHEMA_DEFINITION are required for UPDATE_SCHEMA operation")
                            }
                            sh '''
                            echo "Updating schema for subject: ${SUBJECT_NAME}"
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry bash -c "
                                curl -s \
                                -X POST \
                                -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
                                -d '{\"schema\": \"${SCHEMA_DEFINITION}\"}' \
                                ${SCHEMA_REGISTRY_URL}/subjects/${SUBJECT_NAME}/versions | jq '.'
                            "
                            '''
                            break
                            
                        default:
                            echo "No specific Schema Registry action selected"
                    }
                }
            }
        }

        stage('Schema Registry Health Check') {
            steps {
                sh '''
                echo "Checking Schema Registry health and configuration..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    echo 'Schema Registry Mode:'
                    curl -s ${SCHEMA_REGISTRY_URL}/mode | jq '.'
                    
                    echo 'Schema Registry Config:'
                    curl -s ${SCHEMA_REGISTRY_URL}/config | jq '.'
                    
                    echo 'Schema Registry Subjects Count:'
                    curl -s ${SCHEMA_REGISTRY_URL}/subjects | jq 'length'
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
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml logs --tail=50 schema-registry || true
            '''
        }
        success {
            echo "Pipeline completed successfully! Schema Registry operations completed."
        }
    }
}