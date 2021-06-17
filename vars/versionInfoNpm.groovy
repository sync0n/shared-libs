
import com.linn.Result
import groovy.json.JsonSlurperClassic

def call () {
	try{
		def jsonFile = readFile "package.json"
		def jsonSlurper = new JsonSlurperClassic()
		jsonText = jsonSlurper.parseText(jsonFile)
		def CurrentVersion = jsonText['version']
		return CurrentVersion.trim() 
	} catch(Exception error){
		log.error("Package file version failure. " + error.toString() )
		return new Result(status: false, buildResult: 'FAILURE')
	}
}