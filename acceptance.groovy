import com.sap.icd.jenkins.Utils
import com.sap.piper.internal.Deprecate
import com.sap.piper.internal.ConfigurationHelper
import com.sap.piper.internal.Notify
import groovy.text.SimpleTemplateEngine
import groovy.transform.Field
import static com.sap.piper.internal.Prerequisites.checkScript

void call(Map params) {
	//access stage name
	echo "Start - Extension for stage: ${params.stageName}"

	//access config
	echo "Current stage config: ${params.config}"

	//execute original stage as defined in the template
	params.originalStage()

	// get utils to get files from stash
	def utils = params?.juStabUtils ?: new Utils()

	//access overall pipeline script object
	echo "Branch: ${params.script.commonPipelineEnvironment.gitBranch}"
	node {
		dir('target') {
			deleteDir()
		}

		utils.unstashAll(["deployDescriptor"])

		downloadArtifactsFromNexus script: params.script, fromStaging: true

		sh 'ls -laR .'

		durationMeasure(script: this, measurementName: 'cloudFoundryDeploy_Duration') {
// 		        artifactType: 'mta', nexusUrl: 'https://int.repositories.cloud.sap/'
			cloudFoundryDeploy script: this, buildTool: 'mta', mtaPath: 'target/comsappmtMCS.mtar', cfOrg: 'cpea_awsbasmaster', cfSpace: 'FIMP', cfApiEndpoint: 'https://api.cf.eu10.hana.ondemand.com/', cfCredentialsId: 'HiltiCF' , deployType: 'blue-green' , mtaDeployParameters: '-f --version-rule ALL' 
                        }
		
	}

	echo "End - Extension for stage: ${params.stageName}"
}

return this
