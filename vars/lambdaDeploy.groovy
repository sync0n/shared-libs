#!/usr/bin/env groovy
//TODO(vchistiakov) Should we create a lambda, if it doesn exist?

/**
 * This function deploys lambda from S3.
 *
 * @parma functionName The name of the Lambda function.
 * @param s3Bucket An Amazon S3 bucket in the same AWS Region as your function.
 * The bucket can be in a different AWS account.
 * @param s3Key The Amazon S3 key of the deployment package.
 * @param region The Amazon region to use.
 * @param credetials The credential from Jenkins. It has higher priority
 * if it is defined.
 * @param profile Use a specific profile from your credential file.
 */
def call(String functionName, String s3Bucket, String s3Key, String region, String credetials = null, String profile = null) {
    try {
        if (credetials != null) {
            withAWS(credentials: credetials) {
                sh """
                aws lambda update-function-code \
                    --function-name ${functionName} \
                    --s3-bucket ${s3Bucket} \
                    --s3-key ${s3Key} \
                    --region ${region}
                """.trim()
            }
        } else if (profile != null) {
            sh """
            aws lambda update-function-code \
                --function-name ${functionName} \
                --s3-bucket ${s3Bucket} \
                --s3-key ${s3Key} \
                --region ${region} \
                --profile ${profile}
            """.trim()
        } else {
            sh """
            aws lambda update-function-code \
                --function-name ${functionName} \
                --s3-bucket ${s3Bucket} \
                --s3-key ${s3Key} \
                --region ${region}
            """.trim()
        }
    } catch (Exception error) {
        log.error("Lambda deploy error: ${error.toString()}")
    }

}
