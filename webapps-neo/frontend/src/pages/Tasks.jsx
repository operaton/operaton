import { useSignal } from "@preact/signals";
import { useLocation, useRoute } from "preact-iso";
import { useContext, useEffect, useLayoutEffect } from "preact/hooks";
import { useTranslation } from "react-i18next";

import engine_rest, {
  RequestState,
  RESPONSE_STATE,
} from "../api/engine_rest.jsx";
import * as Icons from "../assets/icons.jsx";
import { BPMNViewer } from "../components/BPMNViewer.jsx";
import { Tabs } from "../components/Tabs.jsx";
import { ListFilter } from "../components/ListFilter.jsx";
import { ManageFilters } from "../components/ManageFilters.jsx";
import {
  filter_share_link,
  parse_list_query,
  with_manage,
  without_manage,
  write_list_query,
} from "../helper/list_query.js";
import { AppState } from "../state.js";
import { StartProcessList } from "./StartProcessList.jsx";
import { TaskForm } from "../components/TaskForm.jsx";
import { formatRelativeDate } from "../helper/date_formatter.js";

const TASK_PAGE_SIZE = 20;

const SORT_OPTIONS = [
  { key: "priority", nameKey: "tasks.sort.priority" },
  { key: "dueDate", nameKey: "tasks.sort.due-date" },
  { key: "followUpDate", nameKey: "tasks.sort.follow-up-date" },
  { key: "name", nameKey: "tasks.sort.task-name" },
  { key: "assignee", nameKey: "tasks.sort.assignee" },
  { key: "processVariable", nameKey: "tasks.sort.process-variable" },
  { key: "executionVariable", nameKey: "tasks.sort.execution-variable" },
  { key: "taskVariable", nameKey: "tasks.sort.task-variable" },
  {
    key: "caseExecutionVariable",
    nameKey: "tasks.sort.case-execution-variable",
  },
  { key: "caseInstanceVariable", nameKey: "tasks.sort.case-instance-variable" },
];

const FILTER_KEYS = [
  { key: "assignee", nameKey: "tasks.filter_keys.assignee", type: "string" },
  {
    key: "assigneeLike",
    nameKey: "tasks.filter_keys.assigneeLike",
    type: "string",
  },
  {
    key: "candidateGroup",
    nameKey: "tasks.filter_keys.candidateGroup",
    type: "string",
  },
  {
    key: "candidateUser",
    nameKey: "tasks.filter_keys.candidateUser",
    type: "string",
  },
  {
    key: "involvedUser",
    nameKey: "tasks.filter_keys.involvedUser",
    type: "string",
  },
  {
    key: "unassigned",
    nameKey: "tasks.filter_keys.unassigned",
    type: "boolean",
  },
  {
    key: "processDefinitionKey",
    nameKey: "tasks.filter_keys.processDefinitionKey",
    type: "string",
  },
  {
    key: "processDefinitionName",
    nameKey: "tasks.filter_keys.processDefinitionName",
    type: "string",
  },
  {
    key: "processDefinitionNameLike",
    nameKey: "tasks.filter_keys.processDefinitionNameLike",
    type: "string",
  },
  {
    key: "processInstanceBusinessKey",
    nameKey: "tasks.filter_keys.processInstanceBusinessKey",
    type: "string",
  },
  {
    key: "processInstanceBusinessKeyLike",
    nameKey: "tasks.filter_keys.processInstanceBusinessKeyLike",
    type: "string",
  },
  {
    key: "taskDefinitionKey",
    nameKey: "tasks.filter_keys.taskDefinitionKey",
    type: "string",
  },
  {
    key: "taskDefinitionKeyLike",
    nameKey: "tasks.filter_keys.taskDefinitionKeyLike",
    type: "string",
  },
  { key: "name", nameKey: "tasks.filter_keys.name", type: "string" },
  { key: "nameLike", nameKey: "tasks.filter_keys.nameLike", type: "string" },
  {
    key: "description",
    nameKey: "tasks.filter_keys.description",
    type: "string",
  },
  {
    key: "descriptionLike",
    nameKey: "tasks.filter_keys.descriptionLike",
    type: "string",
  },
  { key: "priority", nameKey: "tasks.filter_keys.priority", type: "number" },
  { key: "dueBefore", nameKey: "tasks.filter_keys.dueBefore", type: "date" },
  { key: "dueAfter", nameKey: "tasks.filter_keys.dueAfter", type: "date" },
  {
    key: "followUpBefore",
    nameKey: "tasks.filter_keys.followUpBefore",
    type: "date",
  },
  {
    key: "followUpAfter",
    nameKey: "tasks.filter_keys.followUpAfter",
    type: "date",
  },
  {
    key: "createdBefore",
    nameKey: "tasks.filter_keys.createdBefore",
    type: "date",
  },
  {
    key: "createdAfter",
    nameKey: "tasks.filter_keys.createdAfter",
    type: "date",
  },
  { key: "active", nameKey: "tasks.filter_keys.active", type: "boolean" },
  { key: "suspended", nameKey: "tasks.filter_keys.suspended", type: "boolean" },
];

