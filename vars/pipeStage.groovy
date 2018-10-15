def call(name, Closure body) {
    try {
        gitlabCommitStatus(name) {
            stage(name) {
                body()
            }
        }
    } catch (Exception e) {
        currentBuild.result = "FAILURE"
        pipeDingTalkNotify.failed(e)
        error(e)
    }   
}