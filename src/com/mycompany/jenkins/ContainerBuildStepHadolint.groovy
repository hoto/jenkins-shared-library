package com.mycompany.jenkins

class ContainerBuildStepHadolint implements Serializable {

  private final def script

  ContainerBuildStepHadolint(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  boolean scan(def dockerfiles = "Dockerfile") {
    script.sh """
              IS_IN_AUTOMATION="" /entrypoint.sh ${dockerfiles}
              """
  }

}