const is_saved_filter = (value) => value && value !== "all" && value !== "my";

const derive_query = (current_query, patch) => {
  const out = { ...current_query };
  if ("saved_filter_id" in patch) {
    if (patch.saved_filter_id == null || patch.saved_filter_id === "all")
      delete out.filter;
    else out.filter = patch.saved_filter_id;
  }
  if ("sortBy" in patch && patch.sortBy != null) out.sortBy = patch.sortBy;
  if ("sortOrder" in patch && patch.sortOrder != null)
    out.sortOrder = patch.sortOrder;
  return out;
};

const load_tasks = (state, query, firstResult = 0) => {
  const filterValue = query?.filter,
    sortBy = query?.sortBy ?? "name",
    sortOrder = query?.sortOrder ?? "asc",
    sorting = { sortBy, sortOrder };
  if (is_saved_filter(filterValue)) {
    void engine_rest.filter.execute_filter(
      state,
      filterValue,
      firstResult,
      TASK_PAGE_SIZE,
      sorting,
    );
  } else {
    const filter =
      filterValue === "my"
        ? { assignee: state.api.user.profile.value?.id }
        : {};
    void engine_rest.task.get_tasks(
      state,
      sortBy,
      sortOrder,
      firstResult,
      TASK_PAGE_SIZE,
      filter,
    );
  }
};

