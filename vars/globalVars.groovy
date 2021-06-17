class globalVars {
    final Integer buildPipelineTimeout = 30
    final Integer deployPipelineTimeout = 60
    final Integer approvalTimeout = 30
    final String prodAWSAccNum = "850447181109"
    final String preprodAWSAccNum = "131581481424"
    final Integer sonarWaitSeconds = 10
    final Integer sonarTimeOutMinutes = 10
    final String REPO = "bitbucket.org/linn_systems/linn_micro_services.git"
    final String NUGET_REPO = "bitbucket.org/linn_systems/linn_nuget_packages.git"
    final String NPM_REPO = 'bitbucket.org/linn_systems/linn_npm_packages.git'
    final String slackChannel = "system-update-cicd"
    final String testSlackChannel = "devops-test-channel"
    final String preprodKubeContextEU = "jenkins-sre-linnworkspreprod-eu"
    final String prodKubeContextEU = "jenkins-sre-kubernetes-eu"
    final String preprodKubeContextUS = "jenkins-sre-linnworkspreprod-eu"
    final String prodKubeContextUS = "jenkins-sre-linnworks-us"
    final String sonarScanner = "/opt/sonar-scanner/sonar-scanner-4.3.0.2102-linux/bin/sonar-scanner"
    final String prodLambdaS3 = "linn-lambda-repo"
    final String preprodLambdaS3 = "linnpre-lambda-repo"

}
