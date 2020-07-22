// vars/evenOrOdd.groovy

def call(jobName) {

// def call(int buildNumber, jsl) {
//  def containerScan = jsl.com.mycompany.jenkins.ContainerScan.new(this)

//  def agentConfig = getAgentConfig()
//  def agentConfigImage = agentConfig.image
//  def agentConfigArgs = agentConfig.args

  def isPipelinePass = false

  try {

    if (env.MAKEFILE_RECIPE_TARGET == "") {
      sh """#!/bin/bash
              set -euxo pipefail

              make help
              ./pipeline --help

              """
      return 0
    }

    def downstreamJobParameters = [[
                                     $class: 'StringParameterValue',
                                     name  : 'CONFIG_REPO_GIT_COMMIT',
                                     value : checkout(scm).GIT_COMMIT
                                   ],
                                   [
                                     $class: 'StringParameterValue',
                                     name  : 'CONFIG_REPO_GIT_URL',
                                     value : checkout(scm).GIT_URL
                                   ],
                                   [
                                     $class: 'StringParameterValue',
                                     name  : 'IS_IN_AUTOMATION',
                                     value : env.IS_IN_AUTOMATION
                                   ],
                                   [
                                     $class: 'StringParameterValue',
                                     name  : 'IS_TF_DESTROY',
                                     value : env.IS_TF_DESTROY ?: "false"
                                   ],
                                   [
                                     $class: 'StringParameterValue',
                                     name  : 'MAKEFILE_RECIPE_TARGET',
                                     value : env.MAKEFILE_RECIPE_TARGET
                                   ],
                                   [
                                     $class: 'StringParameterValue',
                                     name  : 'TF_VARS_OVERRIDE',
                                     value : params.TF_VARS_OVERRIDE ?: ""
                                   ]
    ]

    println downstreamJobParameters.dump()

    def downstreamJob = build(
      job: jobName,
      propagate: true,
      parameters: downstreamJobParameters
    )

    def downstreamProject = downstreamJob.getProjectName()
    def downstreamBuildNumber = downstreamJob.getNumber()
    sh "echo 'Downstream Job: ${downstreamProject} ${downstreamBuildNumber}'"

    copyArtifacts(
      projectName: "${downstreamProject}",
      filter: "_artifacts/**",
      selector: specific("${downstreamBuildNumber}"),
      target: "_artifacts/copied/${downstreamProject}/${downstreamBuildNumber}/",
      optional: false,
      fingerprint: true
    )


    isPipelinePass = true

  } catch (Exception ex) {
    println("Caught exceptop ${ex}");


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

  return isPipelinePass
}

