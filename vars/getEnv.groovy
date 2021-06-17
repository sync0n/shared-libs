import com.linn.AppProp
import com.linn.Result

def call(appGroup, appName, env, clusterRegion, fromMaster){
	try{
		appNameLower = appName.replaceAll("\\.", "").toLowerCase()
		appGroupLower = appGroup.toLowerCase()
		if(env == 'dev'){
			if ( fromMaster == 'true' ) {
				_helmRepo = "linn-${appNameLower}"
				_s3RepoPath = "s3://linn-helm-repo/charts/${appGroupLower}/${appNameLower}"
			} else {
				_helmRepo = "linnpre-${appNameLower}"
				_s3RepoPath = "s3://linnpre-helm-repo/charts/${appGroupLower}/${appNameLower}"
			}
			_awsAccNum = globalVars.preprodAWSAccNum
			_kubeContext=globalVars.preprodKubeContextEU
		} else if ( env == 'staging' || env == 'prod') {
			_helmRepo = "linn-${appNameLower}"
			_awsAccNum = globalVars.prodAWSAccNum
			_s3RepoPath = "s3://linn-helm-repo/charts/${appGroupLower}/${appNameLower}"
			if(clusterRegion == 'eu'){
				_kubeContext=globalVars.prodKubeContextEU
			} else if (clusterRegion == 'us') {
				_kubeContext=globalVars.prodKubeContextUS
			}
		} else {
			log.error("Please enter valid account name i.e. prod/pre-prod.")
		}
		_application = sh( script: "grep '^application' IaC/${env}/env-variables.properties | cut -d '=' -f 2-", returnStdout: true).trim()
		_ecruri = sh( script: "grep '^ecruri' IaC/${env}/env-variables.properties | cut -d '=' -f 2-", returnStdout: true).trim()
		_ecrcred = sh( script: "grep '^ecrcred' IaC/${env}/env-variables.properties | cut -d '=' -f 2-", returnStdout: true).trim()
		return new AppProp(helmRepo: _helmRepo, awsAccNum: _awsAccNum, s3RepoPath: _s3RepoPath, application: _application, ecruri: _ecruri, ecrcred: _ecrcred, kubeContext: _kubeContext)
	} catch(Exception error){
		log.error("Getting properties failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}