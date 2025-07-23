// Job: kafka-ops/list-schemas
properties([
    parameters([
        string(name: 'ParamsAsENV', defaultValue: 'false', description: 'Use environment parameters'),
        string(name: 'ENVIRONMENT_PARAMS', defaultValue: '', description: 'Environment specific parameters (comma-separated)')
    ])
])

pipeline {
    agent any
    
    environment {
        // Set defaults
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
        CONNECTION_TYPE = 'local-confluent'
        SCHEMA_REGISTRY_URL = 'http://schema-registry:8081'
    }

    stages {
        stage('Parse Environment Parameters') {
            when {
                expression { params.ParamsAsENV == 'true' }
            }
            steps {
                script {
                    if (params.ENVIRONMENT_PARAMS) {
                        def envParams = params.ENVIRONMENT_PARAMS.split(',').collect { it.trim() }
                        if (envParams.size() >= 1 && envParams[0]) env.COMPOSE_DIR = envParams[0]
                        if (envParams.size() >= 2 && envParams[1]) env.CONNECTION_TYPE = envParams[1]
                        if (envParams.size() >= 3 && envParams[2]) env.SCHEMA_REGISTRY_URL = envParams[2]
                        
                        echo "Using environment parameters:"
                        echo "  COMPOSE_DIR: ${env.COMPOSE_DIR}"
                        echo "  CONNECTION_TYPE: ${env.CONNECTION_TYPE}"
                        echo "  SCHEMA_REGISTRY_URL: ${env.SCHEMA_REGISTRY_URL}"
                    }
                }
            }
        }

        stage('Wait for Schema Registry') {
            steps {
                script {
                    def maxRetries = 30
                    def retryCount = 0
                    def servicesReady = false

                    while (retryCount < maxRetries && !servicesReady) {
                        try {
                            sh '''
                            echo "Checking Schema Registry..."
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry curl -f -s http://localhost:8081/subjects > /dev/null
                            '''
                            servicesReady = true
                            echo "Schema Registry is ready!"
                        } catch (Exception e) {
                            echo "Schema Registry not ready yet, waiting... (attempt ${retryCount + 1}/${maxRetries})"
                            sleep(10)
                            retryCount++
                        }
                    }

                    if (!servicesReady) {
                        error("Schema Registry failed to start after ${maxRetries} attempts")
                    }
                }
            }
        }

        stage('Schema Registry Health Check') {
            steps {
                sh '''
                echo "📊 Schema Registry Health Check..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    echo 'Registry URL: ${SCHEMA_REGISTRY_URL}'
                    echo 'Mode:' && curl -s http://localhost:8081/mode
                    echo 'Compatibility Level:' && curl -s http://localhost:8081/config
                "
                '''
            }
        }

        stage('List All Schema Subjects') {
            steps {
                sh '''
                echo "📋 Listing all schema subjects..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    RESPONSE=\\$(curl -s http://localhost:8081/subjects)
                    echo 'All registered subjects:'
                    echo \"\\$RESPONSE\"
                    if [ \"\\$RESPONSE\" = '[]' ]; then
                        echo 'No subjects found in registry'
                    else
                        echo 'Found subjects in registry'
                        # Pretty print each subject
                        echo \"\\$RESPONSE\" | sed 's/\\[//g' | sed 's/\\]//g' | sed 's/,/\\n/g' | sed 's/\"//g' | while read subject; do
                            if [ ! -z \"\\$subject\" ]; then
                                echo \"  - \\$subject\"
                            fi
                        done
                    fi
                "
                '''
            }
        }

        stage('List Schema Details') {
            steps {
                sh '''
                echo "🔍 Getting detailed schema information..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    SUBJECTS=\\$(curl -s http://localhost:8081/subjects | sed 's/\\[//g' | sed 's/\\]//g' | sed 's/,/\\n/g' | sed 's/\\\"//g')
                    
                    if [ \"\\$SUBJECTS\" != \"\" ]; then
                        echo 'Schema Details:'
                        echo \"\\$SUBJECTS\" | while read subject; do
                            if [ ! -z \"\\$subject\" ]; then
                                echo \"\\n=== Subject: \\$subject ===\"
                                echo \"Versions:\"
                                curl -s http://localhost:8081/subjects/\\$subject/versions
                                echo \"\\nLatest Schema:\"
                                curl -s http://localhost:8081/subjects/\\$subject/versions/latest | jq -r '.schema' 2>/dev/null || curl -s http://localhost:8081/subjects/\\$subject/versions/latest
                                echo \"\\n\"
                            fi
                        done
                    else
                        echo 'No schemas to display details for'
                    fi
                "
                '''
            }
        }
    }

    post {
        success {
            echo "✅ Schema listing completed successfully"
        }
        failure {
            echo "❌ Failed to list schemas - check logs for details"
            sh '''
            echo "Pipeline failed. Checking Schema Registry logs..."
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml logs --tail=50 schema-registry || true
            '''
        }
        always {
            echo "List schemas operation completed"
        }
    }
}