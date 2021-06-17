def call(propFilePath) {
    pipeline {
         agent { label 'docker-build' }
         options {
             timeout(time: globalVars.deployPipelineTimeout, unit: 'MINUTES')
         }
        stages {
	 		stage('Get App Properties') {
	 			steps {
                    script { 
                        props = readProperties  file: propFilePath
                        log.info("Application Name: " + props.appName)
                    }	
	 			}
	 		}
        
            stage('Dev Deployment') {
                when{
                    expression{ props.devDeployment == 'true' || props.devDeployment == 'True' }
                }
	 			steps {
     				dir (props.rootDir) {
						script {
                            slackSend (channel: threadId, color: '#8BE206', message: "DEV DEPLOYMENT IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                            helmDeploy(props.appGroup, props.appName, env.version, 'dev', 'eu', env.masterPipeline)
                            urlHealthCheck(props.devHealthCheckUrl)
						}
	 				}	
	 			}
	 		}
			stage('Staging Deployment') {
                when{
                    expression{env.masterPipeline == 'true'}
                    }
	 			steps {
     				dir (props.rootDir) {
						script {
                            slackSend (channel: threadId, color: '#8BE206', message: "STAGING DEPLOYMENT IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")	
                            helmDeployWithNewStg(props.appGroup, props.appName, env.version, 'staging', 'eu', env.masterPipeline)
                            urlHealthCheck(props.stagingHealthCheckUrl)
						}
	 				}	
	 			}
	 		}
            stage('Approve for prod deployment'){
                when{
                    expression{env.masterPipeline == 'true'}
                }
                steps{
                    slackSend (channel: threadId, color: '#4286f4', message: "APPROVAL: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.RUN_DISPLAY_URL})")
                    script {
                        myApproval = getApproval("Deploy to Production", globalVars.approvalTimeout)
                    }
                }
            }
            
            stage('Prod Deployment') {
                when{
                    allOf {
                        expression{env.masterPipeline == 'true'}
                        expression{myApproval.status}
                    }
                }
	 			steps {
     				dir (props.rootDir) {
						script {
                            slackSend (channel: threadId, color: '#FF5733', message: "PRODUCTION DEPLOYMENT IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")	
                            helmDeploy(props.appGroup, props.appName, env.version, 'prod', 'eu', env.masterPipeline)
                            urlHealthCheck(props.prodHealthCheckUrlEU)
                            if (props.regionsProd.contains('us')){
                                helmDeploy(props.appGroup, props.appName, env.version, 'prod', 'us', env.masterPipeline)
                                urlHealthCheck(props.prodHealthCheckUrlUS)
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
                    slackSend (channel: env.threadId, color: '#1BC131', message: "SUCCESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            failure {
                bitbucketStatusNotify(buildState: 'FAILED')
                script {
                    slackSend (channel: env.threadId, color: '#C11B1B', message: "FAILURE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            aborted {
                bitbucketStatusNotify(buildState: 'FAILED')
                script {
                    slackSend (channel: env.threadId, color: '#FF7B00', message: "ABORTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            unstable {
                bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                script {
                    slackSend (channel: env.threadId, color: '#FBFF01', message: "UNSTABLE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                }
            }
            cleanup {
                cleanWs()
            }
        }
    }
}