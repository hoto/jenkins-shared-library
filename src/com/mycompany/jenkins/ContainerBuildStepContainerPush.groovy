package com.mycompany.jenkins

class ContainerBuildStepContainerPush implements Serializable {

  private final def script

    ContainerBuildStepContainerPush(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  boolean push(def config, String imageTag, String DOCKER_REGISTRY_CREDENTIALS_PSW, String DOCKER_REGISTRY_CREDENTIALS_USR) {
    def args = []

    script.sh """
              set -Eeux

              echo '${DOCKER_REGISTRY_CREDENTIALS_PSW}' \\
                | docker login \\
                  --username '${DOCKER_REGISTRY_CREDENTIALS_USR}' \\
                  --password-stdin
              """

    if (config instanceof java.util.Map && config.cmd) {
      script.sh """
              set -Eeux

              ${config.cmd}
      """
    } else {
      script.sh """
              set -Eeux

              docker push ${args.join(" ")} ${imageTag}
              """
    }
  }
}
