// vars/pipelineDemo.groovy

import com.mycompany.jenkins.*

def call(def body = [:]) {
  // evaluate the body block, and collect configuration into the object
  config = BuildConfig.resolve(body)

  def agentInsecureConfigImage = 'docker.io/controlplane/gcloud-sdk:latest'

  /**
   * next:
   * - common use cases for flows to map to this
   * - questions of container build step image startup time?
   * - should there be a JAR/artefact build *outside* a container?
   * - ** low level developer workflow **
   * - bitbucket: enablement, training, intrinsic value with everything as code
   *
   *
   * what about tests that require integration with a container
   * - fe apps with selenium
   * - db-close apps
   * - middleware proxies
   * - stovepipe tests
   *
   * CI vs CD differences
   * - cycle time vs stricter general lifecycle definition
   *
   * # CI
   * unit - mock interfaces
   * - static, config
   * integration
   * - component - multiple java classes/objects
   *
   * # CD
   * integration
   * - system - containers
   * acceptance
   * - smoke
   *
   */

  def imageStepConform = 'controlplaneio/build-step-conform:ajm-test'
  def imageStepHadolint = 'controlplaneio/build-step-hadolint:ajm-test'
  def imageStepGitSecrets = 'controlplaneio/build-step-git-secrets:ajm-test'

  def imageStepConfigArgs =
    '-e IS_IN_AUTOMATION=true ' +
      '--tmpfs /tmp ' +
      '--cap-drop=ALL ' +
      '--cap-add=CHOWN ' +
      '--cap-add=DAC_OVERRIDE ' +
      '--cap-add=DAC_READ_SEARCH ' +
      '--cap-add=FOWNER '

  def agentInsecureConfigArgs = imageStepConfigArgs +
    '-v /var/run/docker.sock:/var/run/docker.sock ' +
    '--user=root '

  // env, credentials, and other pipeline helpers available
  pipeline {
    agent none

    environment {
      ENVIRONMENT = 'ops'
    }

    options {
      disableConcurrentBuilds()
      parallelsAlwaysFailFast()
      ansiColor('xterm')
    }

    stages {

      // ------------------------------------------------------------------------------------------------
      // ------------------------------------------------------------------------------------------------

      stage("Static Analysis and Build") {

        parallel {

          stage('Git commit conformance') {

            when { beforeAgent true; expression { return isStageConfigured(config.stages.gitCommitConformance) } }
            agent { docker { image imageStepConform; args imageStepConfigArgs; } }
            options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

            steps {
              // wrap with in-toto
              cp([:])
              script {
                new ContainerBuildStepConform(this).scan()
              }
            } //steps
          } // analysis stage

          stage('Git secrets') {

            when { beforeAgent true; expression { return isStageConfigured(config.stages.gitSecrets) } }
            agent { docker { image imageStepGitSecrets; args imageStepConfigArgs; } }
            options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

            steps {
              // wrap with in-toto
              cp([:])
              script {
                new ContainerBuildStepGitSecrets(this).scan()
              }
            } //steps
          } // analysis stage

          stage('Dockerfile lint') {

            when { beforeAgent true; expression { return isStageConfigured(config.stages.containerLint) } }
            agent { docker { image imageStepHadolint; args imageStepConfigArgs; } }
            options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

            steps {
              // wrap with in-toto
              cp([:])
              script {
                new ContainerBuildStepHadolint(this).scan(config.stages.containerLint)
              }
            } //steps
          } // analysis stage

          // sonar analysis

          // blackduck analysis

          stage('Image build') {

            when { beforeAgent true; expression { return isStageConfigured(config.stages.containerBuild) } }
            agent { docker { image agentInsecureConfigImage; args agentInsecureConfigArgs; } }
            options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

            steps {
//              in_toto_wrap(['stepName'    : 'Build',
//                            'credentialId': "f0852160-5cc2-4389-b658-f5ee2bd82922",
//                            'transport'   : 'grafeas+http://192.168.1.11:8090']) {
              echo 'Building..'

              script {
                new ContainerBuildStepContainerBuild(this).build(imageTag, config.stages.containerBuild)
              }

//              }
            } //steps
          } // artefact stage

        }
      }

      // ------------------------------------------------------------------------------------------------
      // ------------------------------------------------------------------------------------------------

      stage('Image scan') {

        when { beforeAgent true; expression { return isStageConfigured(config.stages.containerScan) } }

        agent {
          docker {
            image agentInsecureConfigImage
            args agentInsecureConfigArgs
          }
        }

        options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

        steps {
          script {
            new ContainerBuildStepTrivy(this).scan(config.stages.containerScan, imageTag)
          }
        } //steps
      } // stage

      stage('Image push') {

        when { beforeAgent true; expression { return isStageConfigured(config.stages.containerPush) } }

        agent {
          docker {
            image agentInsecureConfigImage
            args agentInsecureConfigArgs
          }
        }

        options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }
        environment {
          DOCKER_REGISTRY_CREDENTIALS = credentials("${ENVIRONMENT}_docker_credentials")
        }

        steps {
          script {
            new ContainerBuildStepContainerPush(this).push(
              config.stages.containerPush, imageTag, DOCKER_REGISTRY_CREDENTIALS_PSW, DOCKER_REGISTRY_CREDENTIALS_USR)
          }
        } //steps
      } // stage

      // ------------------------------------------------------------------------------------------------
      // ------------------------------------------------------------------------------------------------

    } // stages
  }
}

// ------------------------------------------------------------------------------------------------
// helpers
// ------------------------------------------------------------------------------------------------

def debug(object, title = "") {
  println title
  object.dump()
}

def buildError(message = "[error message not supplied to method call]", stackTrace = false) {
  print "ERROR: ${message}"
  if (stackTrace) {
    throw new Exception(message)
  }
  error(message)
}

def getImageTag() {
  return "${getGitRepositoryName()}:${getContainerTag()}"
}

def getGitRepositoryName() {
  if (env.GIT_URL == "") {
    buildError "GIT_URL value was empty at usage."
  }

  return sh(
    script: "basename '${env.GIT_URL}'",
    returnStdout: true
  ).trim()
}

def getContainerTag() {
  if (env.GIT_COMMIT == "" || env.GIT_COMMIT == "null") {
    buildError "GIT_COMMIT value was empty at usage."
  }

//  return "latest"
  return "${env.BUILD_ID}-${env.GIT_COMMIT}"
}

def validateParametersNotEmpty(parameterNames = []) {
  def failingParameters = []

  for (parameterName in parameterNames) {
    if (env."${parameterName}" == "") {
      print "ERROR: Expected parameter ${parameterName} not to be an empty string"
      failingParameters.push(parameterName)
    }
  }

  if (failingParameters.size() != 0) {
    buildError("Some parameters are empty: ${failingParameters.join(', ')}")
  }

  return true
}

def isStageConfigured(configEntry) {
  if (configEntry in Boolean && configEntry) {
    return true
  } else if (configEntry) {
    return true
  }

  return false
}


