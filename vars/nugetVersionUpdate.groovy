import com.linn.Result

def call (appName,fromMaster,projFile,currentVersion){
    try {
        if (fromMaster) {
            versionUpdated = false
            versionExist = versionInfoNuget(appName)
            if (versionExist.indexOf("\n") == -1){
			  lastVersion = "0"
              versionExist = "NULL"
			} else {
				lastVersion = versionExist.substring(versionExist.indexOf("\n")).trim() 
			}
            println "lastVersion = ${lastVersion}"
            if ("${currentVersion}" == "${lastVersion}") {			
		    	bat "dotnet version -s -f .\\${projFile} patch"
		    	versionUpdated = true
		    }
            newVersion = powershell(returnStdout: true, script: """
                \$xml = [Xml] (Get-Content .\\${projFile})
                \$version = \$xml.Project.PropertyGroup.Version
                Write-Output \$version 
            """
            ).trim()
            while ( versionExist.contains("${newVersion}") ) {
				echo "version already exists."
				split = newVersion.split('\\.')
				split[2] = split[2].toInteger() + 1
				newVersion = split.join('.')
				echo "newVersion = ${newVersion}"
				bat "dotnet version -s -f .\\${projFile} ${newVersion}"
                versionUpdated = true
			}
            return versionUpdated
        }
        else {
            branchNameLower = getBranchName(fromMaster)
            testVersion = "${currentVersion}-"+"${branchNameLower}."+"${BUILD_NUMBER}"
            bat "setversion ${testVersion} .\\${projFile}"
            // read the new version of the .csproj file after the patch version bump or version change during prerelease
            newVersion = powershell(returnStdout: true, script: """
                \$xml = [Xml] (Get-Content .\\${projFile})
                \$version = \$xml.Project.PropertyGroup.Version
                Write-Output \$version 
            """
            ).trim()
            println "newVersion = ${newVersion}"
        }
    } catch(Exception error){
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}