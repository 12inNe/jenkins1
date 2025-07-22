// ==============================================================
// 2. TOPIC MANAGEMENT PIPELINE
// ==============================================================
// File: topic-management-pipeline.groovy

@Library('kafka-ops-shared-lib') _

pipeline {
    agent any
    parameters {
        choice(
            name: 'OPERATION',
            choices: ['CREATE_TOPIC', 'LIST_TOPICS', 'DESCRIBE_TOPIC'],
            description: 'Topic operation to perform'
        )
        string(
            name: 'TOPIC_NAME',
            defaultValue: 'user-events',
            description: 'Topic name'
        )
        string(
            name: 'PARTITIONS',
            defaultValue: '3',
            description: 'Number of partitions'
        )
        string(
            name: 'REPLICATION_FACTOR',
            defaultValue: '1',
            description: 'Replication factor'
        )
    }
    environment {
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
    }
    stages {
        stage('Setup') {
            steps {
                script {
                    confluentOps.waitForServices(env.COMPOSE_DIR)
                    confluentOps.createKafkaClientConfig(env.COMPOSE_DIR)
                }
            }
        }

        stage('Topic Operations') {
            parallel {
                stage('Create Topic') {
                    when {
                        expression { params.OPERATION == 'CREATE_TOPIC' }
                    }
                    steps {
                        script {
                            try {
                                confluentOps.createTopic(
                                    env.COMPOSE_DIR, 
                                    params.TOPIC_NAME, 
                                    params.PARTITIONS as Integer, 
                                    params.REPLICATION_FACTOR as Integer
                                )
                                echo "Successfully created topic: ${params.TOPIC_NAME}"
                            } catch (Exception e) {
                                error "Failed to create topic ${params.TOPIC_NAME}: ${e.message}"
                            }
                        }
                    }
                }

                stage('List Topics') {
                    when {
                        expression { params.OPERATION == 'LIST_TOPICS' }
                    }
                    steps {
                        script {
                            try {
                                def topics = confluentOps.listAllTopics(env.COMPOSE_DIR)
                                echo "Available topics: ${topics}"
                            } catch (Exception e) {
                                error "Failed to list topics: ${e.message}"
                            }
                        }
                    }
                }

                stage('Describe Topic') {
                    when {
                        expression { params.OPERATION == 'DESCRIBE_TOPIC' }
                    }
                    steps {
                        script {
                            try {
                                def topicDetails = confluentOps.getTopicDetails(env.COMPOSE_DIR, params.TOPIC_NAME)
                                echo "Topic details for ${params.TOPIC_NAME}: ${topicDetails}"
                            } catch (Exception e) {
                                error "Failed to describe topic ${params.TOPIC_NAME}: ${e.message}"
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Topic management operation completed: ${params.OPERATION}"
        }
        success {
            echo "✅ Pipeline completed successfully!"
        }
        failure {
            echo "❌ Pipeline failed. Check the logs for details."
        }
    }
}