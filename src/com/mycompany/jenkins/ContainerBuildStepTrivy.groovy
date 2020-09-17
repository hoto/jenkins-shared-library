package com.mycompany.jenkins

class ContainerBuildStepTrivy implements Serializable {

  private final def script

  ContainerBuildStepTrivy(def jenkinsfileScriptContext) {
    this.script = jenkinsfileScriptContext
  }

  boolean scan(def config, String imageTag) {
    script.sh """
              set -Eeux

              cat <<EOF > .trivyignore
              CVE-2018-20843
EOF

              docker run --rm \
                -v "\${PWD}/.trivyignore:/.trivyignore" \
                -v /var/run/docker.sock:/var/run/docker.sock \
                aquasec/trivy \
                --ignore-unfixed \
                --exit-code 0 \
                --severity HIGH,CRITICAL \
                ${imageTag}
                """
  }

}

