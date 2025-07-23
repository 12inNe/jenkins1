@Library('kafka-ops-shared-lib') _

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
                        echo "Using environment parameters:"
                        echo "  COMPOSE_DIR: ${env.COMPOSE_DIR}"
                        echo "  CONNECTION_TYPE: ${env.CONNECTION_TYPE}"
                    }
                }
            }
        }

        stage('Setup Kafka Environment') {
            steps {
                script {
                    echo "Setting up Kafka environment at ${env.COMPOSE_DIR}"
                    confluentOps.waitForServices(env.COMPOSE_DIR)
                    confluentOps.createKafkaClientConfig(env.COMPOSE_DIR)
                    echo "Kafka environment ready"
                }
            }
        }

        stage('List All Topics') {
            steps {
                script {
                    echo "📋 Retrieving all Kafka topics..."

                    def topics = confluentOps.listAllTopics(env.COMPOSE_DIR)

                    if (topics && topics.size() > 0) {
                        echo "✅ Found ${topics.size()} topic(s):"
                        echo "=" * 50
                        topics.eachWithIndex { topic, index ->
                            echo "  ${index + 1}. ${topic}"
                        }
                        echo "=" * 50

                        // Store topics list for downstream jobs if needed
                        env.TOPICS_COUNT = topics.size().toString()
                        env.TOPICS_LIST = topics.join(',')
                    } else {
                        echo "⚠️  No topics found in the Kafka cluster"
                        env.TOPICS_COUNT = '0'
                        env.TOPICS_LIST = ''
                    }
                }
            }
        }

        stage('Summary Report') {
            steps {
                script {
                    echo """
📊 KAFKA TOPICS SUMMARY REPORT
================================
Connection Type: ${env.CONNECTION_TYPE}
Kafka Environment: ${env.COMPOSE_DIR}
Total Topics Found: ${env.TOPICS_COUNT}
Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
================================
"""
                }
            }
        }
    }

    post {
        success {
            echo "✅ Successfully listed all Kafka topics (${env.TOPICS_COUNT} found)"
        }
        failure {
            echo "❌ Failed to list topics - check Kafka connection and logs"
        }
        always {
            echo "List topics operation completed"
        }
    }
}