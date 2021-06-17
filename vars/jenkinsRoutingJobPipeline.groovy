#!/usr/bin/env groovy

/**
 * The CI pipeline to downstream jobs by source changes.
 *
 * @param  java.util.List The array with jobs defined as
 * [name: 'name ow dowstream Jenkins job', path: 'subfolder filter']
 */
def call(java.util.List jobs){
    pipeline {
        agent { label 'dockerpre-build' }
        options{
            timeout(time: 10, unit: 'MINUTES')
        }
        stages {
            stage('Downstream job trigger'){
                steps{
                    script{
                        jenkinsDownstreamJobTrigger(jobs)
                    }
                }
            }
        }
    }
}
