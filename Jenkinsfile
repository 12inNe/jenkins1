pipeline {
    agent any
    environment {
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
        SCHEMA_REGISTRY_URL = 'http://localhost:8081'
        KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
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

        stage('Check Services') {
            steps {
                script {
                    try {
                        sh '''
                        echo "Checking Schema Registry connectivity..."
                        docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                        exec -T schema-registry curl -f -s http://localhost:8081/subjects > /dev/null
                        echo "✓ Schema Registry is accessible"

                        echo "Checking Kafka broker connectivity..."
                        docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                        exec -T broker kafka-topics --list --bootstrap-server SASL_PLAINTEXT://broker:29093 \
                        --command-config <(echo "security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\\"admin\\" password=\\"admin-secret\\";") > /dev/null
                        echo "✓ Kafka broker is accessible"
                        '''
                    } catch (Exception e) {
                        error("Required services (Schema Registry or Kafka) are not accessible. Please ensure Docker Compose is running.")
                    }
                }
            }
        }

        stage('List Schema Subjects') {
            steps {
                sh '''
                echo "📋 Listing all schema subjects..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry curl -s http://localhost:8081/subjects | jq -r '.[]' | sort
                echo ""
                echo "Total subjects: $(docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry curl -s http://localhost:8081/subjects | jq 'length')"
                '''
            }
        }

        stage('Schema Registry Health Check') {
            steps {
                sh '''
                echo "📊 Schema Registry Health Check:"
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    echo 'Registry URL: http://localhost:8081'
                    echo 'Mode:' && curl -s http://localhost:8081/mode | jq -r '.mode'
                    echo 'Compatibility Level:' && curl -s http://localhost:8081/config | jq -r '.compatibilityLevel'
                    echo 'Subjects Count:' && curl -s http://localhost:8081/subjects | jq 'length'
                "
                '''
            }
        }

        stage('List Kafka Topics') {
            steps {
                sh '''
                echo "📋 Listing Kafka topics..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T broker bash -c "
                    export KAFKA_OPTS=''
                    unset JMX_PORT
                    unset KAFKA_JMX_OPTS
                    echo 'security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin-secret\";' > /tmp/client.properties
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
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml logs --tail=50 schema-registry || true
            '''
        }
        success {
            sh '''
            echo "✅ Schema Registry health check and operations completed successfully!"
            '''
        }
    }
}