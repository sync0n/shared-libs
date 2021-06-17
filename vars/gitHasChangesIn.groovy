#!/usr/bin/env groovy

/**
 * The functions checks changes were into path or not
 * It compares target and source branches and serch differences. If target branch is missed(null),
 * it compares two last commits in sourceBranch with each other.
 * Since it calls native git diff, a list with changed files is provided to ouput.
 *
 * @param path Path to check.
 * @param targetBranch Branch where changes will be merged.
 * @param sourceBranch Branch whith changes that will be merged to the target branch.
 *
 * @return It returns with 'true', if there are differents and 'false' means no differences.
 */
def boolean call(String path, targetBranch = env.CHANGE_TARGET, sourceBranch = env.BRANCH_NAME) {
    // env.CHANGE_TARGET is null, when we run a job on master/main branch.
    if (targetBranch == null) {
        log.info("No target branch was defined. So, checking changes on branch master")
        return sh(
            script: "git diff --exit-code --name-only HEAD~1 -- ${path}",
            returnStatus: true
        ) == 1
    }

    // Get refernces
    // CHECK(vchistyakov) should we use special "upstream" ref?
    // we set up 2 special refs:
    // +refs/pull-requests/*/merge:refs/remotes/origin/PR-*
    // +refs/heads/master:refs/remotes/upstream/master
    // and recieve + 1 standart ref
    // +refs/heads/master:refs/remotes/origin/master
    String targetBranchRef = sh(
        script: "git rev-parse upstream/${targetBranch}",
        returnStdout: true
    ).trim()
    String sourceBranchRef = sh(
        script: "git rev-parse origin/${sourceBranch}",
        returnStdout: true
    ).trim()

    log.info "Checking for source changes between ${targetBranchRef} (${targetBranch}) and ${sourceBranchRef} (${sourceBranch})..."
    return sh(
        script: "git diff --exit-code --name-only ${targetBranchRef}...${sourceBranchRef} -- ${path}",
        returnStatus: true
    ) == 1
}
