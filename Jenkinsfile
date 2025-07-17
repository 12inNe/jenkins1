pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'Hello, World!'
            }
        }

        stage('Fix Branch Detected') {
            when {
                branch pattern: "fix-*", comparator: "REGEXP"
            }
            steps {
                echo "This branch name matches 'fix'"
            }
        }

        stage('Pull Request Check') {
            when {
                expression {
                    return env.CHANGE_ID != null
                }
            }
            steps {
                echo "This is a Pull Request build. PR ID: ${env.CHANGE_ID}"
            }
        }
    }
}
