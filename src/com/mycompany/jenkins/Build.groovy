package com.mycompany.jenkins

class Build implements Serializable {

    private final def script

    Build(def script) {
        this.script = script
    }

    void setBuildDescription(Map args) {
        script.currentBuild.displayName = args.title
        script.currentBuild.description = args.description
    }

}

