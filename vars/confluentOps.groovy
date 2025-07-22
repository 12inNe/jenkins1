def checkServices(composeDir) {
    sh """
    echo "Checking if compose directory exists..."
    ls -la ${composeDir}

    echo "Checking if docker-compose.yml exists..."
    ls -la ${composeDir}/docker-compose.yml

    echo "Checking running containers..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml ps
    """
}

def waitForServices(composeDir, maxRetries = 30) {
    script {
        def retryCount = 0
        def servicesReady = false

        while (retryCount < maxRetries && !servicesReady) {
            try {
                sh """
                echo "Checking Kafka Broker..."
                docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
                exec -T broker bash -c "echo 'Broker is responsive'"

                echo "Checking Schema Registry..."
                docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
                exec -T schema-registry curl -f -s http://localhost:8081/subjects > /dev/null
                """
                servicesReady = true
                echo "All services are ready!"
            } catch (Exception e) {
                echo "Services not ready yet, waiting... (attempt ${retryCount + 1}/${maxRetries})"
                sleep(10)
                retryCount++
            }
        }

        if (!servicesReady) {
            error("Services failed to start after ${maxRetries} attempts")
        }
    }
}

def createKafkaClientConfig(composeDir) {
    sh """
    echo "Creating client.properties file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'cat > /tmp/client.properties << "EOF"
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
EOF'

    echo "Verifying client.properties..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "cat /tmp/client.properties"
    """
}

def schemaRegistryHealthCheck(composeDir) {
    sh """
    echo "📊 Schema Registry Health Check..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c "
        echo 'Registry URL: http://localhost:8081'
        echo 'Mode:' && curl -s http://localhost:8081/mode
        echo 'Compatibility Level:' && curl -s http://localhost:8081/config
        echo 'Current Subjects:' && curl -s http://localhost:8081/subjects
    "
    """
}

def listSchemaSubjects(composeDir) {
    sh """
    echo "📋 Listing existing schema subjects..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c "
        RESPONSE=\\\$(curl -s http://localhost:8081/subjects)
        echo 'Raw subjects response:'
        echo \"\\\$RESPONSE\"
        if [ \"\\\$RESPONSE\" = '[]' ]; then
            echo 'No existing subjects found'
        else
            echo 'Subjects found in registry'
        fi
    "
    """
}

def createTopic(composeDir, topicName, partitions = 3, replicationFactor = 1) {
    sh """
    echo "Creating test topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "
        export KAFKA_OPTS=''
        export JMX_PORT=''
        export KAFKA_JMX_OPTS=''
        unset JMX_PORT
        unset KAFKA_JMX_OPTS
        kafka-topics --create --topic ${topicName} --bootstrap-server localhost:9092 --command-config /tmp/client.properties --partitions ${partitions} --replication-factor ${replicationFactor} --if-not-exists
    "
    """
}

def registerAvroSchema(composeDir, topicName, schemaContent) {
    sh """
    echo "Registering Avro schema for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c 'curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \\
        --data '"'"'{
            "schema": "${schemaContent}"
        }'"'"' \\
        http://localhost:8081/subjects/${topicName}-value/versions'
    """
}

def verifySchemaRegistration(composeDir, topicName) {
    sh """
    echo "Verifying schema registration for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T schema-registry bash -c 'echo "Schema registration response:" && \\
        curl -s http://localhost:8081/subjects/${topicName}-value/versions/latest && \\
        echo "" && \\
        echo "Schema verification completed"'
    """
}

def createTestData(composeDir, testData) {
    sh """
    echo "Creating test data file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'cat > /tmp/test-data.json << "DATA_EOF"
${testData}
DATA_EOF'

    echo "Verifying test data file..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "cat /tmp/test-data.json"
    """
}

