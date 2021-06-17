import com.linn.Result
def call(type, appGroup, appName, versionFilePath, env){
	try {
		versionUpdated = false
		if (type == 'ms'){
			currentVersion = versionInfoCSproj(versionFilePath)
			versionInfo = helmLastVersion(appGroup, appName, env)
			if (env == 'prod'){
				if (currentVersion == versionInfo.lastVersion) {
					sh "dotnet version -s -f ${versionFilePath} patch"
					versionUpdated = true
				}
				newVersion = versionInfoCSproj(versionFilePath)
				while (versionInfo.versionList.contains(newVersion)) {
					println "version already exists."
					split = newVersion.split('\\.')
					split[2] = split[2].toInteger() + 1
					newVersion = split.join('.')
					println "newVersion = ${newVersion}"
					sh "dotnet version -s -f ${versionFilePath} ${newVersion}"
					versionUpdated = true
				}
				return versionUpdated
			}
		}
	} catch (Exception error) {
		log.error(error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}
