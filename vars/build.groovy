def call(Map args) {
    stage('Build') {
        steps {
            script {
                echo 'Building...'
                echo "Command: ${args.command}"
            }
        }
    }
}


