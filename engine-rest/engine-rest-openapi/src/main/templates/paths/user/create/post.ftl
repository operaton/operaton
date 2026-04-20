<#macro endpoint_macro docsUrl="">
{

  <@lib.endpointInfo
      id = "createUser"
      tag = "User"
      summary = "Create User"
      desc = "Create a new user." />

  <@lib.requestBody
      mediaType = "application/json"
      dto = "UserDto"
      examples = ['"example-1": {
                     "summary": "POST /user/create",
                     "value": {
                         "profile": {
                             "id": "jonny1",
                             "firstName": "John",
                             "lastName": "Doe",
                             "email": "anEmailAddress"
                             },
                         "credentials": {
                            "password": "s3cret"
                        }
                   }
                   }'] />

  "responses" : {

    <@lib.response
        code = "204"
        desc = "Request successful." />

    <@lib.errorResponses docsUrl=docsUrl last = true />
  }
}
</#macro>