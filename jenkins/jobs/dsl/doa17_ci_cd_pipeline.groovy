// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def applicationRepository = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/doa17-application"

// Views
def doa17CICDPipeline = buildPipelineView(projectFolderName + "/DOA17_CI_CD_Pipeline")

// Jobs DOA17_CI_CD_Pipeline
def doa17CodeBuild = freeStyleJob(projectFolderName + "/DOA17_Code_Build")
def doa17CodeDeployDevelopment = freeStyleJob(projectFolderName + "/DOA17_Code_Deploy_Development")
def doa17CodeDeployProduction = freeStyleJob(projectFolderName + "/DOA17_Code_Deploy_Production")

// DOA17_CI_CD_Pipeline
doa17CICDPipeline.with{
    title('DOA17 CI CD Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/DOA17_Code_Build")
    showPipelineDefinitionHeader()
    alwaysAllowManualTrigger()
    refreshFrequency(5)
}

// Job DOA17_Code_Build
doa17CodeBuild.with{
  description("Job Description")
  environmentVariables {
    env('WORKSPACE_NAME', workspaceFolderName)
    env('PROJECT_NAME', projectFolderName)
  }
  parameters{
    stringParam("AWS_REGION",'',"Default AWS Region")
  }
  wrappers {
    preBuildCleanup()
    maskPasswords()
  }
  label("docker")
    steps {
    shell('''
set +x

set -x'''.stripMargin()
    )
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/DOA17_Code_Deploy_Development"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          currentBuild()
        }
      }
    }
  }
}

// Job DOA17_Code_Deploy_Development
doa17CodeDeployDevelopment.with{
  description("Job Description")
  environmentVariables {
    env('WORKSPACE_NAME', workspaceFolderName)
    env('PROJECT_NAME', projectFolderName)
  }
  parameters{
    stringParam("AWS_REGION",'',"Default AWS Region")
  }
  wrappers {
    preBuildCleanup()
    maskPasswords()
  }
  label("docker")
    steps {
    shell('''
set +x
export AWS_DEFAULT_REGION=$AWS_REGION
echo "[INFO] Default region is set to $AWS_DEFAULT_REGION"

echo "[INFO] Creating Code Deploy Application ${ENVIRONMENT_NAME}-WebApp"
aws deploy create-application --application-name ${ENVIRONMENT_NAME}-WebApp

set -x'''.stripMargin()
    )
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/DOA17_Code_Deploy_Production"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          currentBuild()
        }
      }
    }
  }
}

// Job DOA17_Code_Deploy_Production
doa17CodeDeployProduction.with{
  description("Job Description")
  environmentVariables {
    env('WORKSPACE_NAME', workspaceFolderName)
    env('PROJECT_NAME', projectFolderName)
  }
  parameters{
    stringParam("AWS_REGION",'',"Default AWS Region")
  }
  wrappers {
    preBuildCleanup()
    maskPasswords()
  }
  label("docker")
    steps {
    shell('''
set +x
export AWS_DEFAULT_REGION=$AWS_REGION
echo "[INFO] Default region is set to $AWS_DEFAULT_REGION"

echo "[INFO] Creating Code Deploy Deployment Group ${ENVIRONMENT_NAME}-DevWebApp"
aws deploy create-deployment-group --application-name ${ENVIRONMENT_NAME}-WebApp  --deployment-config-name CodeDeployDefault.OneAtATime --deployment-group-name ${ENVIRONMENT_NAME}-DevWebApp --ec2-tag-filters Key=Name,Value=${ENVIRONMENT_NAME}-DevWebApp,Type=KEY_AND_VALUE --service-role-arn ${CODE_DEPLOY_ARN}
set -x'''.stripMargin()
    )
  }
}