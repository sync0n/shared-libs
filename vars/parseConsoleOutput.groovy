import hudson.model.*

def call (){
    def matcher = manager.getLogMatcher('^Report portal link (.*)$')

    println "${matcher}"
}