<!--
  Use this template when adding or modifying REST API endpoint documentation in the OpenAPI spec templates.
  Style guide: docs/api/rest-api-documentation-guidelines.md
-->

## Summary

<!-- Briefly describe what endpoint documentation was added or changed and why -->

## Changed Templates

<!-- List the FTL template files modified, e.g.:
- paths/process-definition/get.ftl
- paths/task/{id}/complete/post.ftl
- lib/commons/sort-params.ftl
-->

---

## Documentation Checklist

### Operation Metadata

- [ ] `operationId` is **unchanged** from the existing value (if modifying an existing endpoint)
- [ ] `tag` exactly matches a name in the `main.ftl` `"tags"` array
- [ ] `summary` is 3–10 words, uses an approved verb, has no HTTP method label, no articles, no trailing punctuation
- [ ] `deprecated = true` is set if and only if the operation is deprecated

### Descriptions

- [ ] Description starts with an active-voice verb phrase (not "This endpoint...", not "Returns...")
- [ ] No Java class or interface names appear (`ProcessDefinition interface`, `RuntimeService`, etc.)
- [ ] No hardcoded documentation URLs — all links use `${docsUrl}`
- [ ] For List endpoints: cross-reference to the Count endpoint is present
- [ ] Security considerations are documented if the endpoint executes custom code or modifies access controls
- [ ] Deprecation notice follows the formula: `**Deprecated:** <reason>. Use <alternative> instead.`

### Parameters

- [ ] All path parameters have `required = true`
- [ ] All path parameters explain the ID format and how to obtain it
- [ ] All date parameters explicitly state ISO 8601 format
- [ ] All enum parameters list every accepted value with its effect in the prose description
- [ ] All boolean parameters explain both the `true` and `false` (or default) effect
- [ ] The final parameter in the list has `last = true`

### Request Body

- [ ] All POST/PUT/PATCH endpoints have `@lib.requestBody` unless the endpoint accepts no body
- [ ] All POST/PUT/PATCH endpoints have at least one request body example
- [ ] Example summaries follow the formula: `<METHOD> /<path>` (optionally with a disambiguating label)
- [ ] Example IDs use realistic formats (`key:version:uuid` for process definitions, UUIDs for instances)
- [ ] Example variables use the typed-value format `{"type": "String", "value": "...", "valueInfo": {}}`

### Responses

- [ ] All 200 responses have at least one response example
- [ ] 204 responses have no `dto` or `examples`
- [ ] 400 descriptions are specific about the invalid condition (not just "Bad Request")
- [ ] 401 and 403 descriptions use the standard boilerplate with `${docsUrl}` link
- [ ] 404 descriptions name the missing resource type and the identifier field
- [ ] The final response in the list has `last = true`

### FreeMarker Syntax

- [ ] Template compiles without errors: `./mvnw clean package -pl engine-rest/engine-rest-openapi -DskipTests`
- [ ] No hardcoded version strings in documentation URLs
- [ ] Backticks inside `desc` strings are escaped as `` \` ``
- [ ] No trailing commas in generated JSON (verify by inspecting the output spec after build)

### Registration

- [ ] The endpoint path is registered in the `endpoints` map in `main.ftl` (for new endpoints)
- [ ] The tag name appears in the `"tags"` array in `main.ftl` (for new tags)
- [ ] CMMN fields (`caseInstanceId`, `caseExecutionId`, `caseDefinitionId`) are marked `deprecated = true`
