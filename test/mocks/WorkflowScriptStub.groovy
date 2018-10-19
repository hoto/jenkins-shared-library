package mocks

class WorkflowScriptStub {

    def env = [
        BRANCH_NAME : 'BRANCH_NAME',
        BUILD_URL   : 'BUILD_URL',
        BUILD_NUMBER: 'BUILD_NUMBER',
    ]

    def currentBuild = [
        currentResult: 'DEFAULT_CURRENT_RESULT'
    ]

    String sh(Map args) {
        'DEFAULT_SH_RETURN_VALUE'
    }

}
