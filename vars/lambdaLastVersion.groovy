#!/usr/bin/env groovy
import com.linn.versionInfo

/**
 * This function gets last version for Lambda
 *
 * @param file The path to a file with version.
 *
 * @return com.linn.versionInfo
 */
def call (file) {
    try {
        lastVersion = sh(script: "cat ${file}", returnStdout: true).trim()

        return new versionInfo(lastVersion: lastVersion, versionList: [lastVersion])
    }
    catch(Exception error) {
        log.error("Getting last version failure. ${error.toString()}")
    }
}
