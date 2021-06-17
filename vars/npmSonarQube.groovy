import com.linn.Result

def call (appName) {
    try {
        newVersion = versionInfoNpm()
        println "newVersion = ${newVersion}"
        withSonarQubeEnv('sonarqube-port') {
            sh """                                                        
                ${globalVars.sonarScanner} -Dsonar.projectKey=npm-${appName} -Dsonar.sources=src -Dsonar.projectVersion=${newVersion}
            """
        }
        timeout(time: 5, unit: 'MINUTES'){
            def qualityGate = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
            if (qualityGate.status != 'OK') {
                sonaranalysisFail = true
            }
        }
    } catch (Exception error) {
		log.error("Sonar failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}