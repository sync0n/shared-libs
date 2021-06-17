import com.linn.Result
def call(appName, dockerFile){
	try{
        // TODO (vchistyakov) I saw it into getEnv.groovy. Maybe, we should push it to AppProps class.
		appNameLower = appName.replaceAll("\\.", "").toLowerCase()
		sh "for dir in `find . \\( -name \"*.Tests\" -o -name \"*.tests\" -o -name \"*.IntegrationTests\" -type d \\) -print`; do cd \${dir}; dotnet add package coverlet.msbuild --no-restore; cd - ; done"
		sh "find . \\( -name *.csproj -o -name *.sln -o -name NuGet.Config -o -name Directory.Build.targets \\) -print0 | tar -cvf ${appNameLower}.tar --null -T -"
		docker.build(appNameLower + "-build-image", "--target build -f ${dockerFile} .")
	} catch(Exception error){
		log.error("Docker dotnet build failure. " + error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}
