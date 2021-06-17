import com.linn.Result

def call(){
    try {
        //appNameLower = appName.replaceAll("\\.", "").toLowerCase()
        //sh "for dir in `find . \\( -name \"*.Tests\" -o -name \"*.tests\" -o -name \"*.IntegrationTests\" -type d \\) -print`; do cd \${dir}; dotnet add package coverlet.msbuild --no-restore; cd - ; done"
        bat "for /D %%i in (*.tests, *.integrationtests) do dotnet add %%i package coverlet.msbuild"
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}