import com.linn.versionInfo
import com.linn.Result

def call (appGroup, appName, env){
	try{
		appNameLower = appName.replaceAll("\\.", "").toLowerCase()
		imageEnv = getEnv(appGroup, appNameLower, env, 'eu', 'false')
		allVersion = sh( script: "helm search -l ${imageEnv.helmRepo} |awk '{print \$3}' |grep -v VERSION |sort -V", returnStdout: true).trim()
		if (allVersion.indexOf("\n") == -1){
		  	lastVersion = "0"
		} else {
			lastVersion = allVersion.substring(allVersion.lastIndexOf("\n")).trim()
		}
		log.info("Application ${appNameLower} last ${env} version is ${lastVersion} with helm repo ${imageEnv.helmRepo} on aws account ${imageEnv.awsAccNum}")
		return new versionInfo (lastVersion: lastVersion.trim(), versionList: allVersion.split("\n"))
	}
	catch (Exception error) {
		log.error(error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}
