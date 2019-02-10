jsl = library(
  identifier: "jenkins-shared-library@${env.BRANCH_NAME}",
  retriever: modernSCM(
    [
      $class: 'GitSCMSource',
      remote: 'https://github.com/hoto/jenkins-shared-library.git'
    ]
  )
)

build = jsl.com.mycompany.jenkins.Build.new(this)
git = jsl.com.mycompany.jenkins.Git.new(this)

pipeline {

  agent {
    docker {
      image 'docker.io/gradle:4.5.1-jdk8'
      args '-v /root/.gradle:/home/gradle/.gradle'
    }
  }

  options {
    timeout(time: 5, unit: 'MINUTES')
  }

  stages {

    stage('Init') {
      steps {
        script {
          COMMIT_MESSAGE = git.commitMessage()
          COMMIT_AUTHOR = git.commitAuthor()

          build.setBuildDescription(
            message: COMMIT_MESSAGE,
            description: COMMIT_AUTHOR
          )
        }
      }
    }

    stage('Unit Tests') {
      steps {
        script {
          sh './gradlew test'
        }
      }
    }
  }
}


