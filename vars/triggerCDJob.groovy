import com.linn.Result
def call(versionFilePath, jenkinCdJobName, slackThreadId, env){
	try{
        newVersion = versionInfoCSproj(versionFilePath)

        if ( env == 'pre-prod' || env == 'preprod'){
            branchNameLower = BRANCH_NAME.toLowerCase().replaceAll("[/ _  ]", "-")
            newVersion = "${newVersion}-"+"${branchNameLower}."+"${BUILD_NUMBER}"
            triggerRemoteJob blockBuildUntilComplete: false, job: "${jenkinCdJobName}", maxConn: 1, parameters: """threadId=${slackThreadId}
version=${newVersion} 
masterPipeline=false""", remoteJenkinsName: 'jenkins-cd', useCrumbCache: true, useJobInfoCache: true
        }else {
            triggerRemoteJob blockBuildUntilComplete: false, job: "${jenkinCdJobName}", maxConn: 1, parameters: """threadId=${slackThreadId}
version=${newVersion} 
masterPipeline=true""", remoteJenkinsName: 'jenkins-cd', useCrumbCache: true, useJobInfoCache: true
        }
    } catch(Exception error){
		log.error("CD job trigger failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}