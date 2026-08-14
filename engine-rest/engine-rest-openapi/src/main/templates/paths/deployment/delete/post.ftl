<#macro endpoint_macro docsUrl="">
{
  <@lib.endpointInfo
      id = "deleteDeployments"
      tag = "Deployment"
      summary = "Delete Deployments"
      desc = "Deletes a list of deployments by the given ids." />

  <@lib.requestBody
      mediaType = "application/json"
      dto = "DeleteDeploymentsDto"
      examples = [
      '"example-1": {
            "summary": "POST /delete",
            "value": {
              "deploymentIds": [
                "deploymentId1",
                "deploymentId2"
              ],
              "skipCustomListeners": true,
              "skipIoMappings": true,
              "cascade": true
            }
        }']
  />

  "responses" : {

    <@lib.response
        code = "200"
        desc = "Request successful."
        dto = "DeleteDeploymentResponse"
        array = true
        examples = ['"example-1": {
                       "summary": "Status 200 Response",
                       "value": [
                         {
                           "status": "SUCCESS",
                           "errorMessage": null,
                           "deploymentId": "aDeploymentId"
                         },
                         {
                           "status": "SUCCESS",
                           "errorMessage": null,
                           "deploymentId": "anotherDeploymentId"
                         }
                       ]
                     }'] />

    <@lib.response
        code = "207"
        desc = "Multi-Status: Indicates that at least one delete operation has failed."
        dto = "DeleteDeploymentResponse"
        array = true
        examples = ['"example-1": {
                       "summary": "Status 207 Response",
                       "value": [
                         {
                           "status": "FAILURE",
                           "errorMessage": "Deployment with id \'aDeploymentId\' does not exist",
                           "deploymentId": "aDeploymentId"
                         },
                         {
                           "status": "SUCCESS",
                           "errorMessage": null,
                           "deploymentId": "anotherDeploymentId"
                         }
                       ]
                     }'] />

    <@lib.errorResponses docsUrl=docsUrl last = true />
  }
}
</#macro>
