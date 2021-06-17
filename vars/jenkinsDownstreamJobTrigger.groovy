#!/usr/bin/env groovy

/**
 * The function helps to run a Jenkins job if a change was made in a git into the subfolder.
 *
 * @param  java.util.List The array with jobs defined as
 * [name: 'name of downstream Jenkins job', path: 'subfolder filter']
 */
def call(java.util.List jobs) {
    boolean changesExist = false
    jobs.each { job ->
        if(gitHasChangesIn(job.path)) {
            changesExist = true
            // check that branch is master or PR and run a the job after.
            jenkinsMasterOrPrSelection(job.name)
        }
    }
    if (!changesExist) {
        log.info("no changes found to any of the projects")
    }
}
