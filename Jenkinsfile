@Library('kafka-ops-shared-lib') _

properties([
    parameters([
        [$class: 'ChoiceParameter', 
            choiceType: 'PT_SINGLE_SELECT', 
            description: 'What topic operation do you want to perform?', 
            filterLength: 1, 
            filterable: false, 
            name: 'OPERATION',
            script: [
                $class: 'GroovyScript', 
                fallbackScript: [
                    classpath: [], 
                    sandbox: true, 
                    script: 
                        '''return['CREATE_TOPIC:ERROR']'''
                ], 
                script: [
                    classpath: [], 
                    sandbox: true, 
                    script: 
                        '''return["CREATE_TOPIC","LIST_TOPICS:selected","DESCRIBE_TOPIC","DELETE_TOPIC"]'''
                ]
            ]
        ], 
        [$class: 'DynamicReferenceParameter', 
            choiceType: 'ET_FORMATTED_HTML', 
            description: 'Topic Configuration Options', 
            name: 'TOPIC_OPTIONS', 
            omitValueField: false, 
            referencedParameters: 'OPERATION',
            script: [
                $class: 'GroovyScript', 
                fallbackScript: [
                    classpath: [], 
                    sandbox: true, 
                    script: 
                        '''return['TOPIC_MANAGEMENT:ERROR']'''
                ], 
                script: [
                    classpath: [], 
                    sandbox: true, 
                    script: 
                        '''
                        if (OPERATION == 'LIST_TOPICS'){
                            return """
                                <div style="background-color: #e8f5e8; padding: 15px; border-radius: 5px; border-left: 4px solid #28a745;">
                                    <h4 style="margin: 0; color: #155724;">📋 List All Topics</h4>
                                    <p style="margin: 5px 0 0 0; color: #155724;">This operation will list all available Kafka topics. No additional parameters required.</p>
                                </div>
                            """
                        } else if (OPERATION == 'CREATE_TOPIC') {
                            return """
                                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; border: 1px solid #dee2e6;">
                                    <h4 style="margin: 0 0 15px 0; color: #495057;">🚀 Create New Topic</h4>
                                    <table style="width: 100%; border-collapse: collapse;">
                                        <tr>
                                            <td style="padding: 8px; vertical-align: top; width: 200px;">
                                                <label style="font-weight: bold; color: #495057;">Topic Name *</label>
                                            </td>
                                            <td style="padding: 8px;">
                                                <input name='value' type='text' value='user-events' style="width: 300px; padding: 5px; border: 1px solid #ced4da; border-radius: 3px;">
                                                <div style="font-size: 12px; color: #6c757d; margin-top: 3px;">Use alphanumeric characters, dots, underscores, and hyphens</div>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; vertical-align: top;">
                                                <label style="font-weight: bold; color: #495057;">Partitions *</label>
                                            </td>
                                            <td style="padding: 8px;">
                                                <select name='value' style="width: 200px; padding: 5px; border: 1px solid #ced4da; border-radius: 3px;">
                                                    <option value='1' selected>1 (Development)</option>
                                                    <option value='3'>3 (Small workload)</option>
                                                    <option value='6'>6 (Medium workload)</option>
                                                    <option value='12'>12 (High workload)</option>
                                                    <option value='24'>24 (Very high workload)</option>
                                                </select>
                                                <div style="font-size: 12px; color: #6c757d; margin-top: 3px;">More partitions = better parallelism but more overhead</div>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; vertical-align: top;">
                                                <label style="font-weight: bold; color: #495057;">Replication Factor *</label>
                                            </td>
                                            <td style="padding: 8px;">
                                                <select name='value' style="width: 200px; padding: 5px; border: 1px solid #ced4da; border-radius: 3px;">
                                                    <option value='1' selected>1 (Development - No redundancy)</option>
                                                    <option value='2'>2 (Staging - Basic redundancy)</option>
                                                    <option value='3'>3 (Production - High availability)</option>
                                                </select>
                                                <div style="font-size: 12px; color: #6c757d; margin-top: 3px;">Production should use 3 for fault tolerance</div>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                            """
                        } else if (OPERATION == 'DESCRIBE_TOPIC') {
                            return """
                                <div style="background-color: #fff3cd; padding: 15px; border-radius: 5px; border-left: 4px solid #ffc107;">
                                    <h4 style="margin: 0 0 15px 0; color: #856404;">🔍 Describe Topic</h4>
                                    <table style="width: 100%;">
                                        <tr>
                                            <td style="padding: 8px; vertical-align: top; width: 200px;">
                                                <label style="font-weight: bold; color: #856404;">Topic Name *</label>
                                            </td>
                                            <td style="padding: 8px;">
                                                <input name='value' type='text' value='user-events' style="width: 300px; padding: 5px; border: 1px solid #ffeaa7; border-radius: 3px;">
                                                <div style="font-size: 12px; color: #856404; margin-top: 3px;">Enter the name of an existing topic to get its details</div>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                            """
                        } else if (OPERATION == 'DELETE_TOPIC') {
                            return """
                                <div style="background-color: #f8d7da; padding: 15px; border-radius: 5px; border-left: 4px solid #dc3545;">
                                    <h4 style="margin: 0 0 15px 0; color: #721c24;">⚠️ Delete Topic</h4>
                                    <div style="background-color: #ffffff; padding: 10px; border-radius: 3px; margin-bottom: 15px; border: 1px solid #f5c6cb;">
                                        <strong style="color: #721c24;">⚠️ WARNING:</strong> This action will permanently delete the topic and all its data. This cannot be undone!
                                    </div>
                                    <table style="width: 100%;">
                                        <tr>
                                            <td style="padding: 8px; vertical-align: top; width: 200px;">
                                                <label style="font-weight: bold; color: #721c24;">Topic Name *</label>
                                            </td>
                                            <td style="padding: 8px;">
                                                <input name='value' type='text' value='' placeholder='Enter topic name to delete' style="width: 300px; padding: 5px; border: 1px solid #f5c6cb; border-radius: 3px;">
                                                <div style="font-size: 12px; color: #721c24; margin-top: 3px;">You will be asked to confirm the deletion before it proceeds</div>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                            """
                        } else {
                            return """
                                <div style="background-color: #d1ecf1; padding: 15px; border-radius: 5px; border-left: 4px solid #17a2b8;">
                                    <h4 style="margin: 0; color: #0c5460;">Select an Operation</h4>
                                    <p style="margin: 5px 0 0 0; color: #0c5460;">Please choose a topic operation from the dropdown above.</p>
                                </div>
                            """
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
        CONNECTION_TYPE = 'local-confluent'
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

        stage('Execute Operation') {
            steps {
                script {
                    switch(params.OPERATION) {
                        case 'LIST_TOPICS':
                            echo "==== Calling List Topics job ===="
                            def listResult = build job: 'GIT-org/jenkins1/list-topic', 
                                parameters: [
                                    string(name: 'ParamsAsENV', value: 'true'),
                                    string(name: 'ENVIRONMENT_PARAMS', value: "${env.COMPOSE_DIR},${env.CONNECTION_TYPE}")
                                ],
                                propagate: false // Don't fail main job if downstream fails
                            
                            echo "==== List Topics Results ===="
                            echo "Status: ${listResult.result}"
                            echo "Build Number: ${listResult.number}"
                            echo "Duration: ${listResult.duration}ms"
                            
                            // Get console output from the downstream job
                            try {
                                def consoleOutput = getConsoleOutput(listResult)
                                echo "==== Console Output from List Topics ===="
                                echo consoleOutput
                            } catch (Exception e) {
                                echo "Could not retrieve console output: ${e.message}"
                            }
                            
                            // Archive the result as an artifact
                            writeFile file: 'list-topics-result.txt', 
                                     text: "Status: ${listResult.result}\nBuild: ${listResult.number}\nDuration: ${listResult.duration}ms"
                            archiveArtifacts artifacts: 'list-topics-result.txt'
                            
                            if (listResult.result != 'SUCCESS') {
                                echo "⚠️ List topics job failed or was unstable"
                            } else {
                                echo "✅ List topics completed successfully"
                            }
                            break

                        case 'CREATE_TOPIC':
                            echo "==== Calling Create Topic job ===="
                            def createResult = build job: 'GIT-org/jenkins1/create-topic', 
                                parameters: [
                                    string(name: 'TopicName', value: "${env.TOPIC_NAME}"),
                                    string(name: 'Partitions', value: "${env.PARTITIONS}"),
                                    string(name: 'ReplicationFactor', value: "${env.REPLICATION_FACTOR}"),
                                    string(name: 'ParamsAsENV', value: 'true'),
                                    string(name: 'ENVIRONMENT_PARAMS', value: "${env.COMPOSE_DIR},${env.CONNECTION_TYPE}")
                                ],
                                propagate: false
                            
                            echo "==== Create Topic Results ===="
                            echo "Status: ${createResult.result}"
                            echo "Build Number: ${createResult.number}"
                            echo "Duration: ${createResult.duration}ms"
                            echo "Topic Name: ${env.TOPIC_NAME}"
                            echo "Partitions: ${env.PARTITIONS}"
                            echo "Replication Factor: ${env.REPLICATION_FACTOR}"
                            
                            try {
                                def consoleOutput = getConsoleOutput(createResult)
                                echo "==== Console Output from Create Topic ===="
                                echo consoleOutput
                            } catch (Exception e) {
                                echo "Could not retrieve console output: ${e.message}"
                            }
                            
                            writeFile file: 'create-topic-result.txt', 
                                     text: "Status: ${createResult.result}\nTopic: ${env.TOPIC_NAME}\nPartitions: ${env.PARTITIONS}\nReplication: ${env.REPLICATION_FACTOR}\nBuild: ${createResult.number}\nDuration: ${createResult.duration}ms"
                            archiveArtifacts artifacts: 'create-topic-result.txt'
                            
                            if (createResult.result != 'SUCCESS') {
                                echo "⚠️ Create topic job failed or was unstable"
                            } else {
                                echo "✅ Topic '${env.TOPIC_NAME}' created successfully"
                            }
                            break

                        case 'DESCRIBE_TOPIC':
                            echo "==== Calling Describe Topic job ===="
                            def describeResult = build job: 'GIT-org/jenkins1/describe-topic', 
                                parameters: [
                                    string(name: 'TopicName', value: "${env.TOPIC_NAME}"),
                                    string(name: 'ParamsAsENV', value: 'true'),
                                    string(name: 'ENVIRONMENT_PARAMS', value: "${env.COMPOSE_DIR},${env.CONNECTION_TYPE}")
                                ],
                                propagate: false
                            
                            echo "==== Describe Topic Results ===="
                            echo "Status: ${describeResult.result}"
                            echo "Build Number: ${describeResult.number}"
                            echo "Duration: ${describeResult.duration}ms"
                            echo "Topic Name: ${env.TOPIC_NAME}"
                            
                            try {
                                def consoleOutput = getConsoleOutput(describeResult)
                                echo "==== Console Output from Describe Topic ===="
                                echo consoleOutput
                            } catch (Exception e) {
                                echo "Could not retrieve console output: ${e.message}"
                            }
                            
                            writeFile file: 'describe-topic-result.txt', 
                                     text: "Status: ${describeResult.result}\nTopic: ${env.TOPIC_NAME}\nBuild: ${describeResult.number}\nDuration: ${describeResult.duration}ms"
                            archiveArtifacts artifacts: 'describe-topic-result.txt'
                            
                            if (describeResult.result != 'SUCCESS') {
                                echo "⚠️ Describe topic job failed or was unstable"
                            } else {
                                echo "✅ Topic '${env.TOPIC_NAME}' described successfully"
                            }
                            break

                        case 'DELETE_TOPIC':
                            echo "Requesting delete confirmation..."
                            def confirmName = input(
                                message: "Delete topic '${env.TOPIC_NAME}'? This cannot be undone!",
                                parameters: [string(name: 'CONFIRM_NAME', description: "Type topic name to confirm")]
                            )
                            if (confirmName != env.TOPIC_NAME) {
                                error "Confirmation failed - typed '${confirmName}' but expected '${env.TOPIC_NAME}'"
                            }
                            echo "Confirmation successful, proceeding with deletion..."

                            echo "==== Calling Delete Topic job ===="
                            def deleteResult = build job: 'GIT-org/jenkins1/delete-topic', 
                                parameters: [
                                    string(name: 'TopicName', value: "${env.TOPIC_NAME}"),
                                    string(name: 'ParamsAsENV', value: 'true'),
                                    string(name: 'ENVIRONMENT_PARAMS', value: "${env.COMPOSE_DIR},${env.CONNECTION_TYPE}")
                                ],
                                propagate: false
                            
                            echo "==== Delete Topic Results ===="
                            echo "Status: ${deleteResult.result}"
                            echo "Build Number: ${deleteResult.number}"
                            echo "Duration: ${deleteResult.duration}ms"
                            echo "Deleted Topic: ${env.TOPIC_NAME}"
                            
                            try {
                                def consoleOutput = getConsoleOutput(deleteResult)
                                echo "==== Console Output from Delete Topic ===="
                                echo consoleOutput
                            } catch (Exception e) {
                                echo "Could not retrieve console output: ${e.message}"
                            }
                            
                            writeFile file: 'delete-topic-result.txt', 
                                     text: "Status: ${deleteResult.result}\nDeleted Topic: ${env.TOPIC_NAME}\nBuild: ${deleteResult.number}\nDuration: ${deleteResult.duration}ms"
                            archiveArtifacts artifacts: 'delete-topic-result.txt'
                            
                            if (deleteResult.result != 'SUCCESS') {
                                echo "⚠️ Delete topic job failed or was unstable"
                            } else {
                                echo "✅ Topic '${env.TOPIC_NAME}' deleted successfully"
                            }
                            break

                        default:
                            error "Unknown operation: ${params.OPERATION}"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Kafka topic operation '${params.OPERATION}' completed successfully"
            
            // Generate a summary report
            script {
                def summary = "=== KAFKA TOPIC OPERATION SUMMARY ===\n"
                summary += "Operation: ${params.OPERATION}\n"
                summary += "Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}\n"
                
                switch(params.OPERATION) {
                    case 'CREATE_TOPIC':
                        summary += "Topic Created: ${env.TOPIC_NAME}\n"
                        summary += "Partitions: ${env.PARTITIONS}\n"
                        summary += "Replication Factor: ${env.REPLICATION_FACTOR}\n"
                        break
                    case 'DELETE_TOPIC':
                        summary += "Topic Deleted: ${env.TOPIC_NAME}\n"
                        break
                    case 'DESCRIBE_TOPIC':
                        summary += "Topic Described: ${env.TOPIC_NAME}\n"
                        break
                    case 'LIST_TOPICS':
                        summary += "Listed all available topics\n"
                        break
                }
                summary += "Status: SUCCESS\n"
                
                echo summary
                writeFile file: 'operation-summary.txt', text: summary
                archiveArtifacts artifacts: 'operation-summary.txt'
            }
        }
        failure {
            echo "❌ Kafka topic operation '${params.OPERATION}' failed - check logs for details"
        }
        always {
            echo "Cleaning up temporary environment variables"
        }
    }
}

// Helper function to get console output from downstream job
def getConsoleOutput(buildResult) {
    def build = buildResult.getRawBuild()
    def logFile = build.getLogFile()
    if (logFile.exists()) {
        return logFile.text
    } else {
        return "Console log not available"
    }
}