pipeline {
    agent any
<<<<<<< Updated upstream

    stages {
        stage('Hello') {
=======
    
    // Active Choice Parameters for Schema Management
    parameters {
        // Schema type selection
        choice(
            name: 'SCHEMA_TYPE',
            choices: ['avro', 'json', 'protobuf'],
            description: 'Select schema serialization format'
        )
        
        // Dynamic schema template selection based on schema type
        activeChoice(
            name: 'SCHEMA_TEMPLATE',
            choiceType: 'SINGLE_SELECT',
            description: 'Select predefined schema template',
            referencedParameters: 'SCHEMA_TYPE',
            script: groovyScript {
                script([
                    classpath: [],
                    sandbox: false,
                    script: '''
                        if (SCHEMA_TYPE == 'avro') {
                            return [
                                'user-avro-schema',
                                'order-avro-schema', 
                                'product-avro-schema',
                                'event-avro-schema',
                                'custom-avro-schema'
                            ]
                        } else if (SCHEMA_TYPE == 'json') {
                            return [
                                'user-json-schema',
                                'order-json-schema',
                                'product-json-schema',
                                'event-json-schema',
                                'custom-json-schema'
                            ]
                        } else if (SCHEMA_TYPE == 'protobuf') {
                            return [
                                'user-proto-schema',
                                'order-proto-schema',
                                'product-proto-schema',
                                'event-proto-schema',
                                'custom-proto-schema'
                            ]
                        }
                        return ['default-schema']
                    '''
                ])
            }
        )
        
        // Schema compatibility level
        choice(
            name: 'COMPATIBILITY_LEVEL',
            choices: ['BACKWARD', 'BACKWARD_TRANSITIVE', 'FORWARD', 'FORWARD_TRANSITIVE', 'FULL', 'FULL_TRANSITIVE', 'NONE'],
            description: 'Schema compatibility level'
        )
        
        // Schema subject naming strategy
        choice(
            name: 'SUBJECT_NAMING_STRATEGY',
            choices: ['TopicNameStrategy', 'RecordNameStrategy', 'TopicRecordNameStrategy'],
            description: 'Schema subject naming strategy'
        )
        
        // Schema evolution action
        choice(
            name: 'SCHEMA_ACTION',
            choices: [
                'register-new',
                'update-existing',
                'test-compatibility',
                'evolve-schema',
                'delete-schema',
                'list-versions'
            ],
            description: 'Schema registry action to perform'
        )
        
        // Schema version (for updates/compatibility tests)
        string(
            name: 'SCHEMA_VERSION',
            defaultValue: 'latest',
            description: 'Schema version (use "latest" for most recent, or specify version number)'
        )
        
        // Boolean parameters
        booleanParam(
            name: 'VALIDATE_SCHEMA',
            defaultValue: true,
            description: 'Validate schema structure before registration'
        )
        
        booleanParam(
            name: 'TEST_SCHEMA_EVOLUTION',
            defaultValue: false,
            description: 'Test schema evolution compatibility'
        )
        
        booleanParam(
            name: 'BACKUP_EXISTING_SCHEMA',
            defaultValue: true,
            description: 'Backup existing schema before updates'
        )
    }
    
    environment {
        COMPOSE_DIR = '/confluent/cp-mysetup/cp-all-in-one'
        SCHEMA_REGISTRY_URL = 'http://localhost:8081'
        KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
        TEST_TOPIC = 'user-events'
        // Schema-specific environment variables
        SELECTED_SCHEMA_TYPE = "${params.SCHEMA_TYPE}"
        SELECTED_SCHEMA_TEMPLATE = "${params.SCHEMA_TEMPLATE}"
        SELECTED_COMPATIBILITY = "${params.COMPATIBILITY_LEVEL}"
        SELECTED_NAMING_STRATEGY = "${params.SUBJECT_NAMING_STRATEGY}"
        SCHEMA_SUBJECT = "${env.TEST_TOPIC}-value"
    }
    
    stages {
        stage('Schema Parameter Validation') {
            steps {
                script {
                    echo "=== Schema Configuration ==="
                    echo "Schema Type: ${params.SCHEMA_TYPE}"
                    echo "Schema Template: ${params.SCHEMA_TEMPLATE}"
                    echo "Compatibility Level: ${params.COMPATIBILITY_LEVEL}"
                    echo "Naming Strategy: ${params.SUBJECT_NAMING_STRATEGY}"
                    echo "Schema Action: ${params.SCHEMA_ACTION}"
                    echo "Schema Version: ${params.SCHEMA_VERSION}"
                    echo "Validate Schema: ${params.VALIDATE_SCHEMA}"
                    echo "Test Evolution: ${params.TEST_SCHEMA_EVOLUTION}"
                    echo "Backup Existing: ${params.BACKUP_EXISTING_SCHEMA}"
                    echo "Subject Name: ${env.SCHEMA_SUBJECT}"
                    echo "============================"
                }
            }
        }

        stage('Verify Docker Compose Setup') {
>>>>>>> Stashed changes
            steps {
                echo 'Hello, World!'
            }
        }

<<<<<<< Updated upstream
        stage('Fix Branch Detected') {
            when {
                branch "fix-*"
            }
            steps {
                 sh '''
                    cat README.md
                 '''
            }
        }

        stage('Pull Request Check') {
            when {
                branch "PR-*"
            }
            steps {
                echo "This only runs for the PRs"
            }
        }
    }
}
=======
        stage('Wait for Schema Registry') {
            steps {
                script {
                    def maxRetries = 30
                    def retryCount = 0
                    def schemaRegistryReady = false

                    while (retryCount < maxRetries && !schemaRegistryReady) {
                        try {
                            sh '''
                            echo "Checking Schema Registry..."
                            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                            exec -T schema-registry curl -f -s http://localhost:8081/subjects > /dev/null
                            '''
                            schemaRegistryReady = true
                            echo "Schema Registry is ready!"
                        } catch (Exception e) {
                            echo "Schema Registry not ready yet, waiting... (attempt ${retryCount + 1}/${maxRetries})"
                            sleep(10)
                            retryCount++
                        }
                    }

                    if (!schemaRegistryReady) {
                        error("Schema Registry failed to start after ${maxRetries} attempts")
                    }
                }
            }
        }

        stage('Set Schema Registry Configuration') {
            steps {
                sh '''
                echo "Setting global compatibility level to: $SELECTED_COMPATIBILITY"
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    curl -X PUT -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
                    --data '{\"compatibility\": \"'$SELECTED_COMPATIBILITY'\"}' \
                    http://localhost:8081/config
                "
                '''
            }
        }

        stage('Generate Schema Definition') {
            steps {
                script {
                    def schemaDefinition = generateSchemaDefinition(
                        params.SCHEMA_TYPE, 
                        params.SCHEMA_TEMPLATE
                    )
                    
                    writeFile file: 'schema.json', text: schemaDefinition
                    
                    echo "Generated schema definition:"
                    sh 'cat schema.json'
                }
            }
        }

        stage('Validate Schema Structure') {
            when {
                expression { params.VALIDATE_SCHEMA == true }
            }
            steps {
                script {
                    if (params.SCHEMA_TYPE == 'avro') {
                        sh '''
                        echo "Validating Avro schema structure..."
                        docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                        exec -T schema-registry bash -c "
                            # Copy schema file to container
                            cat > /tmp/schema.json << 'EOF'
$(cat schema.json)
EOF
                            
                            # Validate schema using Schema Registry API
                            curl -X POST -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
                            --data @/tmp/schema.json \
                            http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions?normalize=true
                        "
                        '''
                    }
                }
            }
        }

        stage('Backup Existing Schema') {
            when {
                expression { 
                    params.BACKUP_EXISTING_SCHEMA == true && 
                    params.SCHEMA_ACTION in ['update-existing', 'evolve-schema']
                }
            }
            steps {
                sh '''
                echo "Backing up existing schema..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    EXISTING_SCHEMA=\\$(curl -s http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions/latest 2>/dev/null || echo 'null')
                    if [ \"\\$EXISTING_SCHEMA\" != 'null' ]; then
                        echo \"Backing up existing schema:\"
                        echo \"\\$EXISTING_SCHEMA\" > /tmp/schema_backup_\\$(date +%Y%m%d_%H%M%S).json
                        echo \"\\$EXISTING_SCHEMA\"
                    else
                        echo 'No existing schema to backup'
                    fi
                "
                '''
            }
        }

        stage('Execute Schema Action') {
            steps {
                script {
                    switch(params.SCHEMA_ACTION) {
                        case 'register-new':
                            registerNewSchema()
                            break
                        case 'update-existing':
                            updateExistingSchema()
                            break
                        case 'test-compatibility':
                            testSchemaCompatibility()
                            break
                        case 'evolve-schema':
                            evolveSchema()
                            break
                        case 'delete-schema':
                            deleteSchema()
                            break
                        case 'list-versions':
                            listSchemaVersions()
                            break
                        default:
                            error("Unknown schema action: ${params.SCHEMA_ACTION}")
                    }
                }
            }
        }

        stage('Test Schema Evolution') {
            when {
                expression { params.TEST_SCHEMA_EVOLUTION == true }
            }
            steps {
                script {
                    testSchemaEvolution()
                }
            }
        }

        stage('Verify Schema Registration') {
            steps {
                sh '''
                echo "Verifying final schema state..."
                docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
                exec -T schema-registry bash -c "
                    echo 'Current schema for subject: ${SCHEMA_SUBJECT}'
                    curl -s http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions/latest | python3 -m json.tool || echo 'No schema found'
                    echo ''
                    echo 'All versions:'
                    curl -s http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions || echo 'No versions found'
                "
                '''
            }
        }
    }

    post {
        always {
            sh '''
            echo "Schema pipeline completed. Final schema registry status:"
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
            exec -T schema-registry bash -c "
                echo 'All subjects:'
                curl -s http://localhost:8081/subjects
                echo ''
                echo 'Global config:'
                curl -s http://localhost:8081/config
            " || true
            '''
        }
        success {
            sh '''
            echo "✅ Schema pipeline completed successfully!"
            echo "✅ Schema Type: $SELECTED_SCHEMA_TYPE"
            echo "✅ Schema Template: $SELECTED_SCHEMA_TEMPLATE"
            echo "✅ Schema Action: ${SCHEMA_ACTION}"
            echo "✅ Compatibility Level: $SELECTED_COMPATIBILITY"
            '''
        }
        failure {
            sh '''
            echo "❌ Schema pipeline failed. Checking Schema Registry logs..."
            docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
            logs --tail=50 schema-registry || true
            '''
        }
    }
}

