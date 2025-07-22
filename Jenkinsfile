properties([
    parameters([
        [$class: 'ChoiceParameter', 
            name: 'OPERATION',
            description: 'What topic operation do you want to perform?', 
            script: [
                $class: 'GroovyScript', 
                script: [
                    script: '''return["CREATE_TOPIC","LIST_TOPICS:selected","DESCRIBE_TOPIC","DELETE_TOPIC"]'''
                ]
            ]
        ], 
        [$class: 'DynamicReferenceParameter', 
            name: 'TOPIC_OPTIONS', 
            description: 'Topic Configuration Options',
            referencedParameters: 'OPERATION',
            script: [
                $class: 'GroovyScript', 
                script: [
                    script: '''
                        if (OPERATION == 'CREATE_TOPIC') {
                            return """
                                <input name='value' type='text' value='user-events' placeholder='Topic Name'>
                                <select name='value'><option value='3'>3 Partitions</option></select>
                                <select name='value'><option value='2'>2 Replicas</option></select>
                            """
                        } else if (OPERATION in ['DESCRIBE_TOPIC', 'DELETE_TOPIC']) {
                            return "<input name='value' type='text' placeholder='Topic Name'>"
                        } else {
                            return "No additional parameters needed"
                        }
                    '''
                ]
            ]
        ]
    ])
])

