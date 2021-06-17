import com.linn.Result

def call() {
    try {
        sh label: '', script: '''
	        npm install
	    '''
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}