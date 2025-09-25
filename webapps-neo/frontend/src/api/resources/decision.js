import { GET, GET_TEXT } from '../helper.jsx'


const get_decision_definition = (state, id) =>
  GET(`/decision-definition/${id}`, state, state.api.decision.definition)

const get_decision_definitions = (state) =>
  GET("/decision-definition", state, state.api.decision.definitions)


const get_dmn_xml = (state, id) =>
  GET(`/decision-definition/${id}/xml`, state, state.api.decision.dmn)


const decision = {
  get_decision_definition,
  get_decision_definitions,
  get_dmn_xml,
}

export default decision
