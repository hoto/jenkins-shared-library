package com.mycompany.jenkins

class Utilities implements Serializable {

  private final def script

    Utilities(def script) {
    this.script = script
  }

  static def mvn(script, args) {
    script.sh "${script.tool 'Maven'}/bin/mvn -s ${script.env.HOME}/jenkins.xml -o ${args}"
  }

  int make(script, args) {
    // todo ???
    script.sh "make ${args}"
  }

  boolean scan(String CONTAINER_TAG) {
    // todo ???
    ContainerBuiltStepHadolint
  }

}

