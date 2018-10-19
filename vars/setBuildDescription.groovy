def call(Map args) {
    stage('Set Build Description') {
        steps {
            script {
                echo 'Setting build description'
            }
        }
    }
}


