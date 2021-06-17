#!/usr/bin/env groovy
// TODO (vchistyakov) should think how to merge it to versionUpdateMaster with other params.
import com.linn.Result

/**
 * This function updates version in csproj file for labda
 *
 * @param versionFilePath Path to the file with the current version.
 * @param lastVersionFilePath Path to the file with the last version.
 *
 * @return boolean
 */
def call(String versionFilePath, String lastVersionFilePath){
    try {
        boolean versionUpdated = false

        String currentVersion = versionInfoCSproj(versionFilePath)
        def lastVersion = lambdaLastVersion(lastVersionFilePath)

        if (currentVersion == lastVersion.lastVersion) {
            sh "dotnet version -s -f ${versionFilePath} patch"
            versionUpdated = true
        }

        return versionUpdated
    } catch (Exception error) {
        log.error(error.toString())
    }
}
