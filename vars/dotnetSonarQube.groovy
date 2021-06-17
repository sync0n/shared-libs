import com.linn.Result
def call(appName, projectDir, projectTestDir, projFile, fromMaster){
    try {
        newVersion = versionInfoCSproj(projFile)
        appNameLower = appName.toLowerCase()
        envVariables = [ 'DOTNET_CLI_TELEMETRY_OPTOUT=1' ]
        withSonarQubeEnv('sonarqube-port') {
            bat """                                                                        \
            dotnet sonarscanner begin /k:"nuget-${appName}" /d:sonar.cs.opencover.reportsPaths="../${projectTestDir}/TestResults/opencover.xml" \
                    /v:="${newVersion}" \
                    /d:sonar.cs.vstest.reportsPaths="../${projectDir}/TestResults/test_results.xml" \
                    /d:sonar.scm.disabled=true /d:sonar.coverage.dtdVerification=true \
                    /d:sonar.coverage.exclusions="*Tests*.cs" \
                    /d:sonar.test.exclusions="*Tests*.cs"
            """
            bat "dotnet build ${projFile}"
            bat "dotnet sonarscanner end"
            if (fromMaster){
                bat "dotnet clean --configuration Release"
            } else {
				bat "dotnet clean --configuration Debug"
            }   
        }
    } catch(Exception error){
		log.error("Sonar failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
    }

    Integer waitSeconds = 10
	Integer timeOutMinutes = 10
	Integer maxRetry = (timeOutMinutes * 60) / waitSeconds as Integer
	for (Integer i = 0; i < maxRetry; i++) {
		try {
			timeout(time: waitSeconds, unit: 'SECONDS') {
			def qualityGate = waitForQualityGate()
			if (qualityGate.status != 'OK') {
				error "Pipeline aborted due to quality gate failure: ${qualityGate.status}"
				sonaranalysisFail = true
			} else {
				i = maxRetry
			}
			}
		} catch (Throwable e) {
		if (i == maxRetry - 1) {
			throw e
		}
		}
	}
}