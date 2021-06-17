import com.linn.Result

def call(appName,CurrentVersion,branchNameLower) {
    try {
	        lasttestVersions = powershell(returnStdout: true, script: """
	    		Invoke-WebRequest -Uri https://linn-nuget-server.s3.amazonaws.com/sleet.packageindex.json -OutFile sleet.packageindex.json
	    		\$json = (Get-Content "sleet.packageindex.json" -Raw) | ConvertFrom-Json
	    		\$lasttestVersions = \$json.packages."${appName}" | Select-String -Pattern "${CurrentVersion}-${branchNameLower}*"
	    		Write-Output \$lasttestVersions | foreach { echo "\$_" }
	    	"""
	    	).trim()
    
            if (lasttestVersions) {
                List versions = lasttestVersions.split( '\n' )
                versions.each {
                    println "Deleted versions=${it}"
                    bat "sleet delete -i ${appName} -v ${it} --source feed --config C:\\sleet.json"
                }
            } else {
                echo "Unable to find any previous versions!"
            }
    } catch(Exception error){
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}