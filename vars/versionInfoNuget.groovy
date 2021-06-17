import com.linn.versionInfo

def call (appName){
	try{
		versionExist = powershell(returnStdout: true, script: """
					Invoke-WebRequest -Uri https://linn-nuget-server.s3.amazonaws.com/sleet.packageindex.json -OutFile sleet.packageindex.json
					\$json = (Get-Content "sleet.packageindex.json" -Raw) | ConvertFrom-Json
					\$versionExist = \$json.packages."${appName}"
					Write-Output \$versionExist | foreach { echo "\$_" }
                    """
        ).trim()
        return versionExist		
	} catch (Exception error) {
		log.error(error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}