import com.linn.Result
def call(appName, sonarProject, rootDir, versionFilePath, dockerFilePath){
    try {
        newVersion = versionInfoCSproj(versionFilePath)
        appNameLower = appName.toLowerCase()
        envVariables = [ 'DOTNET_CLI_TELEMETRY_OPTOUT=1' ]
        withSonarQubeEnv('sonarqube-port') {
            docker.build("${appNameLower}-sonarqube-image", "--target sonarqube -f ${dockerFilePath} .")
            docker.image("${appNameLower}-sonarqube-image:latest").inside("-u 0:0") {
                withEnv(envVariables) {
                    sh """                                                                        \
                    dotnet /sonar-scanner/SonarScanner.MSBuild.dll begin /k:"ms-${sonarProject}" /v:"${newVersion}" /d:sonar.cs.opencover.reportsPaths="/work/linn_micro_services/${rootDir}/TestResults/coverage.opencover.xml" \
                            /d:sonar.cs.vstest.reportsPaths="/work/linn_micro_services/${rootDir}/TestResults/*.trx" \
                            /d:sonar.scm.disabled=true /d:sonar.coverage.dtdVerification=true 
                    """
                    sh "dotnet build /work/linn_micro_services/${rootDir} -c Release"
                    sh "dotnet /sonar-scanner/SonarScanner.MSBuild.dll end"
                }
            }
        }
    } catch(Exception error){
		log.error("Docker Sonar failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
    }

    Integer maxRetry = (globalVars.sonarTimeOutMinutes * 60) / globalVars.sonarWaitSeconds as Integer
    for (Integer i = 0; i < maxRetry; i++) {
        try {
            timeout(time: globalVars.sonarWaitSeconds, unit: 'SECONDS') {
                def qualityGate = waitForQualityGate()
                if (qualityGate.status != 'OK') {
                    log.error("Pipeline aborted due to quality gate failure: ${qualityGate.status}")
                } else {
                    i = maxRetry
                }
            }
        } catch (Throwable error) {
            if (i == maxRetry - 1) {
                log.error("Docker Sonar failure. " + error.toString())
                return new Result(status: false, buildResult: "FAILURE")
            }
        }
    }
}