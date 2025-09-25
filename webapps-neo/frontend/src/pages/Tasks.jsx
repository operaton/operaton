import { useSignal } from "@preact/signals";
import { useRoute } from "preact-iso";
import { useContext, useLayoutEffect } from "preact/hooks";
import { useTranslation } from "react-i18next";

import engine_rest, { RequestState, RESPONSE_STATE } from "../api/engine_rest.jsx";
import * as Icons from "../assets/icons.jsx";
import { BPMNViewer } from "../components/BPMNViewer.jsx";
import { Tabs } from "../components/Tabs.jsx";
import * as formatter from "../helper/date_formatter.js";
import { AppState } from "../state.js";
import { StartProcessList } from "./StartProcessList.jsx";
import { TaskForm } from "./TaskForm.jsx";

const TasksPage = () => {
  const state = useContext(AppState);
  const { params } = useRoute();

  if (state.api.task.list.value === null) {
    void engine_rest.task.get_tasks(state);
  }

  return (
    <main id="content" class="tasks fade-in">
      <TaskList />
      {{
        start: <StartProcessList />,
        filter: <Filter />,
        undefined: <NoSelectedTask />,
      }[params?.task_id] ?? <Task />}
    </main>
  );
};

const TaskList = () => {
  const state = useContext(AppState),
    taskList = state.api.task.list,
    { params } = useRoute(),
    selectedTaskId = params.task_id,
    [t] = useTranslation("translation", { keyPrefix: "tasks" });

  // todo remove me, testing onlyx
  // i18n.changeLanguage('de-DE');

  return (
    <div id="task-list">
      <h2 class="screen-hidden">{t("title")}</h2>
      <div>
        <table>
          <thead>
            <tr>
              <th>{t("task-list.table-headings.task-name")}</th>
              <th>{t("task-list.table-headings.processes-definition")}</th>
              <th>{t("task-list.table-headings.processes-version")}</th>
              <th>{t("task-list.table-headings.created-on")}</th>
              <th>{t("task-list.table-headings.assignee")}</th>
              <th>{t("task-list.table-headings.priority")}</th>
            </tr>
          </thead>
          <tbody>
            <RequestState
              signal={taskList}
              on_success={() =>
                taskList.value?.data?.map((task) => (
                  <TaskRowEntry key={task.id} task={task} selected={task.id === selectedTaskId} />
                ))
              }
            />
          </tbody>
        </table>
      </div>
      <div id="task-actions">
        <a href="/tasks/start" className="button">
          Start&nbsp;Process
        </a>
        <a href="/tasks/filter" className="button">
          Create Filter
        </a>
        <h3>Filters</h3>
        <ul id="filter-list">
          <li aria-selected={true}>All Tasks</li>
          <li>My Tasks</li>
        </ul>
      </div>
    </div>
  );
};

const TaskRowEntry = ({ task, selected }) => {
  const { id, name, created, assignee, priority, definitionName, definitionVersion } = task;

  useLayoutEffect(() => {
    if (selected) {
      document.getElementById(id).scrollIntoView({ behavior: "instant", block: "center" });
    }
  });

  return (
    <tr id={id} key={id} aria-selected={selected}>
      <th scope="row">
        <a href={`/tasks/${id}/${task_tabs[0].id}`} aria-labelledby={id}>
          {name}
        </a>
      </th>
      <td>{definitionName}</td>
      <td>{definitionVersion}</td>
      <td>{formatter.formatRelativeDate(created)}</td>
      <td>{assignee ? assignee : "-"}</td>
      <td>{priority}</td>
    </tr>
  );
};

const NoSelectedTask = () => (
  <div id="task-details" className="fade-in">
    <div class="task-empty">Select a task from the task list above to show its details.</div>
  </div>
);

