/**
 * Simple pipeline utilizing fluent interface.
 */
def call(Map args) {
  node {
    def build = args.jsl.com.mycompany.jenkins.Build.new(this)
    build.setBuildDescription([
      title      : 'My build title',
      description: 'My build description'
    ])
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
