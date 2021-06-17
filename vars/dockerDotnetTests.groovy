import com.linn.Result

/**
 * This function makes tests phase via Docker.
 * Since we don't have a standart parent folder for TestResults, we try looking for in few paths.
 *
 * @param appName Application name. Usually this variable is from pipeline.properties.
 * @param dockerFile Path to Dockerfile.
 * @param projectDir Project path.
 */
def call(String appName, String dockerFile, String projectDir = '') {
	try {
        def currentDir = sh(script: "pwd", returnStdout: true).trim()

        appNameLower = appName.replaceAll("\\.", "").toLowerCase()
		def customImage = docker.build(appNameLower + "-unittests-image", "--target unit-tests -f ${dockerFile} .")

        customImage.inside("-u 0:0") {
            sh """
            # lambda
            if [ -d /work/TestResults ]; then
                cp -vr /work/TestResults "${currentDir}"
                chown -R 1001:1001 "${currentDir}/TestResults"
            # msbuild
            elif [ -d /work/linn_micro_services/${projectDir}/TestResults ]; then
                cp -vr "/work/linn_micro_services/${projectDir}/TestResults" "${currentDir}"
                chown -R 1001:1001 "${currentDir}/TestResults"
            fi
            """
        }
	} catch(Exception error) {
		log.error("Docker dotnet unit tests failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	} finally {
        mstest(testResultsFile: "TestResults/test_results*", failOnError: false)

        // mstest set currentBuild.currentResult to UNSTABLE, if there are any failure in tests.
        // DEVOPS-1457 we have an agreement to stop pipeline, if test failures were happen.
        if(currentBuild.currentResult == 'UNSTABLE') {
            log.error("Unit tests have failures")
        }
    }
}
