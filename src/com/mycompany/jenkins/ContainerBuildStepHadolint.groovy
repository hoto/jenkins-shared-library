package com.mycompany.jenkins

class ContainerBuildStepHadolint implements Serializable {

  private final def script

  ContainerBuildStepHadolint(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  // TODO(ajm) prevent string injection
  boolean scan(def config) {
    def dockerfiles = ""
    if (config instanceof java.util.Map && config.dockerfile) {
      dockerfiles = config.dockerfile
    }
    script.sh """
              set -Eeux

              FILES="${dockerfiles}"
              FILES="\${FILES:-\$(find . -name 'Dockerfile*')}"

              IFS="\t\n"
              for FILE in "\${FILES}"; do
                echo "\${FILE}";
                IS_IN_AUTOMATION="" /entrypoint.sh "\${FILE}" | grep --color=always '.*' || true
              done
              """
  }

}
