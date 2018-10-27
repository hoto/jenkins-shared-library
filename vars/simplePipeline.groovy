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

def build(Map args) {
  node {
    stage('Build') {
      echo "Command: ${args.command}"
    }
  }
  return this
}

def unitTest(Map args) {
  node {
    stage('Unit Test') {
      echo "Command: ${args.command}"
    }
  }
  return this
}

def deploy(Map args) {
  node {
    stage('Deploy') {
      echo "Command: ${args.command}"
    }
  }
  return this
}
