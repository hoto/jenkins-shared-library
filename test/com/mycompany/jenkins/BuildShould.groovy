package com.mycompany.jenkins

import mocks.WorkflowScriptStub
import spock.lang.Specification

class BuildShould extends Specification {

    private Build build
    private def script

    private static final String MESSAGE = 'MESSAGE'
    private static final String DESCRIPTION = 'DESCRIPTION'

    void setup() {
        script = Spy(WorkflowScriptStub)
        build = new Build(script)
    }

    def 'set build description'() {
        when:
        build.setBuildDescription(
            message: MESSAGE,
            description: DESCRIPTION,
        )

        then:
        script.currentBuild.displayName == 'MESSAGE'
        script.currentBuild.description == 'DESCRIPTION'
    }
}
