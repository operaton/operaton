import {
  RESPONSE_STATE,
  _url_engine_rest,
  GET,
  GET_SERVER_URL,
  GET_TEXT,
  POST,
  PUT,
  get_credentials,
} from "../helper.jsx";
import engine_rest from "../engine_rest.jsx";

const get_process_instance_tasks = (state, instance_id) =>
  GET(
    `/task?processInstanceId=${instance_id}`,
    state,
    state.api.task.by_process_instance,
  );

const get_task = (state, task_id) =>
  GET(`/task/${task_id}`, state, state.api.task.one);
// .then(() => get_identity_links(state, state.api.task.one.value.data.id))

/**
 * Update the task by providing a changeset `task` which is merged
 * onto the existing task from the task data present in `state.api.task.one`.
 * @param state
 * @param task
 * @param task_id
 * @returns {Promise<{status: RESPONSE_STATE, data: *} | *>}
 */
const update_task = (state, task, task_id) => {
  return state.api.task.one.value?.data !== undefined
    ? PUT(
        `/task/${state.api.task.one.value.data.id}`,
        { ...state.api.task.one.value.data, ...task },
        state,
        state.api.task.one,
      ).then(() => engine_rest.task.get_task(state, task_id))
    : () => {
        throw new Error("Task in state undefined or null, can not merge");
      };
};

const get_task_form = (state, form_id) =>
  GET_SERVER_URL(`/${form_id}`, state, state.api.task.form);

const get_task_rendered_form = (state, task_id) =>
  GET_TEXT(
    `/task/${task_id}/rendered-form`,
    state,
    state.api.task.rendered_form,
  );

const get_task_deployed_form = (state, task_id) =>
  GET(`/task/${task_id}/deployed-form`, state, state.api.task.deployed_form);

const claim_task = (state, task_id) =>
  POST(
    `/task/${task_id}/claim`,
    { userId: state.api.user.profile.value.id },
    state,
    state.api.task.claim_result,
  );

const unclaim_task = (state, task_id) =>
  POST(
    `/task/${task_id}/unclaim`,
    { userId: state.api.user.profile.value.id },
    state,
    state.api.task.unclaim_result,
  );

const assign_task = (state, assignee, task_id) =>
  POST(
    `/task/${task_id}/assignee`,
    { userId: assignee },
    state,
    state.api.task.assign_result,
  );

const get_identity_links = (state, task_id) =>
  GET(`/task/${task_id}/identity-links`, state, state.api.task.identity_links);

const add_group = (state, task_id, groupId) =>
  POST(
    `/task/${task_id}/identity-links`,
    { groupId, type: "candidate" },
    state,
    state.api.task.add_group,
  );

const delete_group = (state, task_id, groupId) =>
  POST(
    `/task/${task_id}/identity-links/delete`,
    { groupId, type: "candidate" },
    state,
    state.api.task.delete_group,
  );

const tasks_with_process_definitions = async (tasks, state) => {
  const definition_ids = [
    ...new Set(tasks.map((task) => task.processDefinitionId)),
  ];

  await get_task_process_definitions(state, definition_ids).then((defList) => {
    const defMap = new Map(defList.map((def) => [def.id, def]));

    tasks.forEach((task) => {
      const def = defMap.get(task.processDefinitionId);
      task.definitionName = def?.name ?? "";
      task.definitionVersion = def?.version ?? "";
    });
  });

  return tasks;
};

const get_tasks = (state, sort_key = "name", sort_order = "asc") => {
  let headers = new Headers();
  headers.set(
    "Authorization",
    `Basic ${window.btoa(unescape(encodeURIComponent(get_credentials(state))))}`,
  );

  fetch(
    `${_url_engine_rest(state)}/task?sortBy=${sort_key}&sortOrder=${sort_order}`,
    { headers },
  )
    .then((response) =>
      response.ok ? response.json() : Promise.reject(response),
    )
    .then((tasks) => tasks_with_process_definitions(tasks, state))
    .then(
      (json) =>
        (state.api.task.list.value = {
          status: RESPONSE_STATE.SUCCESS,
          data: json,
        }),
    )
    .catch(
      (error) =>
        (state.api.task.list.value = { status: RESPONSE_STATE.ERROR, error }),
    );
};

const get_task_process_definitions = (state, ids) =>
  fetch(
    `${state.server.value.url}/engine-rest/process-definition?processDefinitionIdIn=${ids}`,
    {
      headers: new Headers({
        Authorization: `Basic ${window.btoa("demo:demo")}`,
      }), // fallback, wenn global fehlt
    },
  ).then((r) => r.json());

const post_task_form = (state, task_id, data) =>
  POST(
    `/task/${task_id}/submit-form`,
    {
      variables: data,
      withVariablesInReturn: true,
    },
    state,
    state.api.task.submit_form,
  );

const task = {
  get_tasks,
  get_task,
  update_task,
  get_task_form,
  get_process_instance_tasks,
  get_task_rendered_form,
  get_task_deployed_form,
  claim_task,
  unclaim_task,
  assign_task,
  post_task_form,
  add_group,
  delete_group,
  get_identity_links,
};

export default task;
