def call(Map args) {
  node {
    stage('Build') {
      echo 'Building...'
      echo "Command: ${args.command}"
    }
  }
}


