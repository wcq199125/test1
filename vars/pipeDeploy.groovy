def helm(config){
    def deployEnv = env.DEPLOY_ENV
    def kubeContextName = "${deployEnv}@context"
    def configName = "${config.app.name}.yaml"

    // deploy args
    def deployArgs = """\
    --set app.build_id=${env.BUILD_ID} \
    --set app.runEnv.ENV=${deployEnv} \
    --set app.runEnv.GIT_COMMIT=${config.GIT_COMMIT} \
    --set image.tag=${config.GIT_BRANCH} \
    """.trim()

    if (deployEnv == "prod"){
        deployArgs += """ \
        --set replicaCount=${config.replicaCount} \
        --set ingress.hosts[0]=${config.app.url_prod} \
        """
    } else if (deployEnv == "uat") {
        deployArgs += """ \
        --set replicas=1 \
        --set ingress.hosts[0]=${config.app.url_uat} \
        """
    }
    container("k8s") {
        def deployCmd
        def deployName = config.app.name
        def deployStatus

        ws("resource") {
            echo "Clone helm charts from gitlab..."
            git credentialsId: '73bdcb04-d0c4-4699-b0b1-3925a084e8c7', changelog: false,
                url: 'https://code.simpletour.com/tools/jenkins-cicd.git', branch: 'master'

            deployCmd = "helm upgrade --install --kube-context=$kubeContextName --namespace ${config.app.namespace} -f $configName $deployArgs $deployName . "

            dir('resources/charts/simpletour') {
                if (fileExists(configName)) {
                    sh "rm -rf $configName"
                }
                // write release config
                writeYaml file: configName, data: config

                echo "Start deploy project..."
                echo "command => $deployCmd"
                sh "$deployCmd"

                echo "Wating for deploy status"
                
                sleep(120)

                def readyCmd = """ kubectl get deployment --context=$kubeContextName --namespace ${config.app.namespace} $deployName -o json | jq '.status.conditions[] | select(.reason == "MinimumReplicasAvailable") | .status' | tr -d '"' |tr -d '\n' """
                
                // waiting ready statushao
                for (i = 1 ;i <= 5; i++) {
                    echo "retry [$i]"
                    deployStatus = pipeShell.nodebug(readyCmd)

                    if (deployStatus == "True") { break }
                    sleep(2)

                }
                if (deployStatus == "True") {
                    currentBuild.result = "SUCCESS"
                    echo "[SUCCESS]"
                    pipeDingTalkNotify.succeed()
                } else {                    
                    // rollback
                    sh "helm status $deployName --kube-context=${kubeContextName}"
                    echo "[FAILED]"
                    pipeDingTalkNotify.fail()
                    // rollback to preversion
                    echo "Start rollback..."
                    def rollversion = pipeShell.noDebug("helm history $deployName --max 1|tail -1|awk '{print \$1}'").toInteger() - 1
                    def rollCmd = "helm rollback ${deployName} ${rollversion} --wait --timeout 300 --recreate-pods"
                    
                    echo "Rollback to preversion [${rollversion}]"
                    sh "${rollCmd}"

                }

            }
        }
    }
}
