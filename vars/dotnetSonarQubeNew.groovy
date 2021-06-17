import com.linn.Result
def call(appName, rootDir, projFile, fromMaster){
    try {
        newVersion = versionInfoCSproj(projFile)
        appNameLower = appName.toLowerCase()
        envVariables = [ 'DOTNET_CLI_TELEMETRY_OPTOUT=1' ]
        withSonarQubeEnv('sonarqube-port') {
            bat """                                                                        \
            dotnet sonarscanner begin /k:"nuget-${appName}" \
                    /d:sonar.cs.opencover.reportsPaths="../TestResults/coverage.opencover.xml" \
                    /v:="${newVersion}" \
                    /d:sonar.cs.vstest.reportsPaths="../TestResults/*.trx" \
                    /d:sonar.scm.disabled=true \
                    /d:sonar.coverage.dtdVerification=true \
                    /d:sonar.coverage.exclusions="*tests*.cs" \
                    /d:sonar.test.exclusions="*tests*.cs"
            """
            bat "dotnet build ../ -c Release"
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