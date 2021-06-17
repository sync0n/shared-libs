import com.linn.Result
def call (appGroup, appName, versionFilePath, env){
	try{
		appNameLower = appName.replaceAll("\\.", "").toLowerCase()
		newVersion = versionInfoCSproj(versionFilePath)
		branchNameLower = BRANCH_NAME.toLowerCase().replaceAll("[/ _  ]", "-")
		imageEnv = getEnv(appGroup, appNameLower, env, 'eu', 'false')
		if ( env == 'dev'){
			newVersion = "${newVersion}-"+"${branchNameLower}."+"${currentBuild.number}"
		}
		dir('IaC'){
			sh("sed -i 's/to_be_replaced_by_ci/${imageEnv.ecruri}\\/${imageEnv.application}:${newVersion}/g' charts/values-prod.yaml")
			sh("sed -i 's/to_be_replaced_by_ci/${imageEnv.ecruri}\\/${imageEnv.application}:${newVersion}/g' charts/values-staging.yaml")
			sh("sed -i 's/to_be_replaced_by_ci/${imageEnv.ecruri}\\/${imageEnv.application}:${newVersion}/g' charts/values-dev.yaml")
			sh("sed -i 's/appversion/${newVersion}/g' charts/Chart.yaml")
			sh("sed -i 's/chartversion/${newVersion}/g' charts/Chart.yaml")
			sh("tar -cvzf ${imageEnv.application}-${newVersion}.tgz charts/")
			sh("""helm s3 push --acl="bucket-owner-full-control" ./${imageEnv.application}-${newVersion}.tgz ${imageEnv.helmRepo}""")
		}
	}
	catch (Exception error) {
		log.error("Helm upload failure " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}