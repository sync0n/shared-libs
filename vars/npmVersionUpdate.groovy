import com.linn.Result

def call (fromMaster,repoPath,currentVersion){
    try {
        if (fromMaster) {
            versionUpdated = false
            def versionExist = sh (returnStdout: true, script: """npm view ${repoPath} versions""")
            versionExist = Eval.me(versionExist)
            lastVersion = versionExist.last().trim()
            println "lastVersion = ${lastVersion}"
            if ("${currentVersion}" == "${lastVersion}") {
				sh (returnStdout: true, script: '''npm version patch''')
				versionUpdated = true
			}
            newVersion = versionInfoNpm()
            while ( versionExist.contains(newVersion) ) {
				echo "version already exists."
				split = newVersion.split('\\.')
				split[2] = split[2].toInteger() + 1
				newVersion = split.join('.')
				sh "npm version ${newVersion}"
				versionUpdated = true
                println "newVersion = ${newVersion}"
			}
            return versionUpdated
        }
        else {
            branchNameLower = getBranchName(fromMaster)
            newVersion = "${currentVersion}-"+"${branchNameLower}."+"${env.BUILD_NUMBER}"
			sh (returnStdout: true, script: """npm version ${newVersion}""")
            println "newVersion = ${newVersion}"
        }
    } catch(Exception error){
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}