import com.linn.Result
def call(healthcheckEndpoint) {
    timeout(time: 300, unit: 'SECONDS') {
        waitUntil {
            try {
                sleep 5
                healthcheckStatus = sh( script: "curl -s --head  --request GET  ${healthcheckEndpoint}", returnStdout: true).trim()
                if (healthcheckStatus.contains('200')) {
                    return true
                }
                else {
                    return false
                }
            } catch(Exception error){
                return false
            }
        }    
    }
}