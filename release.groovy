import com.sap.piper.internal.ConfigurationHelper
import com.sap.piper.internal.Notify

import groovy.text.SimpleTemplateEngine
import groovy.transform.Field

import static com.sap.piper.internal.Prerequisites.checkScript

void call(Map params) {
    def PROD_DEPLOYMENT = "false"
    
    stage('1 - Deploy in Canary') {
        parallel(
            'Deploy to cf-eu21-canary': {
                node {
                    deleteDir()
                    try {
                        durationMeasure(script: this, measurementName: 'deploy_test_duration') {
                            downloadArtifactsFromNexus script: this, artifactType: 'mta', buildTool: 'mta', nexusUrl: 'https://int.repositories.cloud.sap/', fromStaging: false
                            cloudFoundryDeploy script: this, cfOrg: 'cas-prod123', cfSpace: 'prod', cfApiEndpoint: 'https://api.cf.eu21.hana.ondemand.com', cfCredentialsId: 'CF_CREDENTIAL', mtaExtensionDescriptor: 'descriptors/prod-eu21.mtaext', testServerUrl: 'https://content-agent-engine.cfapps.eu21.hana.ondemand.com/health'
                            PROD_DEPLOYMENT = "true"
                            echo "PROD_DEPLOYMENT is true"
                        }
                    } catch(Exception ex) {
                        PROD_DEPLOYMENT = "false"
                        echo "PROD_DEPLOYMENT is false"
                        throw ex //if you wan to fail 
                    }
                }
            }, failFast: false
        )
    }
	echo "PROD_DEPLOYMENT"
    echo PROD_DEPLOYMENT

    if(PROD_DEPLOYMENT == "true") {
        echo "Dora Reporting start ..."
        
        def GIT_COMMIT = commonPipelineEnvironment.getGitCommitId()
        def ARTIFACT_VERSION = commonPipelineEnvironment.getArtifactVersion()
        
        echo "Checking ARTIFACT_VERSION status for Dora Reporting..."
        echo ARTIFACT_VERSION
        
        withCredentials([
            string(credentialsId: 'devOpsInsightsTokenCredentialsId', variable: 'devOpsInsightsToken'),
            usernamePassword(credentialsId: 'PAT_P2002260846', usernameVariable: 'user', passwordVariable: 'pwd'),
            usernamePassword(credentialsId: 'Jenkins_I303394', usernameVariable: "jenkinsUser", passwordVariable: 'pwdJenkins')
        ]) {
            sapCollectInsights(script: this,
                artifactVersion: "${ARTIFACT_VERSION}",
                deploymentTarget: 'production',
                gitInstance: 'github.wdf.sap.corp',
                gitOrganization: 'content-agent',
                gitRepository: 'cas',
                identifier: 'content-agent',
                commitId: "${GIT_COMMIT}",
                verbose: true,
                collectChangeSetProduction: true,
                githubToken: "${pwd}",
                devOpsInsightsToken: "${devOpsInsightsToken}"
            )
        }
        
        echo "Dora Reporting end"
    }
}
