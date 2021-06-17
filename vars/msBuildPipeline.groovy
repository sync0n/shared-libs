def call(propFilePath) {
    pipeline {
        agent { label 'dockerpre-build' }
        options {
            timeout(time: globalVars.buildPipelineTimeout, unit: 'MINUTES')
        }
        stages {
		    stage('Compile') {
	 			steps {
                    bitbucketStatusNotify(buildState: 'INPROGRESS')
                    script {
                        slackResponse = slackSend (channel: "#${globalVars.slackChannel}", color: '#D4DADF', message: "STARTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                        slackSend (channel: slackResponse.threadId, color: '#8BE206', message: "BUILD IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                        props = readProperties  file: propFilePath
                        log.info("Application Name: " + props.appName)
                    }
     				dir (props.rootDir) {
						script {
							log.info ("GIT Branch:  ${env.GIT_BRANCH}")
							if (GIT_BRANCH.contains(props.masterBranchName)) {
								fromMaster = true
								versionUpdated = versionUpdateMaster('ms', props.appGroup, props.appName, props.versionFilePath, 'prod')
							}
                            else {
                                fromMaster = false
                            }
                            dockerDotnetBuild(props.appName, props.dockerFilePath)
						}
	 				}
	 			}
	 		}

            stage ('Unit Test') {
                steps {
                    dir (props.rootDir) {
                        dockerDotnetTests(props.appName, props.dockerFilePath, props.rootDir)
                    }
                }
            }
            stage ('Integration Test') {
                when{
                    expression{ props.integrationTests == 'true' || props.integrationTests == 'True' }
                }
                steps {
                    dir (props.rootDir) {
                        dockerDotnetIntTests(props.appName, props.dockerFilePath, props.rootDir,)
                    }
                }
            }
			stage('SonarQube Analysis') {
				steps {
                    dir (props.rootDir) {
                        dockerSonarQube(props.appName, props.sonarProject, props.rootDir, props.versionFilePath, props.dockerFilePath)
					}
				}
			}
            stage('Build & Publish Docker Image') {
				steps {
                    dir (props.rootDir) {
                        script {
                            if (fromMaster){
                                dockerImage(props.appGroup, props.appName, props.versionFilePath, props.dockerFilePath, 'prod')
                            } else {
                                dockerImage(props.appGroup, props.appName, props.versionFilePath, props.dockerFilePath, 'dev')
                            }
                        }
					}
				}
			}
            stage('Publish Helm Charts') {
                steps {
                    dir (props.rootDir) {
                        script {
                            if (fromMaster){
                                helmUpload(props.appGroup, props.appName, props.versionFilePath, 'prod')
                            } else {
                                helmUpload(props.appGroup, props.appName, props.versionFilePath, 'dev')
                            }
                        }
                    }
                }
            }
            stage('Finishing') {
                steps {
                    dir (props.rootDir) {
                        script {
                            if (fromMaster){
                                versionCommitMaster('ms', props.appName, props.versionFilePath, 'prod', props.masterBranchName, versionUpdated)
                            }
                            dockerTestsPublish(props.appName, props.rootDir)
                            slackSend (channel: slackResponse.threadId, color: '#06CEE2', message: "BUILD COMPLETED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                        }
                    }
                }
            }
            stage('Start Deployment') {
                steps {
                    dir (props.rootDir) {
                        script {
                            if (fromMaster){
                                triggerCDJob(props.versionFilePath, props.jenkinCdJobName, slackResponse.threadId, 'prod')
                            } else {
                                if (props.devDeployment == 'true'){
                                    triggerCDJob(props.versionFilePath, props.jenkinCdJobName, slackResponse.threadId, 'pre-prod')
                                } else {
                                    log.info("Dev deployment disabled.")
                                }
                            }
                        }
                    }
                }
            }
        }
        post {
            // only triggered when blue or green sign
            success {
                bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                script {
                    slackSend (channel: slackResponse.threadId, color: '#1BC131', message: "SUCCESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            // triggered when red sign
            failure {
                bitbucketStatusNotify(buildState: 'FAILED')
                script {
                    slackSend (channel: slackResponse.threadId, color: '#C11B1B', message: "FAILURE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            // triggered when aborted
            aborted {
                bitbucketStatusNotify(buildState: 'FAILED')
                script {
                    slackSend (channel: slackResponse.threadId, color: '#FF7B00', message: "ABORTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            // triggered when unstable
            unstable {
                bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                script {
                    slackSend (channel: slackResponse.threadId, color: '#FBFF01', message: "UNSTABLE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            cleanup {
                cleanWs()
            }
        }
    }
}
