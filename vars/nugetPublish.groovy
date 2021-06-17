import com.linn.Result

def call(fromMaster) {
    try {
        if (fromMaster) {
        	bat "sleet push bin\\Release\\ --source feed --config C:\\sleet.json"
        }
		else {
            bat "sleet push bin\\Debug\\ --source feed --config C:\\sleet.json"
        }
    } catch (Exception error) {
		log.error(error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}