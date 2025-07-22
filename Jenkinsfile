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
                                    <p style="margin: 5px 0 0 0; color: #155724;">This operation will list all available Kafka topics with detailed information including count, names, partitions, and replication factors.</p>
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
                                propagate: false,
                                wait: true
                            
                            echo "==== Processing List Topics Results ===="
                            
                            // Method 1: Parse console output for topic information
                            def topicsInfo = parseTopicListOutput(listResult)
                            
                            // Method 2: Use build artifacts if available
                            def artifactData = getJobArtifacts(listResult)
                            
                            // Method 3: Create formatted summary
                            displayTopicsSummary(topicsInfo, artifactData)
                            
                            // Archive enhanced results
                            createTopicsReport(topicsInfo, listResult)
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
                                propagate: false,
                                wait: true
                            
                            processCreateTopicResult(createResult)
                            break

                        case 'DESCRIBE_TOPIC':
                            echo "==== Calling Describe Topic job ===="
                            def describeResult = build job: 'GIT-org/jenkins1/describe-topic', 
                                parameters: [
                                    string(name: 'TopicName', value: "${env.TOPIC_NAME}"),
                                    string(name: 'ParamsAsENV', value: 'true'),
                                    string(name: 'ENVIRONMENT_PARAMS', value: "${env.COMPOSE_DIR},${env.CONNECTION_TYPE}")
                                ],
                                propagate: false,
                                wait: true
                            
                            processDescribeTopicResult(describeResult)
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
                                propagate: false,
                                wait: true
                            
                            processDeleteTopicResult(deleteResult)
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
            
            script {
                def summary = generateOperationSummary()
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

// Enhanced helper functions for better output processing

def parseTopicListOutput(buildResult) {
    def topicsInfo = [:]
    try {
        def consoleOutput = getConsoleOutput(buildResult)
        
        // Parse topic names and details from console output
        def topicLines = consoleOutput.readLines().findAll { line ->
            // Adjust these patterns based on your actual kafka-topics output
            line.contains("Topic:") || line.matches(".*\\s+\\d+\\s+\\d+.*")
        }
        
        def topicNames = []
        topicLines.each { line ->
            // Extract topic names - adjust regex based on your output format
            def matcher = line =~ /Topic:\s*([^\s]+)/
            if (matcher) {
                topicNames.add(matcher[0][1])
            }
        }
        
        topicsInfo.names = topicNames
        topicsInfo.count = topicNames.size()
        topicsInfo.rawOutput = consoleOutput
        
    } catch (Exception e) {
        echo "Error parsing topic list output: ${e.message}"
        topicsInfo.error = e.message
        topicsInfo.count = 0
        topicsInfo.names = []
    }
    
    return topicsInfo
}

def getJobArtifacts(buildResult) {
    def artifactData = [:]
    try {
        def build = buildResult.getRawBuild()
        def artifacts = build.getArtifacts()
        
        artifacts.each { artifact ->
            if (artifact.fileName.endsWith('.json') || artifact.fileName.endsWith('.txt')) {
                def file = new File(build.getRootDir(), "archive/${artifact.relativePath}")
                if (file.exists()) {
                    artifactData[artifact.fileName] = file.text
                }
            }
        }
    } catch (Exception e) {
        echo "Could not retrieve artifacts: ${e.message}"
    }
    return artifactData
}

def displayTopicsSummary(topicsInfo, artifactData) {
    echo "============================================"
    echo "           KAFKA TOPICS SUMMARY            "
    echo "============================================"
    echo "📊 Total Topics Found: ${topicsInfo.count ?: 0}"
    echo "============================================"
    
    if (topicsInfo.names && topicsInfo.names.size() > 0) {
        echo "📝 Topic Names:"
        topicsInfo.names.eachWithIndex { name, index ->
            echo "   ${index + 1}. ${name}"
        }
    } else {
        echo "   No topics found or error occurred"
    }
    
    echo "============================================"
    
    // Display additional artifact information if available
    if (artifactData) {
        artifactData.each { fileName, content ->
            echo "📄 Content from ${fileName}:"
            echo content
            echo "--------------------------------------------"
        }
    }
}

def createTopicsReport(topicsInfo, buildResult) {
    def report = new StringBuilder()
    report.append("=== KAFKA TOPICS DETAILED REPORT ===\n")
    report.append("Generated: ${new Date().format('yyyy-MM-dd HH:mm:ss')}\n")
    report.append("Build: ${buildResult.number}\n")
    report.append("Status: ${buildResult.result}\n")
    report.append("Duration: ${buildResult.duration}ms\n\n")
    
    report.append("SUMMARY:\n")
    report.append("--------\n")
    report.append("Total Topics: ${topicsInfo.count ?: 0}\n\n")
    
    if (topicsInfo.names && topicsInfo.names.size() > 0) {
        report.append("TOPIC LIST:\n")
        report.append("-----------\n")
        topicsInfo.names.eachWithIndex { name, index ->
            report.append("${index + 1}. ${name}\n")
        }
        report.append("\n")
    }
    
    if (topicsInfo.error) {
        report.append("ERRORS:\n")
        report.append("-------\n")
        report.append("${topicsInfo.error}\n\n")
    }
    
    if (topicsInfo.rawOutput) {
        report.append("RAW OUTPUT:\n")
        report.append("-----------\n")
        report.append("${topicsInfo.rawOutput}\n")
    }
    
    writeFile file: 'topics-detailed-report.txt', text: report.toString()
    archiveArtifacts artifacts: 'topics-detailed-report.txt'
}

def processCreateTopicResult(createResult) {
    echo "==== Create Topic Results ===="
    echo "Status: ${createResult.result}"
    echo "Build Number: ${createResult.number}"
    echo "Duration: ${createResult.duration}ms"
    echo "Topic Name: ${env.TOPIC_NAME}"
    echo "Partitions: ${env.PARTITIONS}"
    echo "Replication Factor: ${env.REPLICATION_FACTOR}"
    
    def consoleOutput = getConsoleOutput(createResult)
    
    // Parse for success/error messages
    def success = consoleOutput.contains("Created topic") || createResult.result == 'SUCCESS'
    def errorMsg = extractErrorMessage(consoleOutput)
    
    echo "==== Create Topic Summary ===="
    if (success) {
        echo "✅ Topic '${env.TOPIC_NAME}' created successfully"
        echo "   - Partitions: ${env.PARTITIONS}"
        echo "   - Replication Factor: ${env.REPLICATION_FACTOR}"
    } else {
        echo "❌ Failed to create topic '${env.TOPIC_NAME}'"
        if (errorMsg) {
            echo "   Error: ${errorMsg}"
        }
    }
    
    // Create detailed report
    def report = """
=== CREATE TOPIC REPORT ===
Topic Name: ${env.TOPIC_NAME}
Partitions: ${env.PARTITIONS}
Replication Factor: ${env.REPLICATION_FACTOR}
Status: ${createResult.result}
Success: ${success}
Build: ${createResult.number}
Duration: ${createResult.duration}ms
${errorMsg ? "Error: ${errorMsg}" : ""}

Console Output:
${consoleOutput}
"""
    
    writeFile file: 'create-topic-detailed-report.txt', text: report
    archiveArtifacts artifacts: 'create-topic-detailed-report.txt'
}

def processDescribeTopicResult(describeResult) {
    echo "==== Describe Topic Results ===="
    def consoleOutput = getConsoleOutput(describeResult)
    
    // Parse topic details from output
    def topicDetails = parseTopicDetails(consoleOutput)
    
    echo "📋 Topic Details for '${env.TOPIC_NAME}':"
    echo "   Status: ${describeResult.result}"
    if (topicDetails.partitions) {
        echo "   Partitions: ${topicDetails.partitions}"
    }
    if (topicDetails.replicationFactor) {
        echo "   Replication Factor: ${topicDetails.replicationFactor}"
    }
    if (topicDetails.configs) {
        echo "   Configurations:"
        topicDetails.configs.each { config ->
            echo "     - ${config}"
        }
    }
    
    // Create detailed report
    def report = """
=== DESCRIBE TOPIC REPORT ===
Topic Name: ${env.TOPIC_NAME}
Status: ${describeResult.result}
Build: ${describeResult.number}
Duration: ${describeResult.duration}ms

Parsed Details:
${topicDetails.partitions ? "Partitions: ${topicDetails.partitions}" : ""}
${topicDetails.replicationFactor ? "Replication Factor: ${topicDetails.replicationFactor}" : ""}
${topicDetails.configs ? "Configs: ${topicDetails.configs.join(', ')}" : ""}

Raw Console Output:
${consoleOutput}
"""
    
    writeFile file: 'describe-topic-detailed-report.txt', text: report
    archiveArtifacts artifacts: 'describe-topic-detailed-report.txt'
}

def processDeleteTopicResult(deleteResult) {
    echo "==== Delete Topic Results ===="
    def consoleOutput = getConsoleOutput(deleteResult)
    
    def success = consoleOutput.contains("Topic") && consoleOutput.contains("marked for deletion") || 
                  consoleOutput.contains("deleted") || deleteResult.result == 'SUCCESS'
    
    if (success) {
        echo "✅ Topic '${env.TOPIC_NAME}' deleted successfully"
    } else {
        echo "❌ Failed to delete topic '${env.TOPIC_NAME}'"
        def errorMsg = extractErrorMessage(consoleOutput)
        if (errorMsg) {
            echo "   Error: ${errorMsg}"
        }
    }
}

def parseTopicDetails(consoleOutput) {
    def details = [:]
    try {
        def lines = consoleOutput.readLines()
        
        // Look for partition count
        lines.each { line ->
            if (line.contains("PartitionCount:")) {
                def matcher = line =~ /PartitionCount:\s*(\d+)/
                if (matcher) {
                    details.partitions = matcher[0][1]
                }
            }
            if (line.contains("ReplicationFactor:")) {
                def matcher = line =~ /ReplicationFactor:\s*(\d+)/
                if (matcher) {
                    details.replicationFactor = matcher[0][1]
                }
            }
        }
        
        // Extract configurations
        def configLines = lines.findAll { it.contains("Configs:") }
        if (configLines) {
            details.configs = configLines
        }
        
    } catch (Exception e) {
        echo "Error parsing topic details: ${e.message}"
        details.error = e.message
    }
    
    return details
}

def extractErrorMessage(consoleOutput) {
    try {
        def lines = consoleOutput.readLines()
        def errorLines = lines.findAll { line ->
            line.toLowerCase().contains("error") || 
            line.toLowerCase().contains("exception") ||
            line.toLowerCase().contains("failed")
        }
        return errorLines.join("; ")
    } catch (Exception e) {
        return null
    }
}

def generateOperationSummary() {
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
    
    return summary
}

def getConsoleOutput(buildResult) {
    try {
        def build = buildResult.getRawBuild()
        def logFile = build.getLogFile()
        if (logFile.exists()) {
            return logFile.text
        } else {
            return "Console log not available"
        }
    } catch (Exception e) {
        return "Error retrieving console output: ${e.message}"
    }
}