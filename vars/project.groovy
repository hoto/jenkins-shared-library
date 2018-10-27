def call(Map args) {
  node {
    echo 'call()'
  }
  return this
}

def build() {
  node {
    echo 'build()'
  }
  return this
}

def unitTest() {
  node {
    echo 'unitTest()'
  }
  return this
}

def deploy() {
  node {
    echo 'deploy()'
  }
  return this
}
