package com.mycompany.jenkins

class ContainerBuildStepDockerBuild implements Serializable {

  private final def script

  ContainerBuildStepDockerBuild(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  boolean build(String imageTag, def imageCache = "") {
    def args = []
    if (imageCache != "") {
      // todo(ajm) chaneg interface to array for multiple image cache options
      args.push("--cache-from-image='${imageCache}")

    }
    script.sh """
              docker build ${args.join(" ")} --tag ${imageTag} .
              """
  }

}

