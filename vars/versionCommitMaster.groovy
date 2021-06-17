import com.linn.Result

// TODO(vchistyakov) add props as parameter
def call(type, appName, versionFilePath, environment, masterBranchName, versionUpdated){
    try {
        // get version
        String newVersion
        switch(type.toString()) {
            case "npm":
                newVersion = versionInfoNpm()
                break
            default:
                newVersion = versionInfoCSproj(versionFilePath)
        }

        // git commit if version was switched.
        // git tag
        switch(type.toString()) {
            case "ms":
                if (environment == 'prod') {
                    withCredentials([usernamePassword(
                        credentialsId: 'b9ca3ca9-4bde-4691-90ed-b2443267aeff',
                        usernameVariable: 'GIT_USERNAME',
                        passwordVariable: 'GIT_PASSWORD'
                    )]) {
                        if (versionUpdated) {
                            sh "git add ${versionFilePath}"
                            sh "git commit -m ${newVersion}"
                            sh "git tag ${appName}-${newVersion}"
                            sh "git pull --no-edit https://\${GIT_USERNAME}:\${GIT_PASSWORD}@${globalVars.REPO} master"
                            sh "git push --tags https://\${GIT_USERNAME}:\${GIT_PASSWORD}@${globalVars.REPO} HEAD:${masterBranchName}"
                        }
                    }
                }
                break

            case "nuget":
                withCredentials([usernamePassword(
                    credentialsId: 'a103179f-3da7-4143-aaad-35c7b80d4612',
                    usernameVariable: 'GIT_USERNAME',
                    passwordVariable: 'GIT_PASSWORD'
                )]) {
                    if (versionUpdated) {
                        bat "git pull --no-edit https://%GIT_USERNAME%:%GIT_PASSWORD%@${globalVars.NUGET_REPO} ${masterBranchName}"
                        bat "git commit -m bump ${versionFilePath}"
                        bat "git tag ${appName}-${newVersion}"
                        bat "git push https://%GIT_USERNAME%:%GIT_PASSWORD%@${globalVars.NUGET_REPO} HEAD:${masterBranchName} ${appName}-${newVersion}"
                    } else if (!versionUpdated) {
                        bat "git tag ${appName}-${newVersion}"
                        bat "git push https://%GIT_USERNAME%:%GIT_PASSWORD%@${globalVars.NUGET_REPO} HEAD:${masterBranchName} ${appName}-${newVersion}"
                    }
                }
                break

            case "lambda":
                withCredentials([usernamePassword(
                    credentialsId: 'b9ca3ca9-4bde-4691-90ed-b2443267aeff',
                    usernameVariable: 'GIT_USERNAME',
                    passwordVariable: 'GIT_PASSWORD'
                )]) {
                    // Since a password can have special characters,
                    // we should encode it.
                    String encodedPassword=URLEncoder.encode(GIT_PASSWORD, "UTF-8")
                    // here is a better way to use git url from environment
                    // instead of using an own constant variable.
                    String gitUrl = env.GIT_URL.replace("https://", "https://${GIT_USERNAME}:${encodedPassword}@")

                    //TODO(vchistyakov) fix hardcode
                    sh "echo ${newVersion} > IaC/lastVersion.txt"
                    sh "git add ${versionFilePath} IaC/lastVersion.txt"
                    sh "git commit -m ${newVersion}"
                    sh "git tag ${appName}-${newVersion}"
                    sh "git push ${gitUrl} HEAD:${env.GIT_BRANCH} ${appName}-${newVersion}"
                }
                break
        }
    } catch (Exception error) {
        log.error("Version commit failure " + error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}
