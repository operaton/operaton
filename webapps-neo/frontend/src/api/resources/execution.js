import { PUT } from "../helper.jsx";

/**
 * Set a variable in one execution's own scope. A variable set on the process
 * instance is visible everywhere; one set here belongs to that branch alone,
 * which is what parallel paths need.
 */
const set_local_variable = (state, execution_id, name, body) =>
  PUT(
    `/execution/${execution_id}/localVariables/${encodeURIComponent(name)}`,
    body,
    state,
    state.api.execution.set_variable,
  );

const execution = { set_local_variable };

export default execution;
