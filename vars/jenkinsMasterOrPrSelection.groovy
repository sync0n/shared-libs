#!/usr/bin/env groovy

/**
 * The function runs multibranch job on a branch.
 * Now, we support only master and PR branch.
 *
 * @param String project Multibranch Pipeline project name.
 * @param String branch Name of the actual branch for which the project will be executing.
 * @param String prBranch Name of branch with PR.
 */
def call(String project, String branch = env.CHANGE_BRANCH, String prBranch = env.GIT_BRANCH) {
    // A bit info about default values:
    // When PR is created:
    //   env.CHANGE_BRANCH = your working branch with changes
    //   env.CHANGE_TARGET = a dst branch in PR, as usual it is master =)
    //   env.BRANCH_NAME = PR-*, the same as env.GIT_BRANCH
    //   env.GIT_BRANCH = PR-*, your PR ref
    //
    // When PR is merged:
    //   env.CHANGE_BRANCH = null, isn't defined
    //   env.CHANGE_TARGET = null, isn't defined
    //   env.BRANCH_NAME = master
    //   env.GIT_BRANCH = master
    if (prBranch.contains('master')) {
        jenkinsScanMultiBranch(project, 'master')
        build(job: "${project}/master", wait: false)
    } else if (prBranch.startsWith('PR')) {
        String encodedBranch = URLEncoder.encode(branch, "utf-8")
        jenkinsScanMultiBranch(project, encodedBranch)
        build(job: "${project}/${encodedBranch}", wait: false)
    } else {
        log.warning("Unknown branch found: ${prBranch}!\nPlease note that this pipeline is working only for PR requests or merge to master.")
    }
}
