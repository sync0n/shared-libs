

def call(propFilePath) {
    pipeline {
        agent { label 'docker-build-agent-01' }
        options {
            timeout(time: globalVars.buildPipelineTimeout, unit: 'MINUTES')
        }
        stages {
	 		stage('Preparing') {
	 			steps {
                    bitbucketStatusNotify(buildState: 'INPROGRESS')
                    script { 
                        props = readProperties  file: propFilePath
                        log.info("NPM Package Name: " + props.appName)
                    }
     				dir (props.repoPath) {
						script {	
							log.info ("GIT Branch:  ${env.GIT_BRANCH}")
                            CurrentVersion = versionInfoNpm()
                            println "CurrentVersion = ${CurrentVersion}"
                            if (GIT_BRANCH.contains(props.masterBranchName)) {
                                slackResponse = slackSend (channel: "#${globalVars.slackChannel}", color: '#D4DADF', message: "STARTED: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
                                slackSend (channel: slackResponse.threadId, color: '#8BE206', message: "BUILD IN PROGRESS: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n(${env.BUILD_URL})")
								fromMaster = true
                                versionUpdated = npmVersionUpdate(fromMaster,props.repoPath,CurrentVersion)
							}
                            else {
                                fromMaster = false
                                npmVersionUpdate(fromMaster,props.repoPath,CurrentVersion)
                            }
						}
	 				}
	 			}
	 		}
            
            stage('Install dependencies'){
		    	steps{
		    	    dir(props.repoPath){
		    			npmDependencies()
		    		}
		    	}
		    }

            stage('Unit Tests'){
                when{
                    expression{ props.unitTests == 'true' || props.unitTests == 'True' }
                }
		    	steps{
		    	    dir(props.repoPath){
		    			npmTests()
		    		}
		    	}
		    }

            stage('SonarQube Analysis') {
		    	steps{
                    dir (props.repoPath) {
                        script {
		    				npmSonarQube(props.appName)
                        }
                    }
                }
		    }

            stage('Build and Publish package'){
		    	steps{
		    		dir(props.repoPath){
		    			npmBuildPublish()
		    		}
		    	}
		    }

            stage('Finishing') {
                steps {
                    dir (props.repoPath) {
                        script {
                            if (fromMaster){
                                versionCommitMaster('npm', props.appName, 'dummyPath', 'prod', props.masterBranchName, versionUpdated)
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