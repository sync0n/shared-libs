import com.linn.Result

def call(){
    try {
        bat "dotnet add package coverlet.msbuild"
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}