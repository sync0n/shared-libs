#!/usr/bin/env groovy
import com.linn.Result

/**
 * The CD pipeline for Lambda functions.
 *
 * @param propFilePath Path to pipeline.properties file.
 */
def call(propFilePath) {
    def props = null // properies from propFilePath
    def slackTreadId = null // slack tread
    String s3Key = null // path to lambda artifact in S3
    Result approval = null // approval result

    pipeline {
        agent { label 'docker-build' }
        options{
            timeout(time: globalVars.deployPipelineTimeout, unit: 'MINUTES')
        }
        parameters{
            string(name: 'threadId')
            string(name: 'version')
            choice(name: 'masterPipeline', choices: ['false', 'true'])
        }
        stages{
            stage('Get App Properties') {
                steps{
                    script {
                        props = readProperties(file: propFilePath)
                        s3Key = "${props.rootDir}/${props.appName}-${params.version}.zip"

                        // check if threadId is set up
                        if (!params.threadId?.trim()) {
                            slackTreadId = slackSend(
                                channel: "#${globalVars.testSlackChannel}",
                                color: '#D4DADF',
                                message: "START DEPOY: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                            ).threadId
                        } else {
                            slackTreadId = params.threadId
                        }

                        log.info("Application Name: ${props.appName}")
                    }
                }
            }

            stage('Staging Deployment') {
                when {
                    expression { params.masterPipeline == 'false' }
                }
                steps{
                    dir (props.rootDir) {
                        script {
                            slackSend (
                                channel: slackTreadId,
                                color: '#8BE206',
                                message: "STAGING DEPLOYMENT IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                            )

                            lambdaDeploy(
                                props.lambdaFunctionName,
                                globalVars.preprodLambdaS3,
                                s3Key,
                                'eu-west-1',
                                null,
                                'deployer'
                            )
                        }
                    }
                }
            }

            stage('Approve for prod deployment') {
                when {
                    expression { params.masterPipeline == 'true' }
                }
                steps {
                    script {
                        slackSend (
                            channel: slackTreadId,
                            color: '#4286f4',
                            message: "APPROVAL: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.RUN_DISPLAY_URL})"
                        )

                        approval = getApproval("Deploy to Production", globalVars.approvalTimeout)
                    }
                }
            }

            stage('Prod Deployment') {
                when{
                    allOf {
                        expression { params.masterPipeline == 'true' }
                        expression { approval.status }
                    }
                }
                steps{
                    slackSend (
                        channel: slackTreadId,
                        color: '#8BE206',
                        message: "PRODUCTION DEPLOYMENT IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                    )
                    dir (props.rootDir) {
                        script{

                            lambdaDeploy(
                                props.lambdaFunctionName,
                                globalVars.prodLambdaS3,
                                s3Key,
                                'eu-west-1',
                                null,
                                null
                            )
                        }
                    }
                }
            }
        }
        post {
            success {
                slackSend (
                    channel: slackTreadId,
                    color: '#1BC131',
                    message: "SUCCESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                )
            }

            failure {
                slackSend (
                    channel: slackTreadId,
                    color: '#C11B1B',
                    message: "FAILURE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                )
            }

            aborted {
                slackSend (
                    channel: slackTreadId,
                    color: '#FF7B00',
                    message: "ABORTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                )
            }

            unstable {
                slackSend (
                    channel: slackTreadId,
                    color: '#FBFF01',
                    message: "UNSTABLE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                )
            }

            cleanup {
                cleanWs()
            }
        }
    }
}
