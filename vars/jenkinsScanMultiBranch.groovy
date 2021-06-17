#!/usr/bin/env groovy

/**
 * This function provides triggering branch indexing for a certain multibranch project.
 * It is a fork from https://stackoverflow.com/a/50982658
 *
 * @param String project The multibranch project name.
 * @param String branch The branch for checking.
 * @param boolean wait The flag to make this procedure synchronized.
 */
def call(String project, String branch, boolean wait = true){
    String job = "${project}/${branch}"
    // the `build` step does not support waiting for branch indexing (ComputedFolder job type),
    // so we need some black magic to poll and wait until the expected job appears
    build(job: project, wait: false)

    if(wait) {
        log.info("Waiting for job '${job}' to appear...")
        // the branch could be disabled, if it had existed before and got deleted. Probably this never occurs
        // with real release branches, but might more likely occur when you touch this very file.
        while(Jenkins.instance.getItemByFullName(job) == null || Jenkins.instance.getItemByFullName(job).isDisabled()) {
            sleep 3
        }
    }
}
