import com.linn.Result

def call(projFile){
    try {
        bat "dotnet restore ${projFile} --source https://api.nuget.org/v3/index.json --source https://linn-nuget-server.s3.amazonaws.com/index.json"
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}