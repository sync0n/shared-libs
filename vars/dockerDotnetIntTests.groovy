import com.linn.Result

/**
 * This function makes integration tests via Docker.
 * Since we don't have a standart parent folder for TestResults, we try looking for in few paths.
 *
 * @param appName Application name. Usually this variable is from pipeline.properties.
 * @param dockerFile Path to Dockerfile.
 * @param projectDir Project path.
 */
def call(String appName, String dockerFile, String projectDir = '') {
    //TODO(vchistiakov) we can merge it with dockerDotnetTests function.
    // just add an adiitional parameter like target Docker stage.
    try {
        def currentDir = sh(script: "pwd", returnStdout: true).trim()

        appNameLower = appName.replaceAll("\\.", "").toLowerCase()
        def customImage = docker.build(appNameLower + "-inttests-image", "--target integration-tests -f ${dockerFile} .")

        customImage.inside("-u 0:0") {
            sh """
            if [ -d /work/linn_micro_services/${projectDir}/TestResults ]; then
                cp -vr "/work/linn_micro_services/${projectDir}/TestResults" "${currentDir}"
                chown -R 1001:1001 "${currentDir}/TestResults"
            fi
            """
        }
    } catch(Exception error) {
        log.error("Docker dotnet integration tests failure. " + error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    } finally {
        mstest(testResultsFile: "TestResults/int_test_results*", failOnError: false)

        // mstest set currentBuild.currentResult to UNSTABLE, if there are any failure in tests.
        // DEVOPS-1457 we have an agreement to stop pipeline, if test failures were happen.
        if(currentBuild.currentResult == 'UNSTABLE') {
            log.error("Unit tests have failures")
        }
    }
}
