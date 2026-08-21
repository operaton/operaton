import { GET, PUT, DELETE } from "../helper.jsx";

/**
 * Runtime incidents (distinct from the history-only `history.incident.*`).
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Incident
 */
const get_incidents_by_process_instance = (state, instance_id) =>
  GET(
    `/incident?processInstanceId=${instance_id}`,
    state,
    state.api.incident.by_process_instance,
  );

const get_incidents_by_process_definition = (state, definition_id) =>
  GET(
    `/incident?processDefinitionId=${definition_id}`,
    state,
    state.api.incident.by_process_definition,
  );

const set_incident_annotation = (state, incident_id, annotation) =>
  PUT(
    `/incident/${incident_id}/annotation`,
    { annotation },
    state,
    state.api.incident.annotation,
  );

const clear_incident_annotation = (state, incident_id) =>
  DELETE(
    `/incident/${incident_id}/annotation`,
    null,
    state,
    state.api.incident.annotation,
  );

const incident = {
  by_process_instance: get_incidents_by_process_instance,
  by_process_definition: get_incidents_by_process_definition,
  set_annotation: set_incident_annotation,
  clear_annotation: clear_incident_annotation,
};

export default incident;
