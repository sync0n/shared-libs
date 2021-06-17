#!/usr/bin/env groovy

/**
 * This functions uploads file to S3 bucket
 *
 * @param file Path to uploaded file.
 * @param bucket S3 bucket name
 * @param path Path in the bucket where file will be uploaded.
 * @param credentials Credentials for AWS if it is needed.
 *        It should store in Jenkins' credentials.
 */
def call(file, bucket, path, credentials = null) {
    try {
        if(credentials == null) {
            s3Upload(
                bucket: bucket,
                path: "${path}/",
                file: file,
                acl: "BucketOwnerFullControl"
            )
        } else {
            withAWS(credentials: credentials) {
                s3Upload(
                    bucket: bucket,
                    path: "${path}/",
                    file: file,
                    acl: "BucketOwnerFullControl"
                )
            }
        }
    } catch(Exception error) {
        log.error("Lambda upload error: ${error.toString()}")
    }
}
