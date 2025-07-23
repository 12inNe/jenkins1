// Job: kafka-ops/register-schema
@Library('kafka-ops-shared-lib') _

properties([
    parameters([
        string(name: 'TopicName', defaultValue: '', description: 'Topic name for schema registration'),
        choice(name: 'SchemaType', choices: ['AVRO', 'JSON'], description: 'Type of schema to register'),
        text(name: 'SchemaContent', defaultValue: '', description: 'Schema content (JSON format)'),
        string(name: 'SchemaFile', defaultValue: '', description: 'Path to schema file (alternative to SchemaContent)'),
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
        SCHEMA_REGISTRY_URL = 'http://schema-registry:8081'
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
                        if (envParams.size() >= 3 && envParams[2]) env.SCHEMA_REGISTRY_URL = envParams[2]
                        
                        echo "Using environment parameters:"
                        echo "  COMPOSE_DIR: ${env.COMPOSE_DIR}"
                        echo "  CONNECTION_TYPE: ${env.CONNECTION_TYPE}"
                        echo "  SCHEMA_REGISTRY_URL: ${env.SCHEMA_REGISTRY_URL}"
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
                    
                    if (!params.SchemaContent?.trim() && !params.SchemaFile?.trim()) {
                        error "Either SchemaContent or SchemaFile must be provided"
                    }
                    
                    if (params.SchemaContent?.trim() && params.SchemaFile?.trim()) {
                        error "Provide either SchemaContent or SchemaFile, not both"
                    }
                    
                    echo "Validation passed for schema registration"
                    echo "Topic: ${params.TopicName}"
                    echo "Schema Type: ${params.SchemaType}"
                }
            }
        }

        stage('Setup Kafka Environment') {
            steps {
                script {
                    echo "Setting up Kafka environment at ${env.COMPOSE_DIR}"
                    confluentOps.waitForServices(env.COMPOSE_DIR)
                    confluentOps.createKafkaClientConfig(env.COMPOSE_DIR)
                    confluentOps.schemaRegistryHealthCheck(env.COMPOSE_DIR)
                    echo "Kafka environment ready"
                }
            }
        }

        stage('Prepare Schema Content') {
            steps {
                script {
                    def schemaContent = ""
                    
                    if (params.SchemaContent?.trim()) {
                        schemaContent = params.SchemaContent.trim()
                        echo "Using provided schema content"
                    } else if (params.SchemaFile?.trim()) {
                        try {
                            schemaContent = readFile(params.SchemaFile).trim()
                            echo "Read schema from file: ${params.SchemaFile}"
                        } catch (Exception e) {
                            error "Failed to read schema file '${params.SchemaFile}': ${e.message}"
                        }
                    }
                    
                    // Validate JSON format
                    try {
                        def jsonSlurper = new groovy.json.JsonSlurperClassic()
                        jsonSlurper.parseText(schemaContent)
                        echo "Schema JSON validation passed"
                    } catch (Exception e) {
                        error "Invalid JSON schema format: ${e.message}"
                    }
                    
                    // Store for next stage
                    env.SCHEMA_CONTENT = schemaContent
                }
            }
        }

        stage('Check Existing Subjects') {
            steps {
                script {
                    echo "Checking existing schema subjects..."
                    confluentOps.listSchemaSubjects(env.COMPOSE_DIR)
                }
            }
        }

        stage('Register Schema') {
            steps {
                script {
                    echo "📝 Registering ${params.SchemaType} schema for topic: ${params.TopicName}"
                    
                    // Escape the schema content for shell execution
                    def escapedSchema = env.SCHEMA_CONTENT.replace('"', '\\"').replace('\n', '').replace('\r', '')
                    
                    confluentOps.registerAvroSchema(
                        env.COMPOSE_DIR,
                        params.TopicName,
                        escapedSchema
                    )
                    
                    echo "✅ Schema registered successfully"
                }
            }
        }

        stage('Verify Schema Registration') {
            steps {
                script {
                    echo "🔍 Verifying schema registration for topic: ${params.TopicName}"
                    
                    confluentOps.verifySchemaRegistration(env.COMPOSE_DIR, params.TopicName)
                    
                    echo "✅ Schema registration verified"
                }
            }
        }

        stage('List Updated Subjects') {
            steps {
                script {
                    echo "📋 Updated schema subjects after registration:"
                    confluentOps.listSchemaSubjects(env.COMPOSE_DIR)
                }
            }
        }
    }

    post {
        success {
            echo "✅ Schema for topic '${params.TopicName}' has been registered successfully"
        }
        failure {
            echo "❌ Failed to register schema for topic '${params.TopicName}' - check logs for details"
        }
        always {
            echo "Register schema operation completed"
        }
    }
}