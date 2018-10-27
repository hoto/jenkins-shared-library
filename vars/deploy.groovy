def call(Map args) {
    stage('Deploy') {
        steps {
            script {
                echo 'Deploy...'
                echo "Command: ${args.command}"
            }
        }
    }
}


