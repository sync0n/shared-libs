import com.linn.Result

def call(fromMaster){
    try {
        if (fromMaster){
            bat "dotnet build -c Release"
        } else {
            bat "dotnet build -c Debug"
        }
    } catch (Exception error) {
        log.error(error.toString())
        return new Result(status: false, buildResult: "FAILURE")
    }
}

