import com.linn.Result

/**
 * This function executes dotnet unit tests and publishes result to Jenkins.
 *
 * @param projectTestDir Folder Name with tests.
 * @param projTestFile CsProj File name.
 */
def call (String projectTestDir, String projTestFile) {
    def unitTests = null

    try {
        unitTests = bat (
            script: "dotnet test --verbosity=normal --results-directory TestResults/ --logger trx;LogFileName=test_results.xml /p:CollectCoverage=true /p:CoverletOutputFormat=opencover /p:CoverletOutput=TestResults/opencover.xml ../${projectTestDir}/${projTestFile}",
            returnStatus: true
        )
        bat "dotnet build-server shutdown"
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    } finally {
        mstest(testResultsFile: "TestResults/test_results.xml", failOnError: false)

        // DEVOPS-1457 we have an agreement to stop pipeline, if test failures were happen.
        if (unitTests != 0) {
            log.error('ERROR: Unit tests failure')
        }
    }
}
