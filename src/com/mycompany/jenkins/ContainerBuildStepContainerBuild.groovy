package com.mycompany.jenkins

class ContainerBuildStepContainerBuild implements Serializable {

  private final def script

  ContainerBuildStepContainerBuild(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  boolean build(String imageTag, def config) {
    def args = []
    if (config instanceof java.util.Map && config.imageCache) {
      // todo(ajm) change interface to array for multiple image caches
      args.push("--cache-from-image='${imageCache}")
    }

    if (config instanceof java.util.Map) {
      print("we are IN LIST")
    }
    if (config instanceof java.util.Map) {
      print("we are in Collection")
    }

    if (config instanceof java.util.Map && config.cmd) {
      script.sh """
      ${config.cmd}
      """
    } else {
      script.sh """
              set -Eeux

              # TODO(ajm) internal: use kaniko with /proc unmasking
              # podman's buildah likely used in hamlet
              # jib, micronaut possible incremental/root-and-branch solutions
              # - graalvm

              docker build ${args.join(" ")} --tag ${imageTag} .

              # grafeas push
              """
    }
  }
}
