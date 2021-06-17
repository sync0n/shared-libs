import com.linn.Result

/**
 * This function execute dotnet test.
 * to run test for all internal projects --filter option is used.
 *
 * @param appName ProjectName
 **/
//TODO(vchistyakov): looks like we don't use appName paramenter any more. Can we delete it?
def call (appName) {
    def unitTests = null
    try {
        //bat "dotnet add ${appName}.tests package coverlet.msbuild"
        unitTests = bat (
            script: """dotnet test --filter "FullyQualifiedName~.tests" -c Release --verbosity=normal  --results-directory TestResults/ --logger "trx;LogFilePrefix=test_results" /p:CollectCoverage=true  /p:MergeWith="../TestResults/coverage.json" /p:CoverletOutputFormat=\\"json,opencover\\" /p:CoverletOutput=../TestResults/ -m:1""",
            returnStatus: true
        )
        bat "dotnet build-server shutdown"
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    } finally {
        mstest(testResultsFile: "TestResults/test_results*", failOnError: false)

        // DEVOPS-1457 we have an agreement to stop pipeline, if test failures were happen.
        if (unitTests != 0) {
            log.error('ERROR: unit tests failure')
        }
    }
}
