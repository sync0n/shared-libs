import com.linn.Result

def call(fromMaster) {
    try {
        if (fromMaster) {
            mergedBranch = powershell(returnStdout: true, script: """
                \$parts=(git log --pretty=format:"%s" --merges -n 1).trim('()').split(" ").replace("'", "")
                Write-Output \$parts[2]
            """
            ).trim()
            println "mergedBranchNameLower = ${mergedBranch}"
            return mergedBranch
        } else {
            branchNameLower = BRANCH_NAME.toLowerCase().replaceAll("[/ _  ]", "-")
            println "branchNameLower = ${branchNameLower}"
            return branchNameLower
        }
    } catch (Exception error) {
		log.error(error.toString())
		return new Result(status: false, buildResult: "FAILURE")
	}
}