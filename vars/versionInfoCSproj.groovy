
import com.linn.Result
def call (versionFilePath) {
	try{
		def xml_file = readFile versionFilePath
		def xml_trim = xml_file.trim().replaceFirst("^([\\W]+)<","<")
		def xmlText = new XmlSlurper().parseText(groovy.xml.XmlUtil.serialize(xml_trim))
		def CurrentVersion = xmlText.'PropertyGroup'.depthFirst().findAll { it.name() == 'Version' }
		if ( CurrentVersion.size() > 1 ) {
			log.error("Multiple Versions.")
		} else if ( CurrentVersion.size() == 0 ) {
			log.error("Version not found.")
		} else {
			CurrentVersion = CurrentVersion.join(",")
			log.info("Current Version is ${CurrentVersion}")
			return CurrentVersion.trim()
		}
	} catch(Exception error){
		log.error("Project file version failure. " + error.toString() )
		return new Result(status: false, buildResult: 'FAILURE')
	}
}