// when something has changed (e.g. assignee) in the task we have to update the task list
const Task = () => {
  const state = useContext(AppState),
    {
      api: {
        task: { one: task },
        process: {
          definition: { one: pd },
        },
      },
    } = state;

  return (
    <div id="task-details" className="fade-in">
      <section id="task-data">
        <div>
          <h2>{task.value?.data?.name}</h2>
          <a href={`/processes/${pd.value?.data?.id}`}>
            {pd.value?.data?.name} (version {pd.value?.data?.version})
          </a>
          {state.api.task.one.value?.data !== undefined ? (
            <p>{state.api.task.one.value?.data.description}</p>
          ) : (
            <p>No description provided.</p>
          )}
        </div>

        <dl>
          <dt>Follow Up Date</dt>
          <dd>
            <SetFollowUpDateButton />
          </dd>
          <dt>Due Date</dt>
          <dd>
            <SetDueDateButton />
          </dd>
          <dt>Assignee</dt>
          <dd>
            <ClaimButton />
          </dd>
          <dt>Groups</dt>
          <dd>
            <SetGroupsButton />
          </dd>
        </dl>

        <button>
          <Icons.chat_bubble_left /> Comment
        </button>
      </section>
      <TaskTabs />
    </div>
  );
};

const TaskTabs = () => {
  const state = useContext(AppState);
  const { params } = useRoute();
  const currentTaskId = useSignal(null);

  // reset error/result state (optional)
  state.task_claim_result.value = null;
  state.task_assign_result.value = null;

  if (currentTaskId.value !== params.task_id) {
    currentTaskId.value = params.task_id;
    engine_rest.task
      .get_task(state, params.task_id)
      .then(() => engine_rest.process_definition.one(state, state.api.task.one.value?.data?.processDefinitionId))
      .then(() => engine_rest.task.get_identity_links(state, state.api.task.one.value?.data?.id));
  }

  return (
    <section className="task-tabs">
      {state.api.task.one.value.data !== null && state.api.task.one.value.data !== undefined ? (
        <>
          <Tabs tabs={task_tabs} base_url={`/tasks/${state.api.task.one.value.data.id}`} className="fade-in" />
        </>
      ) : (
        "Loading"
      )}
    </section>
  );
};

const SetDueDateButton = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    {
      api: {
        task: { one: task },
      },
    } = state,
    close = () => document.getElementById("set_due_date").close(),
    show = () => document.getElementById("set_due_date").showModal(),
    due_date = task.value?.data?.due ? new Date(Date.parse(task.value?.data?.due)) : null,
    date_state = useSignal({
      date: due_date !== null ? due_date?.toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
      time:
        due_date !== null
          ? due_date?.toISOString().split("T")[1].substring(0, 5)
          : new Date().toISOString().split("T")[1].substring(0, 5),
    }),
    submit = (event) => {
      event.preventDefault();
      engine_rest.task
        .update_task(state, { due: `${date_state.value.date}T${date_state.value.time}:0.000+0000` }, params.task_id)
        .then(() => {
          close();
        });
    };

  return (
    <>
      <button onClick={show} class="link">
        {due_date === null ? "Set Due Date" : due_date.toLocaleString()}
      </button>

      <dialog id="set_due_date">
        <button onClick={close}>Close</button>
        <h2>Set Due Date for Task</h2>

        <form onSubmit={submit}>
          <label for="date">Date</label>
          <input
            type="date"
            id="date"
            value={due_date !== null ? due_date?.toISOString().split("T")[0] : null}
            onInput={(e) => (date_state.value["date"] = e.currentTarget.value)}
          />
          <label for="time">Time</label>
          <input
            type="time"
            id="time"
            value={due_date !== null ? due_date?.toISOString().split("T")[1].substring(0, 5) : null}
            onInput={(e) => (date_state.value["time"] = e.currentTarget.value)}
          />
          <div class="button-group">
            <button type="submit">Submit</button>
          </div>
        </form>
      </dialog>
    </>
  );
};

