import { GET, PAGINATED_GET } from "../helper.jsx";

const get_decision_definition = (state, id) =>
  GET(`/decision-definition/${id}`, state, state.api.decision.definition);

const get_decision_definitions = (state, params = {}) => {
  const qs = new URLSearchParams(params).toString();
  return GET(
    `/decision-definition${qs ? `?${qs}` : ""}`,
    state,
    state.api.decision.definitions,
  );
};

const get_dmn_xml = (state, id) =>
  GET(`/decision-definition/${id}/xml`, state, state.api.decision.dmn);

/**
 * Historic decision (DMN) evaluations for a definition.
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Historic-Decision-Instance
 */
const get_decision_instances = (state, definition_id, firstResult = 0) =>
  PAGINATED_GET(
    `/history/decision-instance?decisionDefinitionId=${definition_id}&sortBy=evaluationTime&sortOrder=desc`,
    state,
    state.api.decision.instances,
    firstResult,
    20,
  );

const get_decision_instance = (state, id) =>
  GET(
    `/history/decision-instance/${id}?includeInputs=true&includeOutputs=true`,
    state,
    state.api.decision.instance,
  );

const decision = {
  get_decision_definition,
  get_decision_definitions,
  get_dmn_xml,
  get_decision_instances,
  get_decision_instance,
};

export default decision;
