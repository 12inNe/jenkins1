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
