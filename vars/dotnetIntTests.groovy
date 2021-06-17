import com.linn.Result

/**
 * This function executes intergation tests and publishes results to Jenkins
 *
 * @param appName
 */
def call (appName) {
    def intTests = null

    try {
        bat "dotnet add ${appName}.integrationtests package coverlet.msbuild"
        intTests = bat (
            script: """dotnet test --filter "FullyQualifiedName~.integrationtests" -c Release --verbosity=normal  --results-directory TestResults/ --logger "trx;LogFileName=int_test_results.xml" /p:CollectCoverage=true  /p:MergeWith="../TestResults/coverage.json" /p:CoverletOutputFormat=\\"json,opencover\\" /p:CoverletOutput=../TestResults/ -m:1""",
            returnStatus: true
        )
        bat "dotnet build-server shutdown"
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    } finally {
        mstest(testResultsFile: "TestResults/int_test_results.xml")

        // DEVOPS-1457 we have an agreement to stop pipeline, if test failures were happen.
        if (intTests != 0) {
            log.error('ERROR: Integration tests failure')
        }
    }
}
