import com.linn.Result

def call(appGroup, appName, version, envName, clusterRegion, fromMaster){
	appNameLower = appName.replaceAll("\\.", "").toLowerCase()
	appEnv = getEnv(appGroup, appName, envName, clusterRegion, fromMaster)
	dir('IaC'){
		script {
			sh("/home/jenkins/helm/linux-amd64/helm repo update")
			sh("helm fetch ${appEnv.s3RepoPath}/${appEnv.application}-${version}.tgz")
			sh("tar -xvzf ${appEnv.application}-${version}.tgz")
			
			//deploy
			helmUpgrade = sh (
			returnStatus:true,
			script:  "                                                              \
				/home/jenkins/helm/linux-amd64/helm upgrade ${appEnv.application} ${appEnv.helmRepo}/${appEnv.application} --version ${version} \
														--values charts/values-${envName}.yaml \
														--namespace ${envName} \
														--kube-context linnworks-stg \
			"
			)
			log.info(helmUpgrade)
			//change the kube context to respective EKS cluster
			sh("kubectl config use-context linnworks-stg")

			// Verify deployment
			sh("kubectl rollout status deployment ${appNameLower}-deployment -n ${envName} | tail -1 > deploymentStatus.txt")

			timeout(time: 30, unit: 'SECONDS') {
				waitUntil {
					try {
						def deploymentStatus = readFile('deploymentStatus.txt').trim()
						if (deploymentStatus == "deployment \"${appNameLower}-deployment\" successfully rolled out") {
							log.info("Deployment status is: ${deploymentStatus}")  
							return true
						} else {
							log.error("Deployment verification failure.")
							return false
						}
					} catch(Exception error){
						log.error("Deployment failure. " + error.toString())
						return false
					} finally {
						sh "rm -rf deploymentStatus.txt"
					}
				}
			}
			//change the kube context to new EKS cluster
			sh ("kubectl config use-context jenkins-sre-kubernetes-eu")
		}
	}
}
