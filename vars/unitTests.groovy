def call(Map args) {
    stage('Unit Tests') {
        steps {
            script {
                echo 'Running unit tests...'
            }
        }
    }
}


