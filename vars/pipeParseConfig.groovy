def call(gitinfo) {
    def resource = libraryResource("config/business/${APP_NAME}.yaml")
    def config   = readYaml text: resource
    gitinfo.GIT_BRANCH = gitinfo.GIT_BRANCH.replace("master", "latest").replace("origin/", "").replace("release/", "release-")

    config << gitinfo
    config << ['dockerfile': pipeDockerfile("${config.app.template}")]
    return config
}