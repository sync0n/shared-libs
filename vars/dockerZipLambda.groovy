#!/usr/bin/env groovy

/**
 * Thi function archives build Lambda in zip archive.
 * @param appName Application name. Also it is used in archive name.
 * @param versionFile Path to csproj file with version.
 * @param dockerFile Path to Dockerfile.
 *
 * @return Archive file name
 */
def String call(String appName, String versionFile, String dockerFile = "Dockerfile") {
    try {
        def version = versionInfoCSproj(versionFile)
        def appNameLower = appName.toLowerCase()
        def currentDir = sh(script: "pwd", returnStdout: true).trim()
        def customImage = docker.build("${appNameLower}-zip-image", "--target zip -f ${dockerFile} .")
        // we have to use root user due to permissions are used into workspace.
        customImage.inside("-u 0:0") {
            sh "zip --junk-paths ${currentDir}/${appName}-${version}.zip /app/*"
        }
        return "${appName}-${version}.zip"
    } catch(Exception error) {
        log.error("Docker Zip failure: ${error.toString()}")
    }
}
