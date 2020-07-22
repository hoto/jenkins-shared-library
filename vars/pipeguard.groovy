// vars/pipeguard.groovy

import com.mycompany.jenkins.BuildConfig

// from https://stackoverflow.com/questions/54124966/passing-parameters-from-jenkinsfile-to-a-shared-library
def call(def body = [:]) {
  // evaluate the body block, and collect configuration into the object
  config = BuildConfig.resolve(body)

  println "----- CONFIG DUMP -----"
  println config.dump()

  // env not available here

  upstreamProjectName = currentBuild.upstreamBuilds ? currentBuild.upstreamBuilds[0].getProjectName() : ""

  isUpstreamAutoJob = (upstreamProjectName != "" && upstreamProjectName.substring(0, 4).toLowerCase() == 'auto')

  isInAutomation = env.IS_IN_AUTOMATION.toBoolean() || isUpstreamAutoJob

  isTfDestroy = env.IS_TF_DESTROY.toBoolean() || (upstreamProjectName == "Auto-GKE-Destroy")
  tfDestroyFlag = isTfDestroy ? "--tf-override -destroy" : ""
  tfLocalStateFlag = isTfDestroy && isInAutomation ? "" : "--local-state"
  print "tfDestroyFlag: ${tfDestroyFlag}"
  tfForceFlag = isInAutomation ? "--tf-force" : ""

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
  defaultFlags = " --mount-output-path ${artifactOutputPath} ${tfForceFlag}"

  isEnvFile = ""
  if (env.TF_VARS_OVERRIDE != "") {
    isEnvFile = "1"
  }

  def agentConfigImage = 'docker.io/controlplane/gcloud-sdk:latest'
// http://man7.org/linux/man-pages/man7/capabilities.7.html
  def agentConfigArgs = '-v /var/run/docker.sock:/var/run/docker.sock ' +
    '--tmpfs /tmp ' +
    '--user=root ' +
    '--cap-drop=ALL ' +
    '--cap-add=DAC_OVERRIDE ' +
    '--cap-add=CHOWN ' +
    '--cap-add=FOWNER ' +
    '--cap-add=DAC_READ_SEARCH '


  // env, credentials, and other pipeline helpers available
  pipeline {
    agent none

    environment {
      ENVIRONMENT = 'ops'
    }

    stages {

      stage('Run Makefile') {

        when {
          expression {
            return env.MAKEFILE_RECIPE_TARGET.trim() != ''
          }
        }

        agent {
          docker {
            image agentConfigImage
            args agentConfigArgs
          }
        }

        options {
          timeout(time: 25, unit: 'MINUTES')
          retry(1)
          timestamps()
//        disableConcurrentBuilds()
        }

        environment {
          DOCKER_REGISTRY_CREDENTIALS = credentials("${ENVIRONMENT}_docker_credentials")
          TMP_AWS_CREDENTIALS = credentials("${ENVIRONMENT}_aws_credentials")
          AWS_ACCESS_KEY_ID = "${TMP_AWS_CREDENTIALS_USR}"
          AWS_SECRET_ACCESS_KEY = "${TMP_AWS_CREDENTIALS_PSW}"

          // TODO: move digitalocean secret to ops
          DIGITALOCEAN_TOKEN = credentials("dev-digitalocean_digitalocean_token")
          BASE64_SSH_KEY = credentials("dev-digitalocean_ssh_credentials")

          GITHUB_TOKEN = credentials("${ENVIRONMENT}_github_api_token")
          BASE64_GOOGLE_CREDENTIALS = credentials("gcp-iam-master_gcp_credentials")

          // TODO(ajm) this has been superseded by `kube_secret_yaml_base64`, but will work until migrated
          // TODO(ajm) allow this to be set via a param e.g. credentials(env.CRED_BLAH || "gke-dev_gke_kube_secret_yaml")
          KUBE_SECRET_YAML = credentials("gcp-dev_gke_kube_secret_yaml")

          // TODO(ajm) build slaves should have trust host's keys injected
//        GIT_SSH_COMMAND = "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"

        }

        steps {
          cp([:])
//          sh "env"

          validateParametersNotEmpty(['CONFIG_REPO_GIT_URL', 'CONFIG_REPO_GIT_COMMIT', 'MAKEFILE_RECIPE_TARGET'])

          echo "VALIDATED"
          echo "config"

          echo "${config}"
          echo "${config.dump()}"
          echo "env"
          echo "${env}"
          echo "${env}.dump()}"

          script {
            CONFIG_REPO_GIT_COMMIT = checkout(scm).GIT_COMMIT
            CONFIG_REPO_GIT_URL = checkout(scm).GIT_URL
            CONFIG_REPO_DIRECTORY = sh(
              script: "basename \"${CONFIG_REPO_GIT_URL}\"",
              returnStdout: true
            ).trim()
          }

          echo "CONFIG_REPO_DIRECTORY"
          echo "${CONFIG_REPO_DIRECTORY}"
          echo "CONFIG_REPO_GIT_URL"
          echo "${CONFIG_REPO_GIT_URL}"

//          dir("${CONFIG_REPO_DIRECTORY}") {
//            // TODO(ajm) decompose to library
//            checkout(
//              changelog: false,
//              poll: false,
//              scm: [
//                $class                           : 'GitSCM',
//                branches                         : [[name: CONFIG_REPO_GIT_COMMIT]],
//                doGenerateSubmoduleConfigurations: false,
//                extensions                       : [
//                  [
//                    $class: 'CleanBeforeCheckout'
//                  ],
//                  [
//                    $class   : 'CloneOption',
//                    depth    : 0,
//                    noTags   : true,
//                    reference: '',
//                    shallow  : true,
//                    timeout  : 2
//                  ]
//                ],
//                submoduleCfg                     : [],
//                userRemoteConfigs                : [[
//                                                      url          : CONFIG_REPO_GIT_URL,
//                                                      credentialsId: 'ops_git_environment_credentials'
//                                                    ]]
//              ]
//            )
//          }

          ansiColor('xterm') {

            validateParametersNotEmpty(['CONFIG_REPO_GIT_URL', 'CONFIG_REPO_GIT_COMMIT', 'MAKEFILE_RECIPE_TARGET'])

            script {

              try {
                def exitCode = sh(
                  returnStatus: true,
                  script:
                    """#!/bin/bash
                      set -euxo pipefail

                      hr () {
                          set +x
                          printf '=%.0s' \$(seq 120);
                          echo
                          if [[ "\${*:-}" != "" ]]; then
                            echo "\${*}"
                          fi
                          printf '=%.0s' \$(seq 120);
                          echo
                          set -x
                      }

                      EXTRA_FLAGS=""
                      if [[ "${isEnvFile}" != "" ]]; then
                        cat <<END_OF_TF_VARS_OVERRIDE | tee tf-vars-override.tfvars
${env.TF_VARS_OVERRIDE}
END_OF_TF_VARS_OVERRIDE

                        EXTRA_FLAGS+="--tf-vars tf-vars-override.tfvars "
                      fi

                      echo make ${env.MAKEFILE_RECIPE_TARGET} \
                        FLAGS="${defaultFlags} ${tfDestroyFlag} --tf-local-state --image-version ${getContainerTag()} \${EXTRA_FLAGS}" \
                        ${SET_ENV_TEMPLATE}

                      hr
                      id
                      pwd
                      ls -lasp
                      hr

                      mkdir -p "${artifactOutputPath}"
                      JENKINS_UID=\$(stat -c %u Jenkinsfile 2>/dev/null || echo 1000)
                      JENKINS_GID=\$(stat -c %g Jenkinsfile 2>/dev/null || echo 1000)
                      chown -R "\${JENKINS_UID}:\${JENKINS_GID}" .

                      EXTRA_FLAGS=""
                      if [[ "${isEnvFile}" != "" ]]; then
                        cat <<END_OF_TF_VARS_OVERRIDE | tee tf-vars-override.tfvars
${env.TF_VARS_OVERRIDE}
END_OF_TF_VARS_OVERRIDE

                        EXTRA_FLAGS+="--tf-vars tf-vars-override.tfvars "
                      fi

                       hr
                      # debug permissions in /tmp
                      find /tmp -printf '%M %u:%g %p\\n'
                      hr

                      echo '${DOCKER_REGISTRY_CREDENTIALS_PSW}' \\
                      | docker login \\
                        --username '${DOCKER_REGISTRY_CREDENTIALS_USR}' \\
                        --password-stdin
                      hr

                      make ${env.MAKEFILE_RECIPE_TARGET} \
                        FLAGS="${defaultFlags} ${tfDestroyFlag} --tf-local-state --image-version ${getContainerTag()} \${EXTRA_FLAGS}" \
                        ${SET_ENV_TEMPLATE}
                      hr

                      chown -R "\${JENKINS_UID}:\${JENKINS_GID}" ..
                    """)

                if (exitCode == 0) {
                  // exitCode 0: terragrunt plan has no changes
                  sh """
                  echo 'Exit code: 0'
                  """
                } else if (exitCode == 2 && !isInAutomation) {
                  // exitCode 2: terragrunt plan has changes. If in automation, stage 1 will have run with --tf-force

                  try {
                    timeout(time: 15, unit: 'MINUTES') {

                      approveChanges = input(message: 'Do you want to apply these changes?', submitterParameter: 'approvalUser', submitter: 'authenticated', parameters: [
                        [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'The box needs to be ticked to apply changes!', name: 'approved']
                      ])
                    }
                  } catch (err) {
                    // timeout reached or input false
                    def user = err.getCauses()[0].getUser()

                    if ('SYSTEM' == user.toString()) {
                      // SYSTEM means timeout or job cancelled
                      error "Aborted due to approval time out or job cancellation"

                    } else {
                      error "Changes aborted by user: ${user}"
                    }
                  }

                  if (approveChanges.approved == false) {
                    // TODO: reset timeout to zero after approval
                    error "Changes denied by user: ${approveChanges.approvalUser}"

                  } else if (approveChanges.approved == true) {
                    echo "Changes approved by user: ${approveChanges.approvalUser}"
                    echo "Re-running Terraform to apply the changes"

                    sh """#!/bin/bash
                      set -euxo pipefail

                      pwd
                      ls -lasp
                      cd cp-config
                      ls -lasp
                      make ${env.MAKEFILE_RECIPE_TARGET} \
                        FLAGS="${defaultFlags} ${tfDestroyFlag} --image-version ${getContainerTag()} --tf-local-state --tf-apply" \
                        ${SET_ENV_TEMPLATE}
                    """
                  }

                } else {
                  error("Unknown `Makefile` or `pipeline` error, bash exit code: ${exitCode}")
                }

                // push container image

                // TODO(ajm) isolate DOCKER_REGISTRY_CREDENTIALS to this block
                sh """#!/bin/bash
                  set -euxo pipefail

                  echo '${DOCKER_REGISTRY_CREDENTIALS_PSW}' \
                  | docker login \
                    --username '${DOCKER_REGISTRY_CREDENTIALS_USR}' \
                    --password-stdin

                  make push CONTAINER_TAG="${getContainerTag()}"
                """

              } finally {
                // reset filesystem permissions, create and archive artifacts

                sh """#!/bin/bash
                  set -euxo pipefail

                  pwd
                  mkdir -p _artifacts/output/
                  echo "\${BUILD_NUMBER}" > _artifacts/output/_buildNumber
                  echo "\${JOB_NAME}" > _artifacts/output/_jobName

                  JENKINS_UID=\$(stat -c %u Jenkinsfile 2>/dev/null || echo 1000)
                  JENKINS_GID=\$(stat -c %g Jenkinsfile 2>/dev/null || echo 1000)
                  chown -R "\${JENKINS_UID}:\${JENKINS_GID}" .
                """

                archiveArtifacts(
                  artifacts: "_artifacts/**",
                  fingerprint: true
                )

                // sh "find . -printf '%M %u:%g %p\\n' || true"
              }
            }
          }
        } //steps
      } // stage
    }
  }
}

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

def getContainerTag() {
  if (env.GIT_COMMIT == "") {
    buildError "GIT_COMMIT value was empty at usage. "
  }

  return "latest"
//  return "${env.BUILD_ID}-${env.GIT_COMMIT}"
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
