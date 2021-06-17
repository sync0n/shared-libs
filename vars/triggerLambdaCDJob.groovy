#!/usr/bin/env groovy
import com.linn.Result

/**
 * This function trigger remote Lambda CD pipeline
 * //TODO(vchistyakov) Need to discuss about how to migrate it to triggerCDJob.groovy
 *
 * @param version The path to csproj file with version.
 * @param jenkinsCdJobName The Jenkins Cd pipeline job.
 * @param slackThreadId The link to a Slack's tread for updating status.
 * @param environment The our environment choise parameter.
 */
def call(String version, String jenkinsCdJobName, String slackThreadId, String environment) {
	try{
        if ( environment == 'pre-prod' || environment == 'preprod') {
            triggerRemoteJob(
                remoteJenkinsName: 'jenkins-cd',
                blockBuildUntilComplete: false,
                job: "${jenkinsCdJobName}",
                maxConn: 1,
                useCrumbCache: true,
                useJobInfoCache: true,
                parameters: """
threadId=${slackThreadId}
version=${version}
masterPipeline=false
                """.trim()
            )
        } else {
            triggerRemoteJob(
                remoteJenkinsName: 'jenkins-cd',
                blockBuildUntilComplete: false,
                job: "${jenkinsCdJobName}",
                maxConn: 1,
                useCrumbCache: true,
                useJobInfoCache: true,
                parameters: """
threadId=${slackThreadId}
version=${version}
masterPipeline=true
                """.trim()
            )
        }
    } catch(Exception error) {
		log.error("CD job trigger failure: ${error.toString()}")
		return new Result(status: false, buildResult: "FAILURE")
	}
}
