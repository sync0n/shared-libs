import com.linn.Result

def call(message, delay = 30){
	try {
		timeout(time:"${delay}", unit:'MINUTES') {
			def APPROVE_PROD = input message: "${message}", ok: 'Continue',
			parameters: [choice(name: 'APPROVE_PROD', choices: 'YES\nNO', description: 'Deploy from STAGING to PRODUCTION?')]
			if (APPROVE_PROD == 'YES'){
				return new Result(status: true, buildResult: "SUCCESS")
			} else {
				log.abort('Not Approved.')
				return new Result(status: false, buildResult: "ABORTED")
			}
		}
	} catch (error) {
		log.abort('Timeout has been reached! Approval cancelled.')
		return new Result(status: false, buildResult: "ABORTED")
	}
}