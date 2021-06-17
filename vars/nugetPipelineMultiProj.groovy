

def call(propFilePath) {
    pipeline {
        agent { label 'windows-build-agent-02' }
        options {
            timeout(time: globalVars.buildPipelineTimeout, unit: 'MINUTES')
        }
        stages {
	 		stage('Preparing') {
	 			steps {
                    bitbucketStatusNotify(buildState: 'INPROGRESS')
                    script {
                        props = readProperties  file: propFilePath
                        log.info("Nuget Package Name: " + props.appName)
                    }
     				dir ("${props.rootDir}/${props.projectDir}") {
						script {
							log.info ("GIT Branch:  ${env.GIT_BRANCH}")
                            CurrentVersion = versionInfoCSproj(props.projFile)
							if (GIT_BRANCH.contains(props.masterBranchName)) {
                                slackResponse = slackSend (channel: "#${globalVars.slackChannel}", color: '#D4DADF', message: "STARTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                                slackSend (channel: slackResponse.threadId, color: '#8BE206', message: "BUILD IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
								fromMaster = true
                                versionUpdated = nugetVersionUpdate(props.appName,fromMaster,props.projFile,CurrentVersion)
                                mergedBranch = getBranchName(fromMaster)
                                mergedBranchVersions = mergedBranch.toLowerCase().replaceAll("[/ _  ]", "-")
							}
                            else {
                                fromMaster = false
                                nugetVersionUpdate(props.appName,fromMaster,props.projFile,CurrentVersion)
                                dotnetRestore(props.projFile)
                            }
						}
	 				}
                    dir (props.rootDir) {
                        addCoverletNew()
                    }
	 			}
	 		}

            stage ('Unit Test') {
                steps {
                    dir (props.rootDir) {
                        dotnetTestsMultiProj(props.appName)
                    }
                }
            }

            stage ('Integration Test') {
                when{
                    expression{ props.integrationTests == 'true' || props.integrationTests == 'True' }
                }
                steps {
                    dir (props.rootDir) {
                        dotnetIntTestsMultiProj(props.appName)
                    }
                }
            }

            stage('SonarQube Analysis') {
				steps {
                    dir ("${props.rootDir}/${props.projectDir}") {
                        dotnetSonarQubeNew(props.appName, props.rootDir, props.projFile, fromMaster)
					}
				}
			}

            stage('Compile & Build Package') {
				steps {
                    dir (props.rootDir) {
                        dotnetBuildMultiProj(fromMaster)
					}
				}
			}

            stage('Deploy to Nuget Server') {
                steps {
                    dir ("${props.rootDir}/${props.projectDir}") {
                        script {
                            if (fromMaster) {
                                slackSend (channel: slackResponse.threadId, color: '#FF5733', message: "PRODUCTION DEPLOYMENT IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                                nugetRemovePreviousVersion(props.appName, CurrentVersion, mergedBranchVersions)
                                nugetPublish(fromMaster)
                            }
                            else {
                                branchNameLower = getBranchName(fromMaster)
                                nugetRemovePreviousVersion(props.appName, CurrentVersion, branchNameLower)
                                nugetPublish(fromMaster)
                            }
                        }
					}
				}
			}

            stage('Finishing') {
                steps {
                    dir ("${props.rootDir}/${props.projectDir}") {
                        script {
                            if (fromMaster){
                                versionCommitMaster('nuget', props.appName, props.projFile, 'prod', props.masterBranchName, versionUpdated)
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
                    if (fromMaster) {
                        slackSend (channel: slackResponse.threadId, color: '#1BC131', message: "SUCCESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                    }
                }
            }
            // triggered when red sign
            failure {
	    		bitbucketStatusNotify(buildState: 'FAILED')
                script {
                    if (fromMaster) {
                        slackSend (channel: slackResponse.threadId, color: '#C11B1B', message: "FAILURE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                    }
                }
            }
            // triggered when aborted
            aborted {
	    		bitbucketStatusNotify(buildState: 'FAILED')
                script {
                    if (fromMaster) {
                        slackSend (channel: slackResponse.threadId, color: '#FF7B00', message: "ABORTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                    }
                }
            }
            // triggered when unstable
            unstable {
	    		bitbucketStatusNotify(buildState: 'SUCCESSFUL')
                script {
                    if (fromMaster) {
                        slackSend (channel: slackResponse.threadId, color: '#FBFF01', message: "UNSTABLE: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                    }
                }
            }
            cleanup {
              cleanWs()
            }
        }
    }
}
