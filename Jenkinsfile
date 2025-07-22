@Library('kafka-ops-shared-lib') _

// Job: jenkins-practice-manage-topic/create-topic
properties([
    parameters([
        string(name: 'TopicName', defaultValue: '', description: 'Name of the topic to create'),
        string(name: 'Partitions', defaultValue: '3', description: 'Number of partitions'),
        string(name: 'ReplicationFactor', defaultValue: '2', description: 'Replication factor'),
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
                    if (!params.TopicName.matches('^[a-zA-Z0-9._-]+$')) {
                        error "Invalid topic name format. Use alphanumeric characters, dots, underscores, and hyphens only."
                    }
                    if (!params.Partitions?.trim() || !params.Partitions.isInteger()) {
                        error "Partitions must be a valid integer"
                    }
                    if (!params.ReplicationFactor?.trim() || !params.ReplicationFactor.isInteger()) {
                        error "Replication factor must be a valid integer"
                    }
                    
                    echo "Validation passed for topic: ${params.TopicName}"
                    echo "Partitions: ${params.Partitions}, Replication Factor: ${params.ReplicationFactor}"
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

        stage('Create Topic') {
            steps {
                script {
                    echo "Creating topic '${params.TopicName}' with ${params.Partitions} partitions and replication factor ${params.ReplicationFactor}"
                    
                    confluentOps.createTopic(
                        env.COMPOSE_DIR,
                        params.TopicName,
                        params.Partitions as Integer,
                        params.ReplicationFactor as Integer
                    )
                    
                    echo "✅ Topic '${params.TopicName}' created successfully"
                }
            }
        }

        stage('Verify Topic Creation') {
            steps {
                script {
                    echo "Verifying topic creation..."
                    def topicDetails = confluentOps.getTopicDetails(env.COMPOSE_DIR, params.TopicName)
                    echo "Topic verification successful:\n${topicDetails}"
                }
            }
        }
    }

    post {
        success {
            echo "✅ Topic '${params.TopicName}' has been created successfully"
        }
        failure {
            echo "❌ Failed to create topic '${params.TopicName}' - check logs for details"
        }
        always {
            echo "Create topic operation completed"
        }
    }
}