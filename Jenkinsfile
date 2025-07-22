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
                confluentOps.waitForServices(env.COMPOSE_DIR)
                confluentOps.createKafkaClientConfig(env.COMPOSE_DIR)
            }
        }

        stage('Topic Operations') {
            parallel {
                stage('Create Topic') {
                    when {
                        expression { params.OPERATION == 'CREATE_TOPIC' }
                    }
                    steps {
                        confluentOps.createTopic(env.COMPOSE_DIR, params.TOPIC_NAME, params.PARTITIONS as Integer, params.REPLICATION_FACTOR as Integer)
                    }
                }

                stage('List Topics') {
                    when {
                        expression { params.OPERATION == 'LIST_TOPICS' }
                    }
                    steps {
                        confluentOps.listAllTopics(env.COMPOSE_DIR)
                    }
                }

                stage('Describe Topic') {
                    when {
                        expression { params.OPERATION == 'DESCRIBE_TOPIC' }
                    }
                    steps {
                        confluentOps.getTopicDetails(env.COMPOSE_DIR, params.TOPIC_NAME)
                    }
                }
            }
        }
    }
}