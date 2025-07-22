@Library('kafka-ops-shared-lib') _

// Job: jenkins-practice-manage-topic/describe-topic
properties([
    parameters([
        string(name: 'TopicName', defaultValue: '', description: 'Name of the topic to describe'),
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

        stage('Validate Parameters') {
            steps {
                script {
                    if (!params.TopicName?.trim()) {
                        error "Topic name is required"
                    }
                    
                    echo "Validation passed for topic: ${params.TopicName}"
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

        stage('Check Topic Exists') {
            steps {
                script {
                    echo "Checking if topic '${params.TopicName}' exists..."
                    
                    def allTopics = confluentOps.listAllTopics(env.COMPOSE_DIR)
                    def topicExists = allTopics.contains(params.TopicName)
                    
                    if (!topicExists) {
                        error "Topic '${params.TopicName}' does not exist. Available topics: ${allTopics.join(', ')}"
                    }
                    
                    echo "✅ Topic '${params.TopicName}' exists"
                }
            }
        }

        stage('Describe Topic') {
            steps {
                script {
                    echo "🔍 Getting detailed information for topic '${params.TopicName}'..."
                    
                    def topicDetails = confluentOps.getTopicDetails(env.COMPOSE_DIR, params.TopicName)
                    
                    echo """
🔍 TOPIC DETAILS REPORT
========================
Topic Name: ${params.TopicName}
Connection: ${env.CONNECTION_TYPE}
Environment: ${env.COMPOSE_DIR}
========================

${topicDetails}

========================
Report Generated: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
========================
"""
                    
                    // Store details for potential downstream use
                    env.TOPIC_DETAILS = topicDetails
                }
            }
        }
    }

    post {
        success {
            echo "✅ Successfully retrieved details for topic '${params.TopicName}'"
        }
        failure {
            echo "❌ Failed to describe topic '${params.TopicName}' - check if topic exists and Kafka is accessible"
        }
        always {
            echo "Describe topic operation completed"
        }
    }
}