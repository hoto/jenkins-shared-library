package com.mycompany.jenkins

class ContainerBuildStepGitSecrets implements Serializable {

  private final def script

    ContainerBuildStepGitSecrets(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  boolean scan() {
    script.sh """
              set -Eeux

              IS_IN_AUTOMATION="" /entrypoint.sh
              """
  }

}