pipeline {
    agent any
    environment {
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "Starting Kafka Topic Management"
                    echo "Operation: ${params.OPERATION}"
                }
            }
        }

        stage('Parse Parameters') {
            steps {
                script {
                    def option = "${params.TOPIC_OPTIONS}"
                    def values = option.split(',').collect { it.trim() }.findAll { it }

                    switch(params.OPERATION) {
                        case 'CREATE_TOPIC':
                            env.TOPIC_NAME = values[0]
                            env.PARTITIONS = values[1] ?: '3'
                            env.REPLICATION_FACTOR = values[2] ?: '2'
                            echo "Creating topic: ${env.TOPIC_NAME} (${env.PARTITIONS} partitions, ${env.REPLICATION_FACTOR} replicas)"
                            break
                        case 'DESCRIBE_TOPIC':
                        case 'DELETE_TOPIC':
                            env.TOPIC_NAME = values[0]
                            echo "Topic: ${env.TOPIC_NAME}"
                            break
                        case 'LIST_TOPICS':
                            echo "Listing all topics"
                            break
                    }
                }
            }
        }

        stage('Validate Parameters') {
            steps {
                script {
                    switch(params.OPERATION) {
                        case 'CREATE_TOPIC':
                            if (!env.TOPIC_NAME?.trim()) error "Topic name required"
                            if (!env.TOPIC_NAME.matches('^[a-zA-Z0-9._-]+$')) error "Invalid topic name format"
                            echo "Validation passed"
                            break
                        case 'DESCRIBE_TOPIC':
                        case 'DELETE_TOPIC':
                            if (!env.TOPIC_NAME?.trim()) error "Topic name required"
                            echo "Validation passed"
                            break
                    }
                }
            }
        }

        stage('Delete Confirmation') {
            when { expression { params.OPERATION == 'DELETE_TOPIC' } }
            steps {
                script {
                    def confirmName = input(
                        message: "Delete topic '${env.TOPIC_NAME}'? This cannot be undone!",
                        parameters: [string(name: 'CONFIRM_NAME', description: "Type topic name to confirm")]
                    )
                    if (confirmName != env.TOPIC_NAME) error "Confirmation failed"
                    echo "Deletion confirmed"
                }
            }
        }

        stage('Setup Kafka') {
            steps {
                script {
                    echo "Setting up Kafka environment"

                    // Wait for Kafka services
                    sh """
                        cd ${env.COMPOSE_DIR}
                        docker-compose ps kafka
                    """

                    echo "Kafka ready"
                }
            }
        }

        stage('List Topics') {
            when { expression { params.OPERATION == 'LIST_TOPICS' } }
            steps {
                script {
                    echo "Listing all topics..."
                    def result = sh(
                        script: """
                            cd ${env.COMPOSE_DIR}
                            docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list
                        """,
                        returnStdout: true
                    )

                    if (result) {
                        def topics = result.trim().split('\n').findAll { it?.trim() }
                        echo "Found ${topics?.size() ?: 0} topics:"
                        topics?.each { echo "  - ${it}" }
                    } else {
                        echo "No topics found or command failed"
                    }
                }
            }
        }

        stage('Create Topic') {
            when { expression { params.OPERATION == 'CREATE_TOPIC' } }
            steps {
                script {
                    echo "Creating topic ${env.TOPIC_NAME}..."
                    sh """
                        cd ${env.COMPOSE_DIR}
                        docker-compose exec -T kafka kafka-topics \\
                            --bootstrap-server localhost:9092 \\
                            --create \\
                            --topic ${env.TOPIC_NAME} \\
                            --partitions ${env.PARTITIONS} \\
                            --replication-factor ${env.REPLICATION_FACTOR}
                    """
                    echo "Topic created successfully"
                }
            }
        }

        stage('Describe Topic') {
            when { expression { params.OPERATION == 'DESCRIBE_TOPIC' } }
            steps {
                script {
                    echo "Getting details for topic ${env.TOPIC_NAME}..."
                    def details = sh(
                        script: """
                            cd ${env.COMPOSE_DIR}
                            docker-compose exec -T kafka kafka-topics \\
                                --bootstrap-server localhost:9092 \\
                                --describe \\
                                --topic ${env.TOPIC_NAME}
                        """,
                        returnStdout: true
                    ).trim()
                    echo "Topic details:\n${details}"
                }
            }
        }

        stage('Delete Topic') {
            when { expression { params.OPERATION == 'DELETE_TOPIC' } }
            steps {
                script {
                    echo "Deleting topic ${env.TOPIC_NAME}..."
                    sh """
                        cd ${env.COMPOSE_DIR}
                        docker-compose exec -T kafka kafka-topics \\
                            --bootstrap-server localhost:9092 \\
                            --delete \\
                            --topic ${env.TOPIC_NAME}
                    """
                    echo "Topic deleted successfully"
                }
            }
        } up Kafka environment"
                    confluentOps.waitForServices(env.COMPOSE_DIR)
                    confluentOps.createKafkaClientConfig(env.COMPOSE_DIR)
                    echo "Kafka ready"
                }
            }
        }

        stage('List Topics') {
            when { expression { params.OPERATION == 'LIST_TOPICS' } }
            steps {
                script {
                    echo "Listing all topics..."
                    def topics = confluentOps.listAllTopics(env.COMPOSE_DIR)
                    echo "Found ${topics.size()} topics:"
                    topics.each { echo "  - ${it}" }
                }
            }
        }

        stage('Create Topic') {
            when { expression { params.OPERATION == 'CREATE_TOPIC' } }
            steps {
                script {
                    echo "Creating topic ${env.TOPIC_NAME}..."
                    confluentOps.createTopic(
                        env.COMPOSE_DIR,
                        env.TOPIC_NAME,
                        env.PARTITIONS as Integer,
                        env.REPLICATION_FACTOR as Integer
                    )
                    echo "Topic created successfully"
                }
            }
        }

        stage('Describe Topic') {
            when { expression { params.OPERATION == 'DESCRIBE_TOPIC' } }
            steps {
                script {
                    echo "Getting details for topic ${env.TOPIC_NAME}..."
                    def details = confluentOps.getTopicDetails(env.COMPOSE_DIR, env.TOPIC_NAME)
                    echo "Topic details:\n${details}"
                }
            }
        }

        stage('Delete Topic') {
            when { expression { params.OPERATION == 'DELETE_TOPIC' } }
            steps {
                script {
                    echo "Deleting topic ${env.TOPIC_NAME}..."
                    confluentOps.deleteTopic(env.COMPOSE_DIR, env.TOPIC_NAME)
                    echo "Topic deleted successfully"
                }
            }
        }
    }

    post {
        success {
            echo "Operation completed successfully"
        }
        failure {
            echo "Operation failed - check logs for details"
        }
    }
}