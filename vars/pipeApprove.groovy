def call(msg){
    def userInput
    def choiceList = ["Agrees", "Deny"]
    
    def approveUser = "chenweijia,lizhipeng,huangdabin,wangjianqiang,zhengyongtao"
    if (env.GLOBAL_APPROVE_USER != null){
        approveUser = approveUser + ',' + env.GLOBAL_APPROVE_USER
    }
    timeout(time: 1, unit: 'DAYS') {
        userInput = input message: '是否允许上线', submitter: approveUser, ok: "确认",
                          parameters: [choice(choices: ['Agree', 'Deny'], description: '', name: 'approve')]
    }
    if (userInput != "Agree") {
        def errmsg = "Approve refused"
        pipeDingTalkNotify.failed(errmsg)
        currentBuild.result = 'ABORTED'
        error(errmsg)
    }
}