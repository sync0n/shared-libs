#!/usr/bin/env groovy

/**
 * This function provides SonarQube analysis for Lambdas Docker
 *
 * @param appName Application name. it is better to avoid "." and uppercase characters
 * @param sonarProject SonarQube project name
 * @versionFilePath Path to csproj file with version.
 * @dockerFilePath Path to Dockerfile
 */
def call(appName, sonarProject, versionFilePath, String dockerFilePath = "Dockerfile") {
    try {
        //TODO (vchistyakov) it will be better to send a version to avoid external calls
        def version = versionInfoCSproj(versionFilePath)
        def envVariables = ['DOTNET_CLI_TELEMETRY_OPTOUT=1']

        // https://github.com/nosinovacao/dotnet-sonar
        withSonarQubeEnv('sonarqube-port') {
            def customImage = docker.build("${appName}-sonarqube-image", "--target sonarqube -f ${dockerFilePath} .")
            customImage.inside("-u 0:0") {
                withEnv(envVariables) {
                    sh """dotnet /sonar-scanner/SonarScanner.MSBuild.dll begin \
                    /k:"${sonarProject}" \
                    /v:="${version}" \
                    /d:sonar.cs.opencover.reportsPaths="/work/TestResults/opencover.xml" \
                    /d:sonar.cs.vstest.reportsPaths=/work/TesResults/test_results.xml \
                    /d:sonar.scm.disabled=true \
                    /d:sonar.coverage.dtdVerification=true \
                    /d:sonar.coverage.exclusions="*Tests*.cs" \
                    /d:sonar.test.exclusions="*Tests*.cs"
                    """
                    sh "dotnet build -c Release"
                    sh "dotnet /sonar-scanner/SonarScanner.MSBuild.dll end"
                }
            }
        }
        // it waits for sonar's quality gate status and sets the build outcome
        timeout(time: globalVars.sonarTimeOutMinutes, unit: 'MINUTES') {
            def qualityGate = waitForQualityGate()
            if (qualityGate.status != 'OK') {
                log.error("Pipeline aborted due to quality gate failure: ${qualityGate.status}")
            }
        }
    } catch(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException error) {
        // timeout exception
        log.error("Sonar timeout")
    } catch(Exception error) {
        log.error("Docker Sonar failure: ${error.toString()}")
    }
}
