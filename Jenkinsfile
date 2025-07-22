@Library('kafka-ops-shared-lib') _

// Job: jenkins-practice-manage-topic/delete-topic
properties([
    parameters([
        string(name: 'TopicName', defaultValue: '', description: 'Name of the topic to delete'),
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
                    
                    echo "Validation passed for topic deletion: ${params.TopicName}"
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
                    echo "Verifying that topic '${params.TopicName}' exists..."
                    
                    def allTopics = confluentOps.listAllTopics(env.COMPOSE_DIR)
                    def topicExists = allTopics.contains(params.TopicName)
                    
                    if (!topicExists) {
                        error "Topic '${params.TopicName}' does not exist. Available topics: ${allTopics.join(', ')}"
                    }
                    
                    echo "✅ Topic '${params.TopicName}' exists and can be deleted"
                    
                    // Get topic details before deletion for logging
                    def topicDetails = confluentOps.getTopicDetails(env.COMPOSE_DIR, params.TopicName)
                    env.TOPIC_DETAILS_BEFORE_DELETE = topicDetails
                }
            }
        }

        stage('Pre-deletion Safety Check') {
            steps {
                script {
                    echo """
⚠️  FINAL SAFETY CHECK
====================
Topic to DELETE: ${params.TopicName}
Connection: ${env.CONNECTION_TYPE}
Environment: ${env.COMPOSE_DIR}

Topic Details:
${env.TOPIC_DETAILS_BEFORE_DELETE}
====================

⚠️  WARNING: This action cannot be undone!
All data in this topic will be permanently lost!
"""
                }
            }
        }

        stage('Delete Topic') {
            steps {
                script {
                    echo "🗑️  Proceeding with deletion of topic '${params.TopicName}'..."
                    echo "⚠️  This is irreversible - all topic data will be lost!"
                    
                    confluentOps.deleteTopic(env.COMPOSE_DIR, params.TopicName)
                    
                    echo "✅ Topic '${params.TopicName}' has been deleted"
                }
            }
        }

        stage('Verify Deletion') {
            steps {
                script {
                    echo "Verifying topic deletion..."
                    
                    // Wait a moment for deletion to propagate
                    sleep(time: 3, unit: 'SECONDS')
                    
                    def allTopics = confluentOps.listAllTopics(env.COMPOSE_DIR)
                    def topicStillExists = allTopics.contains(params.TopicName)
                    
                    if (topicStillExists) {
                        echo "⚠️  Topic still appears in list - deletion may still be in progress"
                        echo "Current topics: ${allTopics.join(', ')}"
                    } else {
                        echo "✅ Confirmed: Topic '${params.TopicName}' has been successfully deleted"
                        echo "Remaining topics: ${allTopics.join(', ')}"
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo """
✅ DELETION COMPLETED SUCCESSFULLY
=================================
Topic '${params.TopicName}' has been permanently deleted
Connection: ${env.CONNECTION_TYPE}
Environment: ${env.COMPOSE_DIR}
Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
=================================
"""
            }
        }
        failure {
            echo "❌ Failed to delete topic '${params.TopicName}' - check logs for details"
        }
        always {
            echo "Delete topic operation completed"
        }
    }
}