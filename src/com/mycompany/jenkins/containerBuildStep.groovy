package com.mycompany.jenkins

class containerBuildStep implements Serializable {

  private final def script

    containerBuildStep(def script) {
    this.script = script
  }

  boolean scan(String CONTAINER_TAG) {
    script.sh """
              cat <<EOF > .trivyignore
              CVE-2018-20843
EOF

#                -v ~/.cache:/root/.cache/ \

              docker run --rm \
                -v "\${PWD}/.trivyignore:/.trivyignore" \
                -v /var/run/docker.sock:/var/run/docker.sock \
                aquasec/trivy \
                --ignore-unfixed \
                --exit-code 0 \
                --severity HIGH,CRITICAL \
                ${CONTAINER_TAG}
                """
  }

}