def produceMessagesWithSchema(composeDir, topicName, schemaRegistryUrl = "http://schema-registry:8081") {
    sh """
    echo "Producing messages with JSON Schema for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'export KAFKA_OPTS="" && \\
        export JMX_PORT="" && \\
        export KAFKA_JMX_OPTS="" && \\
        unset JMX_PORT && \\
        unset KAFKA_JMX_OPTS && \\
        cat > /tmp/producer.properties << "PRODUCER_EOF"
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
schema.registry.url=${schemaRegistryUrl}
PRODUCER_EOF
        echo "Producer config created:" && \\
        cat /tmp/producer.properties && \\
        echo "Producing messages..." && \\
        kafka-console-producer --bootstrap-server localhost:9092 --topic ${topicName} --producer.config /tmp/producer.properties < /tmp/test-data.json'
    """
}

def consumeMessagesWithSchema(composeDir, topicName, schemaRegistryUrl = "http://schema-registry:8081", consumerGroup = "test-consumer-group") {
    sh """
    echo "Consuming messages with schema validation for topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'export KAFKA_OPTS="" && \\
        export JMX_PORT="" && \\
        export KAFKA_JMX_OPTS="" && \\
        unset JMX_PORT && \\
        unset KAFKA_JMX_OPTS && \\
        cat > /tmp/consumer.properties << "CONSUMER_EOF"
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
value.deserializer=io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer
schema.registry.url=${schemaRegistryUrl}
group.id=${consumerGroup}
auto.offset.reset=earliest
CONSUMER_EOF
        echo "Consumer config created:" && \\
        cat /tmp/consumer.properties && \\
        echo "Consuming messages for 15 seconds..." && \\
        timeout 15s kafka-console-consumer --bootstrap-server localhost:9092 --topic ${topicName} --consumer.config /tmp/consumer.properties --from-beginning || true'
    """
}

def listAllTopics(composeDir) {
    echo "📋 Listing all Kafka topics..."

    def topicsOutput = sh(
        script: """
            docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
            exec -T broker bash -c "
                export KAFKA_OPTS=''
                export JMX_PORT=''
                export KAFKA_JMX_OPTS=''
                unset JMX_PORT
                unset KAFKA_JMX_OPTS
                kafka-topics --list --bootstrap-server localhost:9092 --command-config /tmp/client.properties
            "
        """,
        returnStdout: true
    ).trim()

    def topicsList = topicsOutput.split('\n').findAll { it.trim() != '' }

    return topicsList
}

def getTopicDetails(composeDir, topicName) {
    sh """
    echo "📊 Topic details for ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "
        export KAFKA_OPTS=''
        export JMX_PORT=''
        export KAFKA_JMX_OPTS=''
        unset JMX_PORT
        unset KAFKA_JMX_OPTS
        kafka-topics --describe --topic ${topicName} --bootstrap-server localhost:9092 --command-config /tmp/client.properties
    "
    """
}

def deleteTopic(composeDir, topicName) {
    sh """
    echo "Deleting topic: ${topicName}..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c "
        export KAFKA_OPTS=''
        export JMX_PORT=''
        export KAFKA_JMX_OPTS=''
        unset JMX_PORT
        unset KAFKA_JMX_OPTS
        kafka-topics --delete --topic ${topicName} --bootstrap-server localhost:9092 --command-config /tmp/client.properties
    "
    """
}

def debugFileContents(composeDir) {
    sh """
    echo "🔍 Debug: Checking all created files..."
    docker compose --project-directory ${composeDir} -f ${composeDir}/docker-compose.yml \\
    exec -T broker bash -c 'echo "=== Client Properties ===" && \\
        cat /tmp/client.properties && \\
        echo "" && \\
        echo "=== Test Data JSON ===" && \\
        cat /tmp/test-data.json && \\
        echo "" && \\
        echo "=== Producer Properties ===" && \\
        cat /tmp/producer.properties && \\
        echo "" && \\
        echo "=== Consumer Properties ===" && \\
        cat /tmp/consumer.properties && \\
        echo "" && \\
        echo "=== File sizes ===" && \\
        ls -la /tmp/*.properties /tmp/*.json'
    """
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