// Helper functions for schema operations
def generateSchemaDefinition(schemaType, templateName) {
    def schemas = [
        'avro': [
            'user-avro-schema': '''
            {
                "schema": "{\\"type\\":\\"record\\",\\"name\\":\\"User\\",\\"namespace\\":\\"com.example\\",\\"fields\\":[{\\"name\\":\\"id\\",\\"type\\":\\"int\\"},{\\"name\\":\\"name\\",\\"type\\":\\"string\\"},{\\"name\\":\\"email\\",\\"type\\":\\"string\\"},{\\"name\\":\\"age\\",\\"type\\":\\"int\\"}]}"
            }''',
            'order-avro-schema': '''
            {
                "schema": "{\\"type\\":\\"record\\",\\"name\\":\\"Order\\",\\"namespace\\":\\"com.example\\",\\"fields\\":[{\\"name\\":\\"orderId\\",\\"type\\":\\"string\\"},{\\"name\\":\\"customerId\\",\\"type\\":\\"int\\"},{\\"name\\":\\"amount\\",\\"type\\":\\"double\\"},{\\"name\\":\\"timestamp\\",\\"type\\":\\"long\\"}]}"
            }''',
            'product-avro-schema': '''
            {
                "schema": "{\\"type\\":\\"record\\",\\"name\\":\\"Product\\",\\"namespace\\":\\"com.example\\",\\"fields\\":[{\\"name\\":\\"productId\\",\\"type\\":\\"string\\"},{\\"name\\":\\"name\\",\\"type\\":\\"string\\"},{\\"name\\":\\"price\\",\\"type\\":\\"double\\"},{\\"name\\":\\"category\\",\\"type\\":\\"string\\"}]}"
            }'''
        ],
        'json': [
            'user-json-schema': '''
            {
                "schema": "{\\"type\\":\\"object\\",\\"properties\\":{\\"id\\":{\\"type\\":\\"integer\\"},\\"name\\":{\\"type\\":\\"string\\"},\\"email\\":{\\"type\\":\\"string\\"},\\"age\\":{\\"type\\":\\"integer\\"}},\\"required\\":[\\"id\\",\\"name\\",\\"email\\"]}"
            }''',
            'order-json-schema': '''
            {
                "schema": "{\\"type\\":\\"object\\",\\"properties\\":{\\"orderId\\":{\\"type\\":\\"string\\"},\\"customerId\\":{\\"type\\":\\"integer\\"},\\"amount\\":{\\"type\\":\\"number\\"},\\"timestamp\\":{\\"type\\":\\"integer\\"}},\\"required\\":[\\"orderId\\",\\"customerId\\",\\"amount\\"]}"
            }'''
        ]
    ]
    
    return schemas[schemaType]?[templateName] ?: schemas[schemaType]['user-' + schemaType + '-schema']
}

