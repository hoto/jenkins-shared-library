// vars/pipelineDemo.groovy

import com.mycompany.jenkins.*

// from https://stackoverflow.com/questions/54124966/passing-parameters-from-jenkinsfile-to-a-shared-library
def call(def body = [:]) {
  // evaluate the body block, and collect configuration into the object
  config = BuildConfig.resolve(body)

  println "----- CONFIG DUMP -----"
//  println config.dump()
  println config.stages.dump()

  // env not available here


  upstreamProjectName = currentBuild.upstreamBuilds ? currentBuild.upstreamBuilds[0].getProjectName() : ""

  isUpstreamAutoJob = (upstreamProjectName != "" && upstreamProjectName.substring(0, 4).toLowerCase() == 'auto')

  env.ENV_TEMPLATE = env.ENV_TEMPLATE ?: ""
  if (env.MAKEFILE_RECIPE_TARGET == "run-51-workshop-template" && env.ENV_TEMPLATE != "") {
    SET_ENV_TEMPLATE = "ENVIRONMENT_TEMPLATE=${env.ENV_TEMPLATE}"
  } else {
    SET_ENV_TEMPLATE = ""
  }

  // cp-config is checked out to a first-level subdirectory, so this path must traverse upwards once
  // todo(ajm) cp-config must be checked out when running this pipeline from a non-cp-config job
//  artifactOutputPath = "../_artifacts/output/"
  artifactOutputPath = "_artifacts/output/"

  isEnvFile = ""
  if (env.TF_VARS_OVERRIDE != "") {
    isEnvFile = "1"
  }

  def agentConfigImage = 'docker.io/controlplane/gcloud-sdk:latest'

  def imageStepHadolint = 'controlplaneio/build-step-hadolint:ajm-test'
  def imageStepConform = 'controlplaneio/build-step-conform:ajm-test'
  def imageStepGitSecrets = 'controlplaneio/build-step-git-secrets:ajm-test'

  def imageStepConfigArgs =
    '-e IS_IN_AUTOMATION=true ' +
    '--tmpfs /tmp ' +
    '--cap-drop=ALL ' +
    '--cap-add=DAC_OVERRIDE ' +
    '--cap-add=CHOWN ' +
    '--cap-add=FOWNER ' +
    '--cap-add=DAC_READ_SEARCH '

// http://man7.org/linux/man-pages/man7/capabilities.7.html
  def agentConfigArgs = imageStepConfigArgs +
    '-v /var/run/docker.sock:/var/run/docker.sock ' +
    '--user=root '

  // env, credentials, and other pipeline helpers available
  pipeline {
    agent none

    environment {
      ENVIRONMENT = 'ops'
    }

    options {
      parallelsAlwaysFailFast()
    }

    stages {

      // ------------------------------------------------------------------------------------------------
      // ------------------------------------------------------------------------------------------------

      stage ("Static Analysis and Build") {

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
          } // stage

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
          } // stage

          stage('Dockerfile lint') {

            when { beforeAgent true; expression { return isStageConfigured(config.stages.containerLint) } }
            agent { docker { image imageStepHadolint; args imageStepConfigArgs; } }
            options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

            steps {
              // wrap with in-toto
              cp([:])
              script {
                new ContainerBuildStepHadolint(this).scan()
              }
            } //steps
          } // stage

          stage('Image build') {

            when { beforeAgent true; expression { return isStageConfigured(config.stages.containerBuild) } }
            agent { docker { image agentConfigImage; args agentConfigArgs; } }
            options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

            steps {
              script {
                new ContainerBuildStepDockerBuild(this).build(imageTag)
              }

            } //steps
          } // stage

        }
      }

      // ------------------------------------------------------------------------------------------------
      // ------------------------------------------------------------------------------------------------

      stage('Image scan') {

        when { beforeAgent true; expression { return isStageConfigured(config.stages.containerScan) } }

        agent {
          docker {
            image agentConfigImage
            args agentConfigArgs
          }
        }

        options { timeout(time: 25, unit: 'MINUTES'); retry(1); timestamps() }

        steps {
          script {
            new ContainerBuildStepTrivy(this).scan(imageTag)
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

  return sh (
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


