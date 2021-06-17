def info(message) {
    echo "INFO: ${message}"
}

def warning(message) {
    echo "WARNING: ${message}"
}

def error(message) {
    throw new Exception("ERROR: ${message}")
    currentBuild.result = 'FAILURE'
}
def abort(message) {
    echo "ERROR: ${message}"
    currentBuild.result = 'ABORTED'
}