def registerNewSchema() {
    sh '''
    echo "Registering new schema..."
    docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
    exec -T schema-registry bash -c "
        curl -X POST -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data @schema.json \
        http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions
    "
    '''
}

def updateExistingSchema() {
    sh '''
    echo "Updating existing schema..."
    docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
    exec -T schema-registry bash -c "
        curl -X POST -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data @schema.json \
        http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions
    "
    '''
}

def testSchemaCompatibility() {
    sh '''
    echo "Testing schema compatibility..."
    docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
    exec -T schema-registry bash -c "
        curl -X POST -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data @schema.json \
        http://localhost:8081/compatibility/subjects/${SCHEMA_SUBJECT}/versions/${SCHEMA_VERSION}
    "
    '''
}

def evolveSchema() {
    sh '''
    echo "Evolving schema with compatibility check..."
    docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
    exec -T schema-registry bash -c "
        # First test compatibility
        COMPAT_RESULT=\\$(curl -s -X POST -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data @schema.json \
        http://localhost:8081/compatibility/subjects/${SCHEMA_SUBJECT}/versions/latest)
        
        echo 'Compatibility test result:'
        echo \"\\$COMPAT_RESULT\"
        
        # If compatible, proceed with registration
        if echo \"\\$COMPAT_RESULT\" | grep -q '\"is_compatible\":true'; then
            echo 'Schema is compatible, proceeding with evolution...'
            curl -X POST -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
            --data @schema.json \
            http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions
        else
            echo 'Schema is not compatible, evolution aborted'
            exit 1
        fi
    "
    '''
}

