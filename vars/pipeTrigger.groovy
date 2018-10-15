def call(name){
    def jenkinsfilename = "file/Jenkinsfile"
    def jenkinsfile = libraryResource(jenkinsfilename)
    def resource = libraryResource("config/business/${name}.yaml")
    def config = readYaml text: resource

    if (config.pipeline.enabled) {
        pipePodTemplate(config.app.build_label){
            // global ENV with APP_NAME
            env.APP_NAME = name
            ws("workspace/${config.app.name}") {
                writeFile file: jenkinsfilename, text: jenkinsfile
                load(jenkinsfilename)
            }
        }
    } else {
        currentBuild.result = 'ABORTED'
        error "Project pipeline disabled!! skip..."
    }
}