const TasksPage = () => {
  const state = useContext(AppState);
  const { params, query } = useRoute();

  useEffect(() => {
    if (state.api.filter.list.value === null) {
      void engine_rest.filter.get_filters(state);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    load_tasks(state, query);
    // Re-load when filter/sort change. `query` keys we care about: filter,
    // sortBy, sortOrder. JSON-stringify is the simplest stable dep here.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query.filter, query.sortBy, query.sortOrder]);

  if (params?.task_id === "start") {
    return (
      <main id="content" class="start-process-page fade-in">
        <StartProcessList />
      </main>
    );
  }

  if (params?.task_id === "filter") {
    return (
      <main id="content" class="fade-in">
        <Filter />
      </main>
    );
  }

  if (query?.filters === "manage") {
    return (
      <main id="content" class="fade-in">
        <TasksManage />
      </main>
    );
  }

  return (
    <main id="content" class="tasks fade-in">
      <TaskList />
      {params?.task_id === undefined ? <NoSelectedTask /> : <Task />}
    </main>
  );
};

const TasksManage = () => {
  const state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation();

  useEffect(() => {
    if (state.api.filter.list.value === null) {
      void engine_rest.filter.get_filters(state);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const refresh = () => engine_rest.filter.get_filters(state);
  const on_save = (filter) => {
    const body = {
      resourceType: "Task",
      name: filter.name,
      owner: state.api.user.profile.value?.id,
      query: filter.query,
      properties: {},
    };
    const result = engine_rest.filter.create_filter(state, body);
    if (result && typeof result.then === "function") result.then(refresh);
    else refresh();
  };
  const on_update = (id, filter) => {
    const body = {
      resourceType: "Task",
      name: filter.name,
      owner: state.api.user.profile.value?.id,
      query: filter.query,
      properties: {},
    };
    const result = engine_rest.filter.update_filter(state, id, body);
    if (result && typeof result.then === "function") result.then(refresh);
    else refresh();
  };
  const on_delete = (id) => {
    const result = engine_rest.filter.delete_filter(state, id);
    if (result && typeof result.then === "function") result.then(refresh);
    else refresh();
  };

  return (
    <ManageFilters
      title={t("tasks.filter.manage_title")}
      saved_filters_signal={state.api.filter.list}
      filter_keys={FILTER_KEYS}
      sort_options={SORT_OPTIONS}
      on_save={on_save}
      on_update={on_update}
      on_delete={on_delete}
      on_close={() => route(without_manage(), true)}
      build_share_link={(f) => filter_share_link(window.location.href, f)}
      advanced_editor_href="/tasks/filter"
    />
  );
};

const TaskList = () => {
  const state = useContext(AppState),
    taskList = state.api.task.list,
    { params, query } = useRoute(),
    { route } = useLocation(),
    selectedTaskId = params.task_id,
    [t] = useTranslation(),
    load_more = () => {
      const current = taskList.value?.data?.length ?? 0;
      load_tasks(state, query, current);
    },
    apply_patch = (patch) => {
      const next_pathname = write_list_query(window.location.href, patch);
      route(next_pathname, true);
      load_tasks(state, derive_query(query, patch));
    },
    open_manage = () => route(with_manage(), false);

  const list_current = {
    saved_filter_id: query?.filter ?? null,
    sortBy: query?.sortBy ?? "name",
    sortOrder: query?.sortOrder ?? "asc",
    criteria: parse_list_query(query).criteria,
  };

  return (
    <div id="task-list">
      <h2 class="screen-hidden">{t("tasks.title")}</h2>
      <ListFilter
        sort_options={SORT_OPTIONS}
        saved_filters_signal={state.api.filter.list}
        filter_predicate={(f) =>
          f && f.id && Object.keys(f.query ?? {}).length > 0
        }
        current={list_current}
        defaults={{ sortBy: "name", sortOrder: "asc" }}
        include_my_filter
        on_change={apply_patch}
        on_manage={open_manage}
      />
      <div id="task-table-wrapper">
        <table>
          <thead>
            <tr>
              <th scope="col">{t("tasks.task-list.table-headings.task-name")}</th>
              <th scope="col">{t("tasks.task-list.table-headings.assignee")}</th>
              <th scope="col">{t("tasks.task-list.table-headings.due-in")}</th>
            </tr>
          </thead>
          <tbody>
            <RequestState
              signal={taskList}
              on_success={() =>
                taskList.value?.data?.map((task) => (
                  <TaskRowEntry
                    key={task.id}
                    task={task}
                    selected={task.id === selectedTaskId}
                  />
                ))
              }
            />
          </tbody>
        </table>
        {taskList.value?.hasMore === true ? (
          <button class="load-more" onClick={load_more}>
            {t("tasks.load-more")}
          </button>
        ) : taskList.value?.hasMore === false ? (
          <small class="load-more-end">{t("tasks.no-more-items")}</small>
        ) : null}
      </div>
      <a href="/tasks/start" class="button start-process">
        {t("tasks.start-process-label")}
      </a>
    </div>
  );
};

const TaskRowEntry = ({ task, selected }) => {
  const { id, name, due, assignee } = task;

  useLayoutEffect(() => {
    if (selected) {
      document
        .getElementById(id)
        .scrollIntoView({ behavior: "instant", block: "center" });
    }
  });

  return (
    <tr id={id} key={id} aria-selected={selected}>
      <th scope="row">
        <a href={`/tasks/${id}/${task_tabs[0].id}`}>{name}</a>
      </th>
      <td>{assignee ? assignee : "—"}</td>
      <td>
        {due ? <time datetime={due}>{formatRelativeDate(due)}</time> : "—"}
      </td>
    </tr>
  );
};

const NoSelectedTask = () => {
  const [t] = useTranslation();
  return (
    <div id="task-details" className="fade-in">
      <div class="task-empty">{t("tasks.select-task")}</div>
    </div>
  );
};

// when something has changed (e.g. assignee) in the task we have to update the task list
const Task = () => {
  const state = useContext(AppState),
    [t] = useTranslation(),
    {
      api: {
        task: { one: task },
        process: {
          definition: { one: pd },
        },
      },
    } = state;

  const task_value = task.value;
  const task_data = task_value?.data;
  const is_error =
    task_value?.status === RESPONSE_STATE.ERROR && task_data === undefined;

  if (is_error) {
    const status = task_value.error?.status;
    return (
      <div id="task-details" className="fade-in">
        <div class="task-empty">
          <h2>
            {status === 404
              ? t("tasks.task-not-found")
              : t("tasks.task-load-failed")}
          </h2>
          <p>
            {status === 404
              ? t("tasks.task-not-found-hint")
              : (task_value.error?.message ?? t("tasks.form.unknown-error"))}
          </p>
          <a href="/tasks" class="button">
            {t("tasks.back-to-list")}
          </a>
        </div>
      </div>
    );
  }

  return (
    <div id="task-details" className="fade-in">
      <section id="task-data">
        <header>
          <div>
            <h2>{task.value?.data?.name}</h2>
            <a href={`/processes/${pd.value?.data?.id}`}>
              {pd.value?.data?.name} ({t("processes.version")}{" "}
              {pd.value?.data?.version})
            </a>
            {state.api.task.one.value?.data !== undefined ? (
              <p>{state.api.task.one.value?.data.description}</p>
            ) : (
              <p>{t("tasks.no-description")}</p>
            )}
          </div>
          <CommentButton />
        </header>

        <div class="task-cards">
          <SetFollowUpDateButton />
          <SetDueDateButton />
          <ClaimButton />
          <SetGroupsButton />
        </div>
      </section>
      <div>
        <hr />
      </div>

      <TaskTabs />
    </div>
  );
};

const load_task_chain = async (state, task_id) => {
  await engine_rest.task.get_task(state, task_id);
  const task = state.api.task.one.value?.data;
  if (!task?.id) {
    // Task no longer exists (completed, deleted, or wrong id) — stop here so we
    // don't feed `undefined` into downstream URLs.
    return;
  }
  await engine_rest.process_definition.one(state, task.processDefinitionId);
  await engine_rest.task.get_identity_links(state, task.id);
  await engine_rest.history.get_user_operation(state, task.executionId);
  await engine_rest.task.get_comments(state, task.id);
};

const TaskTabs = () => {
  const state = useContext(AppState);
  const { params } = useRoute();
  const [t] = useTranslation();

  // Load the task whenever the active task changes. Clean stale per-task data
  // on unmount so the next task's panes don't render against the previous
  // task's signals.
  useEffect(() => {
    void load_task_chain(state, params.task_id);
    return () => {
      state.api.task.one.value = null;
      state.api.task.comment.list.value = null;
      state.api.task.identity_links.value = null;
      state.api.history.user_operation.value = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.task_id]);

  return (
    <section className="task-tabs">
      {state.api.task.one.value?.data != null ? (
        <Tabs
          tabs={task_tabs}
          base_url={`/tasks/${state.api.task.one.value.data.id}`}
          className="fade-in"
          label={t("tasks.tabs.label")}
        />
      ) : (
        t("common.loading")
      )}
    </section>
  );
};

const SetDueDateButton = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    {
      api: {
        task: { one: task },
      },
    } = state,
    close = () => document.getElementById("set_due_date").close(),
    show = () => document.getElementById("set_due_date").showModal(),
    due_date = task.value?.data?.due
      ? new Date(Date.parse(task.value?.data?.due))
      : null,
    date_state = useSignal({
      date:
        due_date !== null
          ? due_date?.toISOString().split("T")[0]
          : new Date().toISOString().split("T")[0],
      time:
        due_date !== null
          ? due_date?.toISOString().split("T")[1].substring(0, 5)
          : new Date().toISOString().split("T")[1].substring(0, 5),
    }),
    submit = (event) => {
      event.preventDefault();
      engine_rest.task
        .update_task(
          state,
          {
            due: `${date_state.value.date}T${date_state.value.time}:0.000+0000`,
          },
          params.task_id,
        )
        .then(() => {
          close();
        });
    };

  return (
    <>
      <button type="button" onClick={show} class="task-card">
        <small>{t("tasks.due-date.label")}</small>
        <span>{due_date !== null ? due_date.toLocaleString() : "—"}</span>
        <Icons.pencil />
      </button>

      <dialog id="set_due_date" aria-labelledby="set-due-date-title">
        <button onClick={close}>{t("common.close")}</button>
        <h2 id="set-due-date-title">{t("tasks.due-date.title")}</h2>

        <form onSubmit={submit}>
          <label for="due-date">{t("tasks.due-date.date")}</label>
          <input
            type="date"
            id="due-date"
            value={
              due_date !== null ? due_date?.toISOString().split("T")[0] : null
            }
            onInput={(e) =>
              (date_state.value = {
                ...date_state.peek(),
                date: e.currentTarget.value,
              })
            }
          />
          <label for="due-time">{t("tasks.due-date.time")}</label>
          <input
            type="time"
            id="due-time"
            value={
              due_date !== null
                ? due_date?.toISOString().split("T")[1].substring(0, 5)
                : null
            }
            onInput={(e) =>
              (date_state.value = {
                ...date_state.peek(),
                time: e.currentTarget.value,
              })
            }
          />
          <div class="button-group">
            <button type="submit">{t("common.submit")}</button>
          </div>
        </form>
      </dialog>
    </>
  );
};

const SetFollowUpDateButton = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    {
      api: {
        task: { one: task },
      },
    } = state,
    close = () => document.getElementById("set_follow_up_date").close(),
    show = () => document.getElementById("set_follow_up_date").showModal(),
    followUpDate = task.value?.data?.followUp
      ? new Date(Date.parse(task.value?.data?.followUp))
      : null,
    date_state = useSignal({
      date:
        followUpDate !== null
          ? followUpDate?.toISOString().split("T")[0]
          : new Date().toISOString().split("T")[0],
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
          {
            followUp: `${date_state.value.date}T${date_state.value.time}:0.000+0000`,
          },
          params.task_id,
        )
        .then(() => {
          close();
        });
    };

  return (
    <>
      <button type="button" onClick={show} class="task-card">
        <small>{t("tasks.follow-up.label")}</small>
        <span>
          {followUpDate !== null ? followUpDate.toLocaleString() : "—"}
        </span>
        <Icons.pencil />
      </button>

      <dialog
        id="set_follow_up_date"
        aria-labelledby="set-follow-up-date-title"
      >
        <button onClick={close}>{t("common.close")}</button>
        <h2 id="set-follow-up-date-title">{t("tasks.follow-up.title")}</h2>

        <form onSubmit={submit}>
          <label for="follow-up-date">{t("tasks.due-date.date")}</label>
          <input
            type="date"
            id="follow-up-date"
            value={
              followUpDate !== null
                ? followUpDate?.toISOString().split("T")[0]
                : null
            }
            onInput={(e) =>
              (date_state.value = {
                ...date_state.peek(),
                date: e.currentTarget.value,
              })
            }
          />
          <label for="follow-up-time">{t("tasks.due-date.time")}</label>
          <input
            type="time"
            id="follow-up-time"
            value={
              followUpDate !== null
                ? followUpDate?.toISOString().split("T")[1].substring(0, 5)
                : null
            }
            onInput={(e) =>
              (date_state.value = {
                ...date_state.peek(),
                time: e.currentTarget.value,
              })
            }
          />
          <div class="button-group">
            <button type="submit">{t("common.submit")}</button>
          </div>
        </form>
      </dialog>
    </>
  );
};

const GroupsList = () => {
  const state = useContext(AppState),
    links = state.api.task.identity_links.value?.data;
  if (!links) return "—";
  const groups = links
    .filter((l) => l.type === "candidate" && l.groupId)
    .map((l) => l.groupId);
  return groups.length > 0 ? groups.join(", ") : "—";
};

const SetGroupsButton = () => {
  const state = useContext(AppState),
    [t] = useTranslation(),
    close = () => document.getElementById("add_groups").close(),
    show = () => document.getElementById("add_groups").showModal(),
    group_state = useSignal(null),
    submit = (event) => {
      event.preventDefault();
      engine_rest.task
        .add_group(state, state.api.task.one.value.data.id, group_state.value)
        .then(() => {
          if (
            state.api.task.add_group.value.status === RESPONSE_STATE.SUCCESS
          ) {
            group_state.value = "";
          }
        });
    },
    delete_group = (group_id) =>
      engine_rest.task.delete_group(
        state,
        state.api.task.one.value.data.id,
        group_id,
      );

  return (
    <>
      <button type="button" onClick={show} class="task-card">
        <small>{t("tasks.groups.set")}</small>
        <span>
          <GroupsList />
        </span>
        <Icons.pencil />
      </button>

      <dialog id="add_groups" aria-labelledby="add-groups-title">
        <header>
          <h2 id="add-groups-title">{t("tasks.groups.manage")}</h2>
          <button
            type="button"
            onClick={close}
            class="neutral"
            aria-label={t("common.close")}
          >
            <Icons.close />
          </button>
        </header>

        <h3>{t("tasks.groups.add")}</h3>
        <form onSubmit={submit}>
          <label for="group_id">{t("tasks.groups.group-id")}</label>
          <input
            id="group_id"
            key="group_id"
            required
            onInput={(e) => (group_state.value = e.currentTarget.value)}
          />
          <div class="button-group">
            <button type="submit">{t("tasks.groups.add-group")}</button>
          </div>
        </form>

        <h3>{t("tasks.groups.remove-groups")}</h3>

        <RequestState
          signal={state.api.task.identity_links}
          on_success={() => (
            <table>
              <thead>
                <tr>
                  <th>{t("tasks.groups.group-id")}</th>
                  <th>{t("common.action")}</th>
                </tr>
              </thead>
              <tbody>
                {state.api.task.identity_links.value.data.map(
                  ({ groupId, type }, index) =>
                    type === "candidate" ? (
                      <tr key={index}>
                        <td>{groupId}</td>
                        <td>
                          <button onClick={() => delete_group(groupId)}>
                            {t("common.delete")}
                          </button>
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

const CommentButton = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    close = () => document.getElementById("add_comment").close(),
    show = () => document.getElementById("add_comment").showModal(),
    message = useSignal(""),
    submit = (event) => {
      event.preventDefault();
      engine_rest.task
        .create_comment(state, params.task_id, message.value)
        .then(() => {
          message.value = "";
          engine_rest.task.get_comments(state, params.task_id);
          close();
        });
    };

  return (
    <>
      <button type="button" onClick={show}>
        {t("tasks.comment-add")}
      </button>

      <dialog id="add_comment" aria-labelledby="add-comment-title">
        <button onClick={close}>{t("common.close")}</button>
        <h2 id="add-comment-title">{t("tasks.comment")}</h2>

        <form onSubmit={submit}>
          <label for="comment_message">{t("tasks.comment-message")}</label>
          <textarea
            id="comment_message"
            required
            value={message.value}
            onInput={(e) => (message.value = e.currentTarget.value)}
          />
          <div class="button-group">
            <button type="submit">{t("common.submit")}</button>
          </div>
        </form>
      </dialog>
    </>
  );
};

const ClaimButton = () => {
  const state = useContext(AppState),
    [t] = useTranslation(),
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
          <button type="button" onClick={show} class="task-card">
            <small>{t("tasks.task-list.table-headings.assignee")}</small>
            <span>{task?.assignee ?? "—"}</span>
            <Icons.pencil />
          </button>

          <dialog id="set_assignee" aria-label={t("tasks.assignee-menu")}>
            <button type="button" onClick={close}>
              {t("common.close")}
            </button>
            {assignee_is_different && !assigned ? (
              <button
                type="button"
                onClick={() =>
                  engine_rest.task.assign_task(state, null, task.id)
                }
                class="secondary"
              >
                <Icons.user_minus /> {t("tasks.reset-assignee")}
              </button>
            ) : (user_is_assignee || claimed) && !unclaimed ? (
              <button
                type="button"
                onClick={() => engine_rest.task.unclaim_task(state, task.id)}
                class="secondary"
              >
                <Icons.user_minus /> {t("tasks.unclaim")}
              </button>
            ) : (
              <button
                type="button"
                onClick={() => engine_rest.task.claim_task(state, task.id)}
                class="secondary"
              >
                <Icons.user_plus /> {t("tasks.claim")}
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

  const process_definition_id = selected_task.value?.data?.processDefinitionId;
  useEffect(() => {
    if (process_definition_id) {
      void engine_rest.process_definition.diagram(state, process_definition_id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [process_definition_id]);

  return (
    <>
      <div id="diagram" />
      <RequestState
        signal={diagram}
        on_success={() => (
          <BPMNViewer
            xml={diagram.value.data?.bpmn20Xml}
            container="diagram"
            highlight={[selected_task.value?.data?.taskDefinitionKey]}
          />
        )}
      />
    </>
  );
};

const CRITERIA_KEYS = [
  "assignee",
  "assigneeLike",
  "candidateGroup",
  "candidateUser",
  "involvedUser",
  "unassigned",
  "processDefinitionKey",
  "processDefinitionName",
  "processDefinitionNameLike",
  "processInstanceBusinessKey",
  "processInstanceBusinessKeyLike",
  "taskDefinitionKey",
  "taskDefinitionKeyLike",
  "name",
  "nameLike",
  "description",
  "descriptionLike",
  "priority",
  "dueBefore",
  "dueAfter",
  "followUpBefore",
  "followUpAfter",
  "createdBefore",
  "createdAfter",
  "active",
  "suspended",
];

const Filter = () => {
  const state = useContext(AppState),
    [t] = useTranslation(),
    { route } = useLocation(),
    form = useSignal({
      name: "",
      description: "",
      color: "#000000",
      priority: 0,
      refresh: false,
      criteria: [],
      variables: [],
    }),
    update = (key, value) => (form.value = { ...form.peek(), [key]: value }),
    add_criteria = () =>
      update("criteria", [
        ...form.peek().criteria,
        { key: CRITERIA_KEYS[0], value: "" },
      ]),
    remove_criteria = (index) =>
      update(
        "criteria",
        form.peek().criteria.filter((_, i) => i !== index),
      ),
    update_criteria = (index, field, value) =>
      update(
        "criteria",
        form
          .peek()
          .criteria.map((c, i) => (i === index ? { ...c, [field]: value } : c)),
      ),
    add_variable = () =>
      update("variables", [...form.peek().variables, { name: "", label: "" }]),
    remove_variable = (index) =>
      update(
        "variables",
        form.peek().variables.filter((_, i) => i !== index),
      ),
    update_variable = (index, field, value) =>
      update(
        "variables",
        form
          .peek()
          .variables.map((v, i) =>
            i === index ? { ...v, [field]: value } : v,
          ),
      ),
    submit = (event) => {
      event.preventDefault();
      const {
          name,
          description,
          color,
          priority,
          refresh,
          criteria,
          variables,
        } = form.value,
        query = {};
      criteria.forEach(({ key, value }) => {
        if (key === "unassigned" || key === "active" || key === "suspended")
          query[key] = true;
        else if (key === "priority") query[key] = parseInt(value, 10);
        else if (value) query[key] = value;
      });
      const body = {
        resourceType: "Task",
        name,
        owner: state.api.user.profile.value?.id,
        query,
        properties: {
          description,
          color,
          priority: parseInt(priority, 10) || 0,
          refresh,
          variables,
        },
      };
      engine_rest.filter.create_filter(state, body).then(() => {
        route("/tasks");
      });
    };

  return (
    <div class="filter-editor">
      <header>
        <h2>{t("tasks.filter.title")}</h2>
        <a href="/tasks" class="button">
          {t("common.back")}
        </a>
      </header>

      <form onSubmit={submit}>
        <fieldset>
          <legend>{t("tasks.filter.general")}</legend>
          <div class="filter-fields">
            <label for="filter-name">{t("common.name")}</label>
            <input
              id="filter-name"
              required
              value={form.value.name}
              onInput={(e) => update("name", e.currentTarget.value)}
            />
            <label for="filter-description">
              {t("tasks.filter.description")}
            </label>
            <input
              id="filter-description"
              value={form.value.description}
              onInput={(e) => update("description", e.currentTarget.value)}
            />
            <label for="filter-priority">
              {t("tasks.task-list.table-headings.priority")}
            </label>
            <input
              id="filter-priority"
              type="number"
              value={form.value.priority}
              onInput={(e) => update("priority", e.currentTarget.value)}
            />
            <label for="filter-color">{t("tasks.filter.color")}</label>
            <input
              id="filter-color"
              type="color"
              value={form.value.color}
              onInput={(e) => update("color", e.currentTarget.value)}
            />
            <label class="filter-checkbox" for="filter-auto-refresh">
              <input
                id="filter-auto-refresh"
                type="checkbox"
                checked={form.value.refresh}
                onInput={(e) => update("refresh", e.currentTarget.checked)}
              />
              {t("tasks.filter.auto-refresh")}
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend>{t("tasks.filter.criteria")}</legend>
          {form.value.criteria.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>{t("common.key")}</th>
                  <th>{t("common.value")}</th>
                  <th>{t("common.action")}</th>
                </tr>
              </thead>
              <tbody>
                {form.value.criteria.map((criterion, i) => (
                  <tr key={i}>
                    <td>
                      <select
                        value={criterion.key}
                        onChange={(e) =>
                          update_criteria(i, "key", e.currentTarget.value)
                        }
                      >
                        {CRITERIA_KEYS.map((k) => (
                          <option key={k} value={k}>
                            {k}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      {criterion.key === "unassigned" ||
                      criterion.key === "active" ||
                      criterion.key === "suspended" ? (
                        <em>{t("common.yes")}</em>
                      ) : (
                        <input
                          value={criterion.value}
                          onInput={(e) =>
                            update_criteria(i, "value", e.currentTarget.value)
                          }
                        />
                      )}
                    </td>
                    <td>
                      <button type="button" onClick={() => remove_criteria(i)}>
                        {t("common.remove")}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <button type="button" onClick={add_criteria}>
            {t("tasks.filter.add-criteria")}
          </button>
        </fieldset>

        <fieldset>
          <legend>{t("tasks.filter.variables")}</legend>
          <p>{t("tasks.filter.variables-hint")}</p>
          {form.value.variables.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>{t("common.name")}</th>
                  <th>{t("tasks.filter.label")}</th>
                  <th>{t("common.action")}</th>
                </tr>
              </thead>
              <tbody>
                {form.value.variables.map((variable, i) => (
                  <tr key={i}>
                    <td>
                      <input
                        value={variable.name}
                        onInput={(e) =>
                          update_variable(i, "name", e.currentTarget.value)
                        }
                      />
                    </td>
                    <td>
                      <input
                        value={variable.label}
                        onInput={(e) =>
                          update_variable(i, "label", e.currentTarget.value)
                        }
                      />
                    </td>
                    <td>
                      <button type="button" onClick={() => remove_variable(i)}>
                        {t("common.remove")}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <button type="button" onClick={add_variable}>
            {t("tasks.filter.add-variable")}
          </button>
        </fieldset>

        <div class="filter-actions">
          <button type="submit">{t("common.save")}</button>
          <a href="/tasks">{t("common.cancel")}</a>
        </div>
      </form>
    </div>
  );
};

const merge_history = (...sources) =>
  sources
    .flatMap((s) => s())
    .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

const history_from_operations = (signal) =>
  (signal.value?.data ?? []).map((op) => ({
    timestamp: op.timestamp,
    user: op.userId,
    type: op.operationType,
    detail: [op.property, op.newValue].filter(Boolean).join(": "),
  }));

const history_from_comments = (signal) =>
  (signal.value?.data ?? []).map((c) => ({
    timestamp: c.time,
    user: c.userId,
    type: "Comment",
    detail: c.message,
  }));

const HistoryTab = () => {
  const state = useContext(AppState),
    [t] = useTranslation(),
    {
      api: {
        history: { user_operation },
        task: { comment },
      },
    } = state;

  const ready =
    user_operation.value?.status === RESPONSE_STATE.SUCCESS &&
    comment.list.value?.status === RESPONSE_STATE.SUCCESS;

  const entries = ready
    ? merge_history(
        () => history_from_operations(user_operation),
        () => history_from_comments(comment.list),
      )
    : [];

  return (
    <>
      <h2>{t("tasks.history.title")}</h2>

      <table>
        <thead>
          <tr>
            <th>{t("tasks.history.date-time")}</th>
            <th>{t("tasks.history.user")}</th>
            <th>{t("common.type")}</th>
            <th>{t("tasks.history.detail")}</th>
          </tr>
        </thead>
        <tbody>
          {ready ? (
            entries.map((entry, i) => (
              <tr key={i}>
                <td>
                  <time datetime={entry.timestamp}>
                    {formatRelativeDate(entry.timestamp)}
                  </time>
                </td>
                <td>{entry.user}</td>
                <td>{entry.type}</td>
                <td>{entry.detail}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colspan="4">{t("common.loading")}</td>
            </tr>
          )}
        </tbody>
      </table>
    </>
  );
};

const AttachmentsTab = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    name = useSignal(""),
    description = useSignal(""),
    file = useSignal(null);

  const load = () =>
    void engine_rest.task.get_attachments(state, params.task_id);

  useEffect(() => {
    load();
    return () => {
      state.api.task.attachment.list.value = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.task_id]);

  const submit = async (event) => {
    event.preventDefault();
    if (!file.value) return;
    const fd = new FormData();
    fd.append("attachment-name", name.value || file.value.name);
    fd.append("attachment-description", description.value);
    fd.append("attachment-type", file.value.type || "application/octet-stream");
    fd.append("content", file.value);
    await engine_rest.task.create_attachment(state, params.task_id, fd);
    name.value = "";
    description.value = "";
    file.value = null;
    load();
  };

  const remove = async (id) => {
    await engine_rest.task.delete_attachment(state, params.task_id, id);
    load();
  };

  return (
    <div class="task-attachments">
      <RequestState
        signal={state.api.task.attachment.list}
        on_success={() => {
          const rows = state.api.task.attachment.list.value?.data ?? [];
          if (rows.length === 0)
            return <p class="info-box">{t("tasks.attachments.empty")}</p>;
          return (
            <table>
              <thead>
                <tr>
                  <th>{t("common.name")}</th>
                  <th>{t("tasks.attachments.description")}</th>
                  <th>{t("common.action")}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((a) => (
                  <tr key={a.id}>
                    <td>
                      <a
                        href={engine_rest.task.attachment_url(
                          state,
                          params.task_id,
                          a.id,
                        )}
                        target="_blank"
                        rel="noreferrer"
                      >
                        {a.name ?? a.id}
                      </a>
                    </td>
                    <td>{a.description}</td>
                    <td>
                      <button
                        type="button"
                        class="danger"
                        onClick={() => remove(a.id)}
                      >
                        {t("common.delete")}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          );
        }}
      />
      <form onSubmit={submit}>
        <h3>{t("tasks.attachments.add")}</h3>
        <div class="dialog-fields">
          <label>
            {t("common.name")}
            <input
              type="text"
              value={name.value}
              onInput={(e) => (name.value = e.currentTarget.value)}
            />
          </label>
          <label>
            {t("tasks.attachments.description")}
            <input
              type="text"
              value={description.value}
              onInput={(e) => (description.value = e.currentTarget.value)}
            />
          </label>
          <label>
            {t("tasks.attachments.file")}
            <input
              type="file"
              onChange={(e) => (file.value = e.currentTarget.files[0])}
            />
          </label>
        </div>
        <div class="button-group">
          <button type="submit" disabled={!file.value}>
            {t("tasks.attachments.upload")}
          </button>
        </div>
      </form>
    </div>
  );
};

const task_tabs = [
  {
    nameKey: "tasks.tabs.form",
    id: "form",
    pos: 0,
    Component: TaskForm,
  },
  {
    nameKey: "tasks.tabs.history",
    id: "history",
    pos: 1,
    Component: HistoryTab,
  },
  {
    nameKey: "tasks.tabs.attachments",
    id: "attachments",
    pos: 2,
    Component: AttachmentsTab,
  },
  {
    nameKey: "tasks.tabs.diagram",
    id: "diagram",
    pos: 3,
    Component: Diagram,
  },
];

export { TasksPage };
