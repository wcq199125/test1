def start(msg="") {
    private picUrl = "http://icon-park.com/imagefiles/loading7_gray.gif"
    sendMessage(msg, "START",picUrl)
}

def succeed(msg="") {
    private picUrl = "http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/sign-check-icon.png"
    sendMessage(msg, "SUCCESS", picUrl)
}

def failed(msg="") {
    private picUrl = "http://www.iconsdb.com/icons/preview/soylent-red/x-mark-3-xxl.png"
    sendMessage(msg, "FAILED", picUrl)
}

def sendMessage(msginfo="", msgaction, msgpic) {
    def dingtalkAPI = "https://oapi.dingtalk.com/robot/send?access_token=7517330643eba343d9d619e349b011c99938e16057a55bfc99f15cd2baa624b7"

    def textTmpl  = "构建用户: %s | 构建环境: %s \n构建分支: %s | 持续时间: %s\n变更日志: %s"
    def bodyTmpl = """{
        "msgtype": "link", 
        "link": {
            "title": "%s", 
            "text": "%s",
            "messageUrl": "%s",
            "picUrl": "%s"
        }
    }""".trim()

    def duration  = currentBuild.duration.toInteger() / 1000 + "s"
    def changeLog = getChangeLog()
    def msgURL    = env.RUN_DISPLAY_URL
    def buildUser
    // trigger by system scan branch
    if (env.DEPLOY_ENV == null) { 
       buildUser = "system" 
    } else {
       buildUser = currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()   
    }

    //format body template
    def bodyTitle = String.format("[%s] %s%s",msgaction, env.JOB_NAME, env.BUILD_DISPLAY_NAME)
    def bodyText
    if (msginfo == "") {
        bodyText  = String.format(textTmpl, buildUser, env.DEPLOY_ENV, env.BRANCH_NAME, duration, changeLog)
    } else {
        bodyText = msginfo
    }
    def bodyJson  = String.format(bodyTmpl, bodyTitle, bodyText, msgURL, msgpic)

    httpRequest contentType: 'APPLICATION_JSON_UTF8', httpMode: 'POST', quiet: true, requestBody: bodyJson, url: dingtalkAPI
}

@NonCPS
def getChangeLog() {
    MAX_MSG_LEN = 200
    def changes = ""
    def changeLogSets = currentBuild.changeSets
    if (changeLogSets.empty) {
        return "No changes"
    }
    // get changeLog
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            truncated_msg = entry.msg.take(MAX_MSG_LEN)
            changes += "[${truncated_msg}]"
        }
    }

    return changes
}