const SetFollowUpDateButton = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    {
      api: {
        task: { one: task },
      },
    } = state,
    close = () => document.getElementById("set_follow_up_date").close(),
    show = () => document.getElementById("set_follow_up_date").showModal(),
    followUpDate = task.value?.data?.followUp ? new Date(Date.parse(task.value?.data?.followUp)) : null,
    date_state = useSignal({
      date: followUpDate !== null ? followUpDate?.toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
      time:
        followUpDate !== null
          ? followUpDate?.toISOString().split("T")[1].substring(0, 5)
          : new Date().toISOString().split("T")[1].substring(0, 5),
    }),
    // due:	"2025-06-18T13:58:44.000+0000"
    submit = (event) => {
      event.preventDefault();
      engine_rest.task
        .update_task(
          state,
          { followUp: `${date_state.value.date}T${date_state.value.time}:0.000+0000` },
          params.task_id,
        )
        .then(() => {
          close();
        });
    };

  return (
    <>
      <button onClick={show} class="link">
        {followUpDate === null ? "Set Due Date" : followUpDate.toLocaleString()}
      </button>

      <dialog id="set_follow_up_date">
        <button onClick={close}>Close</button>
        <h2>Set Follow Up Date for Task</h2>

        <form onSubmit={submit}>
          <label for="date">Date</label>
          <input
            type="date"
            id="date"
            value={followUpDate !== null ? followUpDate?.toISOString().split("T")[0] : null}
            onInput={(e) => (date_state.value["date"] = e.currentTarget.value)}
          />
          <label for="time">Time</label>
          <input
            type="time"
            id="time"
            value={followUpDate !== null ? followUpDate?.toISOString().split("T")[1].substring(0, 5) : null}
            onInput={(e) => (date_state.value["time"] = e.currentTarget.value)}
          />
          <div class="button-group">
            <button type="submit">Submit</button>
          </div>
        </form>
      </dialog>
    </>
  );
};

const SetGroupsButton = () => {
  const state = useContext(AppState),
    {
      api: {
        task: { identity_links },
      },
    } = state,
    close = () => document.getElementById("add_groups").close(),
    show = () => document.getElementById("add_groups").showModal(),
    group_state = useSignal(null),
    submit = (event) => {
      event.preventDefault();
      engine_rest.task.add_group(state, state.api.task.one.value.data.id, group_state.value).then(() => {
        if (state.api.task.add_group.value.status === RESPONSE_STATE.SUCCESS) {
          group_state.value = "";
        }
      });
    },
    delete_group = (group_id) => engine_rest.task.delete_group(state, state.api.task.one.value.data.id, group_id);

  return (
    <>
      <button onClick={show} class="link">
        {identity_links.value?.data
          ? "Set groups"
          : identity_links.value?.data
              ?.reduce((res, { groupId, type }) => (type === "candidate" ? `${res + groupId}, ` : res), "")
              .slice(0, -2)}
      </button>

      <dialog id="add_groups">
        <header>
          <h2>Manage Groups</h2>
          <button onClick={close} class="neutral">
            <Icons.close />
          </button>
        </header>

        <h3>Add Groups</h3>
        <form onSubmit={submit}>
          <label for="group_id">Group ID</label>
          <input id="group_id" key="group_id" required onInput={(e) => (group_state.value = e.currentTarget.value)} />
          <div class="button-group">
            <button type="submit">Add Group</button>
          </div>
        </form>

        <h3>Remove Groups</h3>

        <RequestState
          signal={state.api.task.identity_links}
          on_success={() => (
            <table>
              <thead>
                <tr>
                  <th>Group ID</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {state.api.task.identity_links.value.data.map(({ groupId, type }, index) =>
                  type === "candidate" ? (
                    <tr key={index}>
                      <td>{groupId}</td>
                      <td>
                        <button onClick={() => delete_group(groupId)}>Delete</button>
                      </td>
                    </tr>
                  ) : null,
                )}
              </tbody>
            </table>
          )}
        />
      </dialog>
    </>
  );
};

const ClaimButton = () => {
  const state = useContext(AppState),
    task = state.api.task.one.value?.data,
    user = state.api.user.profile.value?.data,
    claim_result = state.api.task.claim_result.value?.data,
    assign_result = state.api.task.assign_result.value?.data,
    unclaim_result = state.api.task.unclaim_result.value?.data,
    close = () => document.getElementById("set_assignee").close(),
    show = () => document.getElementById("set_assignee").showModal(),
    user_is_assignee = task?.assignee,
    assignee_is_different = task?.assignee && user?.id !== task?.assignee,
    claimed = claim_result?.status === RESPONSE_STATE.SUCCESS,
    assigned = assign_result?.status === RESPONSE_STATE.SUCCESS,
    unclaimed = unclaim_result?.status === RESPONSE_STATE.SUCCESS;

  return (
    <RequestState
      signal={state.api.task.one}
      on_success={() => (
        <>
          <button class="link" onClick={show}>
            {task?.assignee === null ? "Claim" : task?.assignee === user?.id ? "You" : task?.assignee}
          </button>

          <dialog id="set_assignee">
            <button onClick={close}>Close</button>
            {assignee_is_different && !assigned ? (
              <button onClick={() => engine_rest.task.assign_task(state, null, task.id)} className="secondary">
                <Icons.user_minus /> Reset Assignee
              </button>
            ) : (user_is_assignee || claimed) && !unclaimed ? (
              <button onClick={() => engine_rest.task.unclaim_task(state, task.id)} className="secondary">
                <Icons.user_minus /> Unclaim
              </button>
            ) : (
              <button onClick={() => engine_rest.task.claim_task(state, task.id)} className="secondary">
                <Icons.user_plus /> Claim
              </button>
            )}
          </dialog>
        </>
      )}
    />
  );
};

