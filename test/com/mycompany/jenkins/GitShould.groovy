package com.mycompany.jenkins

import mocks.WorkflowScriptStub
import spock.lang.Specification

class GitShould extends Specification {

    private Git git
    private def script

    private static final String COMMIT_MESSAGE =
        "This commit message is very long. This commit message is very long. This commit message is very long. This commit message is very long. This commit message is very long. Now it's more then 180 characters."
    private static final String COMMIT_AUTHOR =
        "Commit Author Name Commit Author Name Commit Author Name Commit Author Name Commit Author Name"
    private static final String COMMIT_HASH = "5e68e94b46dcde93def3f93f0da1933000294dc4"

    void setup() {
        script = Spy(WorkflowScriptStub)
        git = new Git(script)
    }

    def 'trim commit message'() {
        given:
        script.sh([script: 'git log --format=%B -n 1 HEAD | head -n 1', returnStdout: true]) >> COMMIT_MESSAGE

        expect:
        git.commitMessage() == COMMIT_MESSAGE.substring(0, 180).trim()
    }

    def 'trim commit author'() {
        given:
        script.sh([script: 'git log --format=\'%an\' -n 1 HEAD', returnStdout: true]) >> COMMIT_AUTHOR

        expect:
        git.commitAuthor() == COMMIT_AUTHOR.substring(0, 80).trim()
    }

    def 'trim commit hash'() {
        given:
        script.sh([script: 'git rev-parse HEAD', returnStdout: true]) >> COMMIT_HASH

        expect:
        git.commitHash() == COMMIT_HASH.substring(0, 7).trim()
    }

    def 'leave branch name as is if does not contain a slash'() {
        given:
        script.env >> [BRANCH_NAME: 'TESLA-123-some-feature']

        expect:
        git.shortBranchName() == "TESLA-123-some-feature"
    }

    def 'shorten branch name if it contains a slash'() {
        given:
        script.env >> [BRANCH_NAME: 'feature/TESLA-123-some-feature']

        expect:
        git.shortBranchName() == "TESLA-123-some-feature"
    }

    def 'return true when branch is master'() {
        given:
        script.env >> [BRANCH_NAME: 'master']

        expect:
        git.isMasterBranch()
    }

}
