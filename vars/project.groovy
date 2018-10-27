def call(Map args) {
  node {
    echo 'call()'
  }
  return this
}

def build() {
  node {
    stage('Build') {
      echo 'build()'
    }
  }
  return this
}

def unitTest() {
  node {
    stage('Unit Test') {
      echo 'unitTest()'
    }
  }
  return this
}

def deploy() {
  node {
    stage('Deploy') {
      echo 'deploy()'
    }
  }
  return this
}
