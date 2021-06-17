import com.linn.Result

/**
 * @Deprecated. this functionality was moved close to test execution.
 *
 */
def call(appName, rootDir){
	try{
		appNameLower = appName.replaceAll("\\.", "").toLowerCase()
		docker.image("${appNameLower}-unittests-image:latest").inside() {
            sh "cp -r /work/linn_micro_services/${rootDir}/* ."
        }
        step([$class: 'MSTestPublisher', failOnError: false, testResultsFile: "**/*.trx"])
	} catch(Exception error){
		log.error("Docker publish tests failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}
