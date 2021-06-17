#!/usr/bin/env groovy
package com.linn

/**
 * Utilities class contains usefull function.
 */
class Utilities {
    /**
     * Sort versions in string format.
     *
     * @param versions String list with versions.
     * @param descending Boolean flag to choose ordering.
     * @return List with sorted versions.
     */
    @NonCPS
    static List sortVersions(List versions, boolean descending = true) {
        def sortedVersions = versions.sort() { a, b ->
            // split versions by dots (.)
            List versionA = a.tokenize('.')
            List versionB = b.tokenize('.')

            // find minimal version size
            def commonIndices = Math.min(versionA.size(), versionB.size())

            for (int i = 0; i < commonIndices; ++i) {
                // get indices
                int numberA = versionA[i].toInteger()
                int numberB = versionB[i].toInteger()

                // compare indices
                if (numberA != numberB) {
                    return descending ? numberB <=> numberA : numberA <=> numberB
                }
            }

            // we have to understand what version is longer, if common indices are identical
            return descending ? versionB.size() <=> versionA.size() : versionA.size() <=> versionB.size()
        }

        return sortedVersions
    }
}
