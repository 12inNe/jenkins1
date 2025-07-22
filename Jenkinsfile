// ==============================================================
// 2. TOPIC MANAGEMENT PIPELINE WITH ACTIVE CHOICES
// ==============================================================
// File: topic-management-pipeline.groovy

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
    }
    
    stages {
        stage('Parse Parameters') {
            steps {
                script {
                    echo "🔧 Parsing parameters for operation: ${params.OPERATION}"
                    
                    // Parse the TOPIC_OPTIONS parameter based on operation type
                    def option = "${params.TOPIC_OPTIONS}"
                    def values = option.split(',').collect { it.trim() }.findAll { it }
                    
                    // Set environment variables based on operation
                    switch(params.OPERATION) {
                        case 'CREATE_TOPIC':
                            if (values.size() >= 3) {
                                env.TOPIC_NAME = values[0]
                                env.PARTITIONS = values[1]
                                env.REPLICATION_FACTOR = values[2]
                            } else {
                                error "❌ Invalid parameters for CREATE_TOPIC operation"
                            }
                            break
                        case 'DESCRIBE_TOPIC':
                        case 'DELETE_TOPIC':
                            if (values.size() >= 1) {
                                env.TOPIC_NAME = values[0]
                            } else {
                                error "❌ Topic name is required for ${params.OPERATION} operation"
                            }
                            break
                        case 'LIST_TOPICS':
                            // No additional parameters needed
                            break
                        default:
                            error "❌ Unknown operation: ${params.OPERATION}"
                    }
                    
                    echo "✅ Parameters parsed successfully"
                }
            }
        }
        
        stage('Validate Parameters') {
            steps {
                script {
                    echo "🔍 Validating parameters..."
                    
                    // Validation logic based on operation
                    switch(params.OPERATION) {
                        case 'CREATE_TOPIC':
                            if (!env.TOPIC_NAME || env.TOPIC_NAME.trim() == '') {
                                error "❌ Topic name cannot be empty"
                            }
                            if (!env.TOPIC_NAME.matches('^[a-zA-Z0-9._-]+$')) {
                                error "❌ Topic name can only contain alphanumeric characters, dots, underscores, and hyphens"
                            }
                            def partitions = env.PARTITIONS as Integer
                            def replicationFactor = env.REPLICATION_FACTOR as Integer
                            
                            if (partitions < 1 || partitions > 50) {
                                error "❌ Partitions must be between 1 and 50"
                            }
                            if (replicationFactor < 1 || replicationFactor > 3) {
                                error "❌ Replication factor must be between 1 and 3"
                            }
                            break
                        case 'DESCRIBE_TOPIC':
                        case 'DELETE_TOPIC':
                            if (!env.TOPIC_NAME || env.TOPIC_NAME.trim() == '') {
                                error "❌ Topic name is required for ${params.OPERATION}"
                            }
                            break
                    }
                    
                    echo "✅ Parameters validated successfully"
                }
            }
        }

        stage('Confirmation for Delete') {
            when {
                expression { params.OPERATION == 'DELETE_TOPIC' }
            }
            agent none
            steps {
                script {
                    def CONFIRM_NAME = input(
                        message: "⚠️ DANGER ZONE ⚠️\n\nYou are about to permanently delete topic: '${env.TOPIC_NAME}'\n\nThis action cannot be undone and will result in permanent data loss!\n\nType the topic name exactly as shown above to confirm:",
                        parameters: [
                            string(
                                defaultValue: '', 
                                description: "Type '${env.TOPIC_NAME}' to confirm deletion", 
                                name: 'CONFIRM_NAME'
                            )
                        ],
                        ok: "🗑️ DELETE TOPIC",
                        cancel: "❌ Cancel (Recommended)"
                    )
                    
                    if (CONFIRM_NAME != env.TOPIC_NAME) {
                        error "❌ Topic name confirmation failed. Deletion cancelled for safety."
                    }
                    
                    env.DELETION_CONFIRMED = 'true'
                    echo "⚠️ Deletion confirmed by user"
                }
            }
        }

        stage('Setup Kafka Environment') {
            steps {
                script {
                    echo "🔧 Setting up Kafka environment..."
                    confluentOps.waitForServices(env.COMPOSE_DIR)
                    confluentOps.createKafkaClientConfig(env.COMPOSE_DIR)
                    echo "✅ Kafka environment ready"
                }
            }
        }

        stage('Execute Topic Operations') {
            parallel {
                stage('Create Topic') {
                    when {
                        expression { params.OPERATION == 'CREATE_TOPIC' }
                    }
                    steps {
                        script {
                            try {
                                echo "🚀 Creating topic '${env.TOPIC_NAME}' with ${env.PARTITIONS} partitions and replication factor ${env.REPLICATION_FACTOR}"
                                
                                confluentOps.createTopic(
                                    env.COMPOSE_DIR,
                                    env.TOPIC_NAME,
                                    env.PARTITIONS as Integer,
                                    env.REPLICATION_FACTOR as Integer
                                )
                                
                                def successMessage = "✅ Successfully created topic: ${env.TOPIC_NAME}"
                                echo successMessage
                                
                                // Write result for potential artifact collection
                                writeFile file: 'create_result.txt', text: "Success: Topic '${env.TOPIC_NAME}' created with ${env.PARTITIONS} partitions and replication factor ${env.REPLICATION_FACTOR}"
                                
                            } catch (Exception e) {
                                def errorMessage = "❌ Failed to create topic ${env.TOPIC_NAME}: ${e.message}"
                                echo errorMessage
                                writeFile file: 'create_result.txt', text: "Error: ${e.message}"
                                error errorMessage
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'create_result.txt', allowEmptyArchive: true
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
                                echo "📋 Listing all available topics..."
                                def topics = confluentOps.listAllTopics(env.COMPOSE_DIR)
                                
                                def successMessage = "✅ Available topics: ${topics}"
                                echo successMessage
                                
                                writeFile file: 'list_result.txt', text: "Success: Available topics:\n${topics.join('\n')}"
                                
                            } catch (Exception e) {
                                def errorMessage = "❌ Failed to list topics: ${e.message}"
                                echo errorMessage
                                writeFile file: 'list_result.txt', text: "Error: ${e.message}"
                                error errorMessage
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'list_result.txt', allowEmptyArchive: true
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
                                echo "🔍 Getting details for topic: ${env.TOPIC_NAME}"
                                def topicDetails = confluentOps.getTopicDetails(env.COMPOSE_DIR, env.TOPIC_NAME)
                                
                                def successMessage = "✅ Topic details for ${env.TOPIC_NAME}: ${topicDetails}"
                                echo successMessage
                                
                                writeFile file: 'describe_result.txt', text: "Success: Topic details for '${env.TOPIC_NAME}':\n${topicDetails}"
                                
                            } catch (Exception e) {
                                def errorMessage = "❌ Failed to describe topic ${env.TOPIC_NAME}: ${e.message}"
                                echo errorMessage
                                writeFile file: 'describe_result.txt', text: "Error: ${e.message}"
                                error errorMessage
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'describe_result.txt', allowEmptyArchive: true
                        }
                    }
                }

                stage('Delete Topic') {
                    when {
                        expression { params.OPERATION == 'DELETE_TOPIC' && env.DELETION_CONFIRMED == 'true' }
                    }
                    steps {
                        script {
                            try {
                                echo "⚠️ DELETING topic: ${env.TOPIC_NAME}"
                                echo "🚨 This action cannot be undone!"
                                
                                // Add a countdown for dramatic effect and last chance to think
                                for (int i = 5; i > 0; i--) {
                                    echo "⏱️ Deleting in ${i} seconds... (Pipeline can still be aborted)"
                                    sleep(time: 1, unit: 'SECONDS')
                                }
                                
                                confluentOps.deleteTopic(env.COMPOSE_DIR, env.TOPIC_NAME)
                                
                                def successMessage = "✅ Successfully deleted topic: ${env.TOPIC_NAME}"
                                echo successMessage
                                
                                writeFile file: 'delete_result.txt', text: "Success: Topic '${env.TOPIC_NAME}' has been permanently deleted"
                                
                            } catch (Exception e) {
                                def errorMessage = "❌ Failed to delete topic ${env.TOPIC_NAME}: ${e.message}"
                                echo errorMessage
                                writeFile file: 'delete_result.txt', text: "Error: ${e.message}"
                                error errorMessage
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'delete_result.txt', allowEmptyArchive: true
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def operationName = params.OPERATION?.replace('_', ' ')?.toLowerCase()?.split(' ')?.collect { it.capitalize() }?.join(' ')
                echo "🏁 Topic management operation completed: ${operationName}"
                
                // Archive all result files
                archiveArtifacts artifacts: '*_result.txt', allowEmptyArchive: true
            }
        }
        success {
            echo "✅ Pipeline completed successfully!"
            script {
                switch(params.OPERATION) {
                    case 'CREATE_TOPIC':
                        echo "💡 Next steps: You can now produce/consume messages to/from '${env.TOPIC_NAME}'"
                        echo "📊 Topic created with ${env.PARTITIONS} partitions and replication factor ${env.REPLICATION_FACTOR}"
                        break
                    case 'DELETE_TOPIC':
                        echo "🗑️ Topic '${env.TOPIC_NAME}' has been permanently deleted"
                        echo "⚠️ All data associated with this topic is now lost"
                        break
                    case 'LIST_TOPICS':
                        echo "📋 All available topics have been listed above"
                        break
                    case 'DESCRIBE_TOPIC':
                        echo "🔍 Topic details have been displayed above"
                        break
                }
            }
        }
        failure {
            echo "❌ Pipeline failed. Check the logs for details."
            script {
                echo "💡 Common troubleshooting steps:"
                echo "   🔧 Ensure Kafka cluster is running and accessible"
                echo "   📝 Verify topic names use only valid characters (alphanumeric, dots, underscores, hyphens)"
                echo "   🔍 For CREATE: Check if topic already exists"
                echo "   🔍 For DESCRIBE/DELETE: Check if topic exists"
                echo "   🔒 Verify sufficient permissions for the operation"
                echo "   🌐 Check network connectivity to Kafka brokers"
            }
        }
        aborted {
            echo "⏹️ Pipeline was aborted by user"
            script {
                if (params.OPERATION == 'DELETE_TOPIC') {
                    echo "✅ Topic deletion was safely cancelled"
                }
            }
        }
    }
}