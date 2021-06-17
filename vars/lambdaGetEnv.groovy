import com.linn.LambdaProp
import com.linn.Result

def call() {
    try {

    }
    catch(Exception error) {
        log.error("Gettng properties failure. ${error.toString()}")
        return new Result(status: false, buildResult: "FAILURE")
    }
}
