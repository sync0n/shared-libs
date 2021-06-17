import com.linn.Result
def call (appGroup, appName, versionFilePath, dockerFile, env){
	try{
		branchNameLower = BRANCH_NAME.toLowerCase().replaceAll("[/ _  ]", "-")
		newVersion = versionInfoCSproj(versionFilePath)
		appNameLower = appName.toLowerCase()
		imageEnv = getEnv(appGroup, appName, env, 'eu', 'false')
		if (env == 'dev'){
			newVersion = "${newVersion}-"+"${branchNameLower}."+"${currentBuild.number}"
		}
		docker.build("${imageEnv.ecruri}/${imageEnv.application}:${newVersion}", "--target final -f ${dockerFile} .")
		sh("eval \$(aws ecr get-login --no-include-email --registry-ids ${globalVars.preprodAWSAccNum} --region eu-west-1| sed 's|https://||')")
		docker.withRegistry("https://${imageEnv.ecruri}", "${imageEnv.ecrcred}"){
			docker.image("${imageEnv.ecruri}/${imageEnv.application}:${newVersion}").push()
        }
		sh("for IMAGES in \$(aws ecr list-images --repository-name ${imageEnv.application}  --filter tagStatus=TAGGED | jq -r '.imageIds[].imageTag'|grep ${branchNameLower} |grep -v ${newVersion}); do aws ecr batch-delete-image --registry-id ${globalVars.preprodAWSAccNum} --repository-name ${imageEnv.application} --image-ids imageTag=\$IMAGES; done")
	} catch(Exception error){
		log.error("Docker create image failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}