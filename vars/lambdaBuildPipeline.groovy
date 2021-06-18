#!/usr/bin/env groovy

/**
 * The CI pipeline for Lambda functions.
 *
 * @param propFilePath Path to pipeline.properties file.
 */
def call(String propFilePath) {
    def props = null // properies from propFilePath
    def slackResponse = null // Slack
    def appNameLower = null
    boolean fromMaster = false
    boolean versionUpdated = false
    String zipName = null

    pipeline {
        agent { dockerfile true }
        options {
            timeout(time: globalVars.buildPipelineTimeout, unit: 'MINUTES')
        }
        stages {
            stage('Compile') {
                steps {
                    script {
                        props = readProperties(file: propFilePath)
                        log.info("Application Name: ${props.appName}")
                        log.info("GIT Branch: ${env.GIT_BRANCH}")
                    }
                    dir(props.rootDir) {
                        script{
                            // check if it is main branch
                            if(env.GIT_BRANCH.contains(props.masterBranchName)) {
                                fromMaster = true
                                versionUpdated = lambdaVersionUpdate(props.versionFilePath, props.lastVersionFilePath)
                            }
                            appNameLower = props.appName.replaceAll("\\.", "").toLowerCase()
                            dockerDotnetBuild(appNameLower, props.dockerFilePath)
                        }
                    }
                }
            }

            stage('Unit Test') {
                steps {
                    dir(props.rootDir) {
                        dockerDotnetTests(appNameLower, props.dockerFilePath)
                    }
                }
            }

            stage('SonarQube Analysis') {
                steps {
                    dir(props.rootDir) {
                        dockerSonarQubeLambda(appNameLower, props.sonarProject, props.versionFilePath)
                    }
                }
            }

            stage('Make Package') {
                steps {
                    dir(props.rootDir){
                        script{
                            zipName = dockerZipLambda(props.appName, props.versionFilePath)
                        }
                    }
                }
            }

            stage('Publish Package') {
                steps {
                    dir(props.rootDir){
                        script {
                            if(fromMaster) {
                                lambdaUpload(zipName, globalVars.prodLambdaS3, props.rootDir)
                            } else {
                                lambdaUpload(zipName, globalVars.preprodLambdaS3, props.rootDir, "linnpre-jenkins")
                            }
                        }
                    }
                }
            }

            stage('Finishing') {
                environment {
                    GIT_TRACE = "true"
                    GIT_CURL_VERBOSE = "true"
                }
                steps {
                    dir(props.rootDir) {
                        script {
                            slackSend (
                                channel: slackResponse.threadId,
                                color: '#06CEE2',
                                message: "BUILD COMPLETED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                            )
                            if(fromMaster) {
                                versionCommitMaster(
                                    'lambda',
                                    props.appName,
                                    props.versionFilePath,
                                    null,
                                    null,
                                    null
                                )
                            }
                        }
                    }
                }
            }

            stage('Start Deployment') {
                steps{
                    dir(props.rootDir) {
                        script {
                            if (fromMaster) {
                                triggerLambdaCDJob(
                                    versionInfoCSproj(props.versionFilePath),
                                    props.jenkinsCdJobName,
                                    slackResponse.threadId,
                                    'prod'
                                )
                            } else {
                                triggerLambdaCDJob(
                                    versionInfoCSproj(props.versionFilePath),
                                    props.jenkinsCdJobName,
                                    slackResponse.threadId,
                                    'preprod'
                                )
                            }
                        }
                    }
                }
            }
        }
        post {
            success {
                bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                script {
                    slackSend (
                        channel: slackResponse.threadId,
                        color: '#1BC131',
                        message: "SUCCESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                    )
                }

            }

            failure {
                bitbucketStatusNotify(buildState: 'FAILED')
                script{
                    slackSend (
                        channel: slackResponse.threadId,
                        color: '#C11B1B',
                        message: "FAILURE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                    )
                }
            }

            aborted {
                bitbucketStatusNotify(buildState: 'FAILED')
                script {
                    slackSend (
                        channel: slackResponse.threadId,
                        color: '#FF7B00',
                        message: "ABORTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                    )
                }
            }

            unstable {
                bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                script {
                    slackSend (
                        channel: slackResponse.threadId,
                        color: '#FBFF01',
                        message: "UNSTABLE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})"
                    )
                }
            }

            cleanup {
                cleanWs()
            }
        }
    }
}