def deleteSchema() {
    sh '''
    echo "Deleting schema..."
    docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
    exec -T schema-registry bash -c "
        curl -X DELETE http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions/${SCHEMA_VERSION}
    "
    '''
}

def listSchemaVersions() {
    sh '''
    echo "Listing schema versions..."
    docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
    exec -T schema-registry bash -c "
        echo 'Available versions for subject: ${SCHEMA_SUBJECT}'
        curl -s http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions
        echo ''
        echo 'Latest version details:'
        curl -s http://localhost:8081/subjects/${SCHEMA_SUBJECT}/versions/latest | python3 -m json.tool
    "
    '''
}

def testSchemaEvolution() {
    sh '''
    echo "Testing schema evolution scenarios..."
    docker compose --project-directory $COMPOSE_DIR -f $COMPOSE_DIR/docker-compose.yml \
    exec -T schema-registry bash -c "
        # Test adding optional field
        echo 'Testing schema evolution with optional field...'
        EVOLVED_SCHEMA='{\"schema\": \"{\\\\"type\\\\":\\\\"record\\\\",\\\\"name\\\\":\\\\"User\\\\",\\\\"namespace\\\\":\\\\"com.example\\\\",\\\\"fields\\\\":[{\\\\"name\\\\":\\\\"id\\\\",\\\\"type\\\\":\\\\"int\\\\"},{\\\\"name\\\\":\\\\"name\\\\",\\\\"type\\\\":\\\\"string\\\\"},{\\\\"name\\\\":\\\\"email\\\\",\\\\"type\\\\":\\\\"string\\\\"},{\\\\"name\\\\":\\\\"age\\\\",\\\\"type\\\\":\\\\"int\\\\"},{\\\\"name\\\\":\\\\"phone\\\\",\\\\"type\\\\":[\\\\"null\\\\",\\\\"string\\\\"],\\\\"default\\\\":null}]}\"}'

        curl -X POST -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data \"\\$EVOLVED_SCHEMA\" \
        http://localhost:8081/compatibility/subjects/${SCHEMA_SUBJECT}/versions/latest
    "
    '''
}
>>>>>>> Stashed changes
