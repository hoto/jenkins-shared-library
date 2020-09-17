package com.mycompany.jenkins

class ContainerBuildStepConform implements Serializable {

  private final def script

    ContainerBuildStepConform(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  boolean scan() {
    script.sh """
              set -Eeux

              IS_IN_AUTOMATION="" /entrypoint.sh
              """
  }

}

