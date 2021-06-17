import com.linn.Result

def call(projFile, fromMaster){
    try {
        if (fromMaster){
            bat "dotnet build ${projFile} -c Release"
        } else {
            bat "dotnet build ${projFile} -c Debug"
        }
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}