const Diagram = () => {
  const state = useContext(AppState),
    {
      api: {
        process: {
          definition: { diagram },
        },
        task: { one: selected_task },
      },
    } = state;

  if (selected_task !== null) {
    void engine_rest.process_definition.diagram(state, selected_task.value.data.processDefinitionId);
  }

  return (
    <>
      <div id="diagram" />
      <RequestState
        signal={diagram}
        on_success={() => <BPMNViewer xml={diagram.value.data?.bpmn20Xml} container="diagram" />}
      />
    </>
  );
};

const Filter = () => {
  return (
    <div>
      <form>
        <h2>Filter Editor</h2>
        <h3>General</h3>
        <label>
          Name
          <input />
        </label>
        <label>
          Description
          <input />
        </label>
        <label>
          Color
          <input type="color" />
        </label>
        <label>
          Priority
          <input type="number" />
        </label>
        <label>
          Auto Referesh
          <input type="checkbox" />
        </label>

        <h3>Criteria</h3>
        <button>Add Criteria</button>
        <table>
          <thead>
            <tr>
              <th scope="column">Key</th>
              <th scope="column">Value</th>
              <th scope="column">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td></td>
              <td></td>
              <td>Remove</td>
            </tr>
          </tbody>
        </table>

        <h3>Permissions</h3>

        <label>
          Accessible by all users
          <input type="checkbox" />
        </label>

        <button>Add Permission</button>
        <table>
          <thead>
            <tr>
              <th scope="column">Group / User</th>
              <th scope="column">Identifier</th>
              <th scope="column">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td></td>
              <td></td>
              <td>Remove</td>
            </tr>
          </tbody>
        </table>

        <h3>Variables</h3>

        <p>The variables you set here will be shown in the task list view.</p>

        <label>
          Show undefined variables
          <input type="checkbox" />
        </label>

        <button>Add Variable</button>
        <table>
          <thead>
            <tr>
              <th scope="column">Name</th>
              <th scope="column">Label</th>
              <th scope="column">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td></td>
              <td></td>
              <td>Remove</td>
            </tr>
          </tbody>
        </table>
      </form>
    </div>
  );
};

const HistoryTab = () => {
  const state = useContext(AppState),
    {
      api: {
        history: { user_operation },
        task: { one },
      },
    } = state;

  void engine_rest.history.get_user_operation(state, one.value?.data?.executionId);

  return (
    <>
      <h2>History</h2>

      <table>
        <thead>
          <tr>
            <th>Date / Time</th>
            <th>User</th>
            <th>Action</th>
            <th>Type</th>
            <th>Value</th>
          </tr>
        </thead>
        <tbody>
          <RequestState signal={user_operation} on_success={() => <HistoryEntry />} />
        </tbody>
      </table>
    </>
  );
};

const HistoryEntry = () =>
  useContext(AppState).api.history.user_operation.value.data.map(
    ({ timestamp, userId, operationType, property, newValue }, index) => (
      <tr key={index}>
        <td>{timestamp}</td>
        <td>{userId}</td>
        <td>{operationType}</td>
        <td>{property}</td>
        <td>{newValue}</td>
      </tr>
    ),
  );

const task_tabs = [
  {
    name: "Form",
    id: "form",
    pos: 0,
    target: <TaskForm />,
  },
  {
    name: "History",
    id: "history",
    pos: 1,
    target: <HistoryTab />,
  },
  {
    name: "Diagram",
    id: "diagram",
    pos: 2,
    target: <Diagram />,
  },
];

export { TasksPage };
