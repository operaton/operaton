import { useSignal } from "@preact/signals";
import { useLocation, useRoute } from "preact-iso";
import { useContext, useEffect, useLayoutEffect, useRef } from "preact/hooks";
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
  keep_list_query,
} from "../helper/list_query.js";
import { resolve_user } from "../api/helper.jsx";
import { AppState } from "../state.js";
import {
  VARIABLE_TYPES,
  coerce_variable_value,
  format_variable_value,
} from "../helper/variables.js";
import { StartProcessList } from "./StartProcessList.jsx";
import { ConfirmDialog } from "../components/Dialog.jsx";
import { TaskForm } from "../components/TaskForm.jsx";
import {
  formatAbsolute,
  formatRelativeDate,
  formatTimestamp,
  fromLocalParts,
  toLocalParts,
} from "../helper/date_formatter.js";

const TASK_PAGE_SIZE = 20;

// Sorting by a variable is deliberately absent. The engine needs the variable's
// name and type alongside the key, which neither the query string nor this
// interface carries: GET refuses the key outright, and a request without those
// two answers "variableName is null". Offering the choice only meant an error
// whichever way it was taken. The five keys come back with the data path that
// can express them.
const SORT_OPTIONS = [
  { key: "priority", nameKey: "tasks.sort.priority" },
  { key: "dueDate", nameKey: "tasks.sort.due-date" },
  { key: "followUpDate", nameKey: "tasks.sort.follow-up-date" },
  { key: "name", nameKey: "tasks.sort.task-name" },
  { key: "assignee", nameKey: "tasks.sort.assignee" },
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

const load_tasks = (
  state,
  query,
  firstResult = 0,
  pageSize = TASK_PAGE_SIZE,
) => {
  const filterValue = query?.filter,
    sortBy = query?.sortBy ?? "name",
    sortOrder = query?.sortOrder ?? "asc",
    sorting = { sortBy, sortOrder };
  if (is_saved_filter(filterValue)) {
    void engine_rest.filter.execute_filter(
      state,
      filterValue,
      firstResult,
      pageSize,
      sorting,
    );
  } else {
    const filter =
      filterValue === "my" ? { assignee: resolve_user(state) } : {};
    void engine_rest.task.get_tasks(
      state,
      sortBy,
      sortOrder,
      firstResult,
      pageSize,
      filter,
    );
  }
};

// Reload the list from the top, keeping however many entries are already shown.
// A plain reload starts at firstResult 0, which replaces the list and would undo
// "load more"; asking for as many as are on screen refreshes all of them at once.
const reload_tasks = (state, query) => {
  const shown = state.api.task.list.value?.data?.length ?? 0;
  load_tasks(state, query, 0, Math.max(shown, TASK_PAGE_SIZE));
};

const TasksPage = () => {
  const state = useContext(AppState);
  const { params, query } = useRoute();
  const open_task_id = useRef(undefined);

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

  useEffect(() => {
    // Completing a task routes back to /tasks, and so does navigating back by
    // hand. Either way the list still holds what it fetched before, including
    // the task that is now gone. Reload it on the way back — never on the way
    // in, so opening a task costs no extra request.
    const returned_to_list =
      open_task_id.current !== undefined && params.task_id === undefined;
    open_task_id.current = params.task_id;
    if (returned_to_list) reload_tasks(state, query);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.task_id]);

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

// A saved filter is private until someone is authorized to read it. The engine
// expresses that as authorizations on the Filter resource: a global grant for
// everyone (userId "*", type 0) or a grant per user or group (type 1).
const FILTER_RESOURCE_TYPE = 5;

const grant_access = async (state, filter_id, { readable_by_all, grants }) => {
  if (readable_by_all) {
    await engine_rest.authorization.create(state, {
      type: 0,
      permissions: ["READ"],
      userId: "*",
      resourceType: FILTER_RESOURCE_TYPE,
      resourceId: filter_id,
    });
  }
  for (const grant of grants) {
    const id = grant.id.trim();
    if (!id) continue;
    await engine_rest.authorization.create(state, {
      type: 1,
      permissions: ["READ"],
      [grant.type === "group" ? "groupId" : "userId"]: id,
      resourceType: FILTER_RESOURCE_TYPE,
      resourceId: filter_id,
    });
  }
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
      owner: resolve_user(state),
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
      owner: resolve_user(state),
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

// A task that belongs to no process — a follow-up, a reminder, something the
// model does not cover. The engine answers 204 without a body, so the id is
// chosen here; otherwise the new task could not be opened afterwards.
const CreateTaskButton = () => {
  const state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation(),
    name = useSignal(""),
    assignee = useSignal(""),
    description = useSignal(""),
    tenant = useSignal(""),
    close = () => document.getElementById("create_task").close(),
    show = () => {
      void engine_rest.tenant.by_member(state, null, true);
      document.getElementById("create_task").showModal();
    },
    tenants = state.api.tenant.by_member.value?.data ?? [],
    submit = async (event) => {
      event.preventDefault();
      const id = crypto.randomUUID();
      const result = await engine_rest.task.create_task(state, {
        id,
        name: name.value.trim(),
        assignee: assignee.value.trim() || null,
        description: description.value.trim() || null,
        tenantId: tenant.value || null,
      });
      if (result?.status !== RESPONSE_STATE.SUCCESS) return;
      name.value = "";
      assignee.value = "";
      description.value = "";
      tenant.value = "";
      close();
      route(`/tasks/${id}/form`);
    };

  return (
    <>
      <button type="button" class="button create-task" onClick={show}>
        {t("tasks.create.open")}
      </button>

      <dialog id="create_task" aria-labelledby="create-task-title">
        <button type="button" onClick={close}>
          {t("common.close")}
        </button>
        <h2 id="create-task-title">{t("tasks.create.title")}</h2>
        <form onSubmit={submit}>
          <label for="new-task-name">{t("common.name")}</label>
          <input
            id="new-task-name"
            type="text"
            required
            value={name.value}
            onInput={(e) => (name.value = e.currentTarget.value)}
          />
          <label for="new-task-assignee">
            {t("tasks.task-list.table-headings.assignee")}
          </label>
          <input
            id="new-task-assignee"
            type="text"
            value={assignee.value}
            onInput={(e) => (assignee.value = e.currentTarget.value)}
          />
          <label for="new-task-description">
            {t("tasks.attachments.description")}
          </label>
          <input
            id="new-task-description"
            type="text"
            value={description.value}
            onInput={(e) => (description.value = e.currentTarget.value)}
          />
          {tenants.length > 1 && (
            <>
              <label for="new-task-tenant">{t("tasks.tenant")}</label>
              <select
                id="new-task-tenant"
                value={tenant.value}
                onChange={(e) => (tenant.value = e.currentTarget.value)}
              >
                <option value="">—</option>
                {tenants.map((x) => (
                  <option key={x.id} value={x.id}>
                    {x.name ?? x.id}
                  </option>
                ))}
              </select>
            </>
          )}
          <div class="button-group">
            <button type="submit" disabled={!name.value.trim()}>
              {t("tasks.create.save")}
            </button>
          </div>
        </form>
      </dialog>
    </>
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
              <th scope="col">
                {t("tasks.task-list.table-headings.task-name")}
              </th>
              <th scope="col">
                {t("tasks.task-list.table-headings.assignee")}
              </th>
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
      <div class="list-actions">
        <a href="/tasks/start" class="button start-process">
          {t("tasks.start-process-label")}
        </a>
        <CreateTaskButton />
      </div>
    </div>
  );
};

const TaskRowEntry = ({ task, selected }) => {
  const { id, name, due, assignee } = task,
    { query } = useRoute(),
    // Opening a task must not drop the chosen filter and sorting: they live in
    // the address, so a link without them returns to an unfiltered list.
    list_query = keep_list_query(query);

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
        <a href={`/tasks/${id}/${task_tabs[0].id}${list_query}`}>{name}</a>
      </th>
      <td>{assignee ? assignee : "—"}</td>
      <td>
        {due ? (
          <time datetime={due} title={formatAbsolute(due)}>
            {formatRelativeDate(due)}
          </time>
        ) : (
          "—"
        )}
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
    { params, query } = useRoute(),
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
  // An error belongs to the task it was raised for. Without that, the error of
  // a task that no longer exists would stand in front of every task opened
  // afterwards: it renders in place of the tabs, and the tabs are what load a
  // task, so nothing would ever be asked for again.
  const is_error =
    task_value?.status === RESPONSE_STATE.ERROR &&
    task_data === undefined &&
    (task_value.requested_id === undefined ||
      task_value.requested_id === params.task_id);

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
          <a href={`/tasks${keep_list_query(query)}`} class="button">
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
            {task.value?.data?.tenantId && (
              <p class="tenant">
                {t("tasks.tenant")}: {task.value.data.tenantId}
              </p>
            )}
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
  // Note which task the answer was about, so a failure cannot be mistaken for
  // the state of the next task opened.
  const answer = state.api.task.one.value;
  if (answer?.status === RESPONSE_STATE.ERROR)
    state.api.task.one.value = { ...answer, requested_id: task_id };
  const task = state.api.task.one.value?.data;
  if (!task?.id) {
    // Task no longer exists (completed, deleted, or wrong id) — stop here so we
    // don't feed `undefined` into downstream URLs.
    return;
  }
  // A task created by hand belongs to no process definition; asking for one
  // only answers 404 and leaves an error in the shared signal.
  if (task.processDefinitionId) {
    await engine_rest.process_definition.one(state, task.processDefinitionId);
  }
  await engine_rest.task.get_identity_links(state, task.id);
};

const TaskTabs = () => {
  const state = useContext(AppState);
  const { params } = useRoute();
  const [t] = useTranslation();

  // Load the task whenever the active task changes, and clear the panes that
  // belong to the previous one.
  //
  // task.one is deliberately not among them. A task that no longer exists puts
  // the detail into its error state, and that state renders in place of these
  // tabs — unmounting them, so clearing task.one here would wipe the very error
  // that caused the unmount. The tabs would mount again, load again and fail
  // again, without end. The request keeps the previous task visible until the
  // next one arrives anyway, so nothing is gained by blanking it.
  useEffect(() => {
    void load_task_chain(state, params.task_id);
    return () => {
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
    date_state = useSignal(toLocalParts(due_date ?? new Date())),
    // Close only once the engine has taken it; a rejected change must stay on
    // screen, with what was typed still in the fields.
    save = (value) =>
      Promise.resolve(
        engine_rest.task.update_task(state, { due: value }, params.task_id),
      ).then((result) => {
        if (result?.status === RESPONSE_STATE.SUCCESS) close();
      }),
    submit = (event) => {
      event.preventDefault();
      save(fromLocalParts(date_state.value.date, date_state.value.time));
    },
    set_now = () => {
      const now = toLocalParts(new Date());
      date_state.value = now;
      save(fromLocalParts(now.date, now.time));
    },
    reset = () => save(null);

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
            value={date_state.value.date}
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
            value={date_state.value.time}
            onInput={(e) =>
              (date_state.value = {
                ...date_state.peek(),
                time: e.currentTarget.value,
              })
            }
          />
          <div class="button-group">
            <button type="button" class="secondary" onClick={set_now}>
              {t("tasks.dates.now")}
            </button>
            <button type="button" class="secondary" onClick={reset}>
              {t("tasks.dates.reset")}
            </button>
            <button type="submit">{t("common.submit")}</button>
          </div>
        </form>
        {state.api.task.update_result.value?.status ===
          RESPONSE_STATE.ERROR && (
          <p role="alert" class="error">
            {t("tasks.update-failed")}
          </p>
        )}
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
    date_state = useSignal(toLocalParts(followUpDate ?? new Date())),
    // Close only once the engine has taken it; a rejected change must stay on
    // screen, with what was typed still in the fields.
    save = (value) =>
      Promise.resolve(
        engine_rest.task.update_task(
          state,
          { followUp: value },
          params.task_id,
        ),
      ).then((result) => {
        if (result?.status === RESPONSE_STATE.SUCCESS) close();
      }),
    submit = (event) => {
      event.preventDefault();
      save(fromLocalParts(date_state.value.date, date_state.value.time));
    },
    set_now = () => {
      const now = toLocalParts(new Date());
      date_state.value = now;
      save(fromLocalParts(now.date, now.time));
    },
    reset = () => save(null);

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
            value={date_state.value.date}
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
            value={date_state.value.time}
            onInput={(e) =>
              (date_state.value = {
                ...date_state.peek(),
                time: e.currentTarget.value,
              })
            }
          />
          <div class="button-group">
            <button type="button" class="secondary" onClick={set_now}>
              {t("tasks.dates.now")}
            </button>
            <button type="button" class="secondary" onClick={reset}>
              {t("tasks.dates.reset")}
            </button>
            <button type="submit">{t("common.submit")}</button>
          </div>
        </form>
        {state.api.task.update_result.value?.status ===
          RESPONSE_STATE.ERROR && (
          <p role="alert" class="error">
            {t("tasks.update-failed")}
          </p>
        )}
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
    // Both the card above and the table below read identity_links, and nothing
    // re-read them after a change: an added or removed group only appeared
    // after leaving the task and coming back. The dialog deliberately stays
    // open — managing groups is usually several steps in a row.
    refresh_groups = () =>
      engine_rest.task.get_identity_links(
        state,
        state.api.task.one.value.data.id,
      ),
    submit = (event) => {
      event.preventDefault();
      // The engine takes an empty group id without complaint and the task then
      // carries a candidate nobody can name, so it is refused here.
      const group_id = (group_state.value ?? "").trim();
      if (!group_id) return;
      engine_rest.task
        .add_group(state, state.api.task.one.value.data.id, group_id)
        .then(() => {
          if (
            state.api.task.add_group.value.status === RESPONSE_STATE.SUCCESS
          ) {
            group_state.value = "";
            void refresh_groups();
          }
        });
    },
    delete_group = (group_id) =>
      void Promise.resolve(
        engine_rest.task.delete_group(
          state,
          state.api.task.one.value.data.id,
          group_id,
        ),
      ).then((result) => {
        if (result?.status !== RESPONSE_STATE.SUCCESS) return;
        void refresh_groups();
      });

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
            value={group_state.value ?? ""}
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
    { query } = useRoute(),
    assignee_input = useSignal(""),
    task = state.api.task.one.value?.data,
    signed_in_user = resolve_user(state),
    close = () => document.getElementById("set_assignee").close(),
    show = () => document.getElementById("set_assignee").showModal(),
    user_is_assignee = task?.assignee,
    assignee_is_different = task?.assignee && signed_in_user !== task?.assignee,
    unknown_user = useSignal(false),
    assign_failed =
      state.api.task.assign_result.value?.status === RESPONSE_STATE.ERROR,
    // Every action here changes who holds the task, and the answer carries no
    // body. Re-read the task so the card and the dialog show the new state,
    // and close — otherwise the dialog sits there unchanged and the click
    // looks as though it did nothing. The list carries the holder in a column
    // of its own and may well be filtered by it, so it is re-read too.
    then_refresh = (request) =>
      void Promise.resolve(request).then((result) => {
        if (result?.status !== RESPONSE_STATE.SUCCESS) return;
        void engine_rest.task.get_task(state, task.id);
        reload_tasks(state, query);
        close();
      }),
    assign_to_user = async (event) => {
      event.preventDefault();
      const user_id = assignee_input.value.trim();
      if (!user_id) return;
      unknown_user.value = false;

      // Check the id before handing the task over. Assigning to an id that does
      // not exist succeeds in the engine and leaves the task held by nobody: it
      // drops out of every list and only "reset assignee" brings it back.
      const found = await engine_rest.user.find(state, user_id);
      if (
        found?.status !== RESPONSE_STATE.SUCCESS ||
        !(found.data?.length > 0)
      ) {
        unknown_user.value = true;
        return;
      }

      then_refresh(
        engine_rest.task
          .assign_task(state, user_id, task.id)
          .then((result) => ((assignee_input.value = ""), result)),
      );
    };

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
            {/* The action on the task as it stands. Right-aligned like the
                submit below, so the two blocks read as one dialog rather than
                as a button loose beside the close button. */}
            <div class="button-group">
              {assignee_is_different ? (
                <button
                  type="button"
                  onClick={() =>
                    then_refresh(
                      engine_rest.task.assign_task(state, null, task.id),
                    )
                  }
                  class="secondary"
                >
                  <Icons.user_minus /> {t("tasks.reset-assignee")}
                </button>
              ) : user_is_assignee ? (
                <button
                  type="button"
                  onClick={() =>
                    then_refresh(engine_rest.task.unclaim_task(state, task.id))
                  }
                  class="secondary"
                >
                  <Icons.user_minus /> {t("tasks.unclaim")}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() =>
                    then_refresh(engine_rest.task.claim_task(state, task.id))
                  }
                  class="secondary"
                >
                  <Icons.user_plus /> {t("tasks.claim")}
                </button>
              )}
            </div>

            {/* Heading outside the form on purpose: the global `form` rule is a
                two-column label/input grid, and anything else placed in it
                becomes a grid item — the heading would sit beside the field. */}
            <h2>{t("tasks.assign-to")}</h2>
            <form onSubmit={assign_to_user}>
              <label for="assignee-input">{t("tasks.assign-to-label")}</label>
              <input
                id="assignee-input"
                type="text"
                autocomplete="off"
                aria-describedby={
                  unknown_user.value ? "assignee-error" : undefined
                }
                value={assignee_input.value}
                onInput={(e) => {
                  assignee_input.value = e.target.value;
                  unknown_user.value = false;
                }}
              />
              <div class="button-group">
                <button type="submit" disabled={!assignee_input.value.trim()}>
                  <Icons.user_plus /> {t("tasks.assign")}
                </button>
              </div>
            </form>
            {unknown_user.value && (
              <p id="assignee-error" role="alert" class="error">
                {t("tasks.assign-unknown-user")}
              </p>
            )}
            {assign_failed && (
              <p role="alert" class="error">
                {t("tasks.assign-failed")}
              </p>
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
    { query } = useRoute(),
    { route } = useLocation(),
    form = useSignal({
      name: "",
      description: "",
      color: "#000000",
      priority: 0,
      refresh: false,
      criteria: [],
      variables: [],
      readable_by_all: false,
      grants: [],
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
    add_grant = () =>
      update("grants", [...form.peek().grants, { type: "user", id: "" }]),
    remove_grant = (index) =>
      update(
        "grants",
        form.peek().grants.filter((_, i) => i !== index),
      ),
    update_grant = (index, field, value) =>
      update(
        "grants",
        form
          .peek()
          .grants.map((g, i) => (i === index ? { ...g, [field]: value } : g)),
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
        owner: resolve_user(state),
        query,
        properties: {
          description,
          color,
          priority: parseInt(priority, 10) || 0,
          refresh,
          variables,
        },
      };
      void engine_rest.filter
        .create_filter(state, body)
        .then(async (result) => {
          const id = result?.data?.id;
          if (id) await grant_access(state, id, form.value);
          route("/tasks");
        });
    };

  return (
    <div class="filter-editor">
      <header>
        <h2>{t("tasks.filter.title")}</h2>
        <a href={`/tasks${keep_list_query(query)}`} class="button">
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
          <a href={`/tasks${keep_list_query(query)}`}>{t("common.cancel")}</a>
        </div>
      
        <fieldset>
          <legend>{t("tasks.filter.permissions")}</legend>
          <p>{t("tasks.filter.permissions-hint")}</p>
          <label class="checkbox">
            <input
              type="checkbox"
              checked={form.value.readable_by_all}
              onChange={(e) =>
                update("readable_by_all", e.currentTarget.checked)
              }
            />
            {t("tasks.filter.readable-by-all")}
          </label>

          {form.value.grants.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>{t("common.type")}</th>
                  <th>{t("common.name")}</th>
                  <th>{t("common.action")}</th>
                </tr>
              </thead>
              <tbody>
                {form.value.grants.map((grant, i) => (
                  <tr key={i}>
                    <td>
                      <select
                        value={grant.type}
                        onChange={(e) =>
                          update_grant(i, "type", e.currentTarget.value)
                        }
                      >
                        <option value="user">{t("tasks.filter.user")}</option>
                        <option value="group">{t("tasks.filter.group")}</option>
                      </select>
                    </td>
                    <td>
                      <input
                        value={grant.id}
                        onInput={(e) =>
                          update_grant(i, "id", e.currentTarget.value)
                        }
                      />
                    </td>
                    <td>
                      <button
                        type="button"
                        class="danger"
                        onClick={() => remove_grant(i)}
                        aria-label={t("common.delete")}
                        title={t("common.delete")}
                      >
                        <Icons.trash />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <button type="button" onClick={add_grant}>
            {t("tasks.filter.add-permission")}
          </button>
        </fieldset>
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
    { params } = useRoute(),
    [t] = useTranslation(),
    {
      api: {
        history: { user_operation },
        task: { comment },
      },
    } = state;

  // Every other tab fetches what it shows when it is opened. These two were
  // loaded once with the task instead, so the tab never caught up with a
  // comment or an action taken since.
  useEffect(() => {
    void engine_rest.history.get_user_operation_by_task(state, params.task_id);
    void engine_rest.task.get_comments(state, params.task_id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.task_id]);

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
                    {formatTimestamp(entry.timestamp)}
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

// Variables carried by the task, outside whatever the form happens to expose.
// The previous Tasklist showed them beside the form and let the assignee change
// them; without that, anything the form does not mention is invisible.
const VariablesTab = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    task = state.api.task.one.value?.data,
    editing = useSignal(null),
    draft = useSignal({ name: "", type: "String", value: "" });

  const load = () =>
    void engine_rest.task.get_task_variables(state, params.task_id);

  useEffect(() => {
    load();
    return () => {
      state.api.task.variables.value = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.task_id]);

  const mine = !!task?.assignee && task.assignee === resolve_user(state);

  const save = async (name, type, raw) => {
    await engine_rest.task.set_task_variable(state, params.task_id, name, {
      value: coerce_variable_value(type, raw),
      type,
    });
    editing.value = null;
    draft.value = { name: "", type: "String", value: "" };
    load();
  };

  const remove = async (name) => {
    await engine_rest.task.delete_task_variable(state, params.task_id, name);
    load();
  };

  const rows = Object.entries(state.api.task.variables.value?.data ?? {});

  return (
    <div class="task-variables">
      <h2 class="screen-hidden">{t("tasks.variables.title")}</h2>
      {rows.length === 0 ? (
        <p class="info-box">{t("tasks.variables.empty")}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>{t("common.name")}</th>
              <th>{t("common.type")}</th>
              <th>{t("common.value")}</th>
              {mine && <th>{t("common.action")}</th>}
            </tr>
          </thead>
          <tbody>
            {rows.map(([name, v]) => (
              <tr key={name}>
                <td>{name}</td>
                <td>{v.type}</td>
                <td>
                  {editing.value === name ? (
                    <input
                      type="text"
                      value={draft.value.value}
                      onInput={(e) =>
                        (draft.value = {
                          ...draft.peek(),
                          value: e.currentTarget.value,
                        })
                      }
                    />
                  ) : (
                    format_variable_value(v.value)
                  )}
                </td>
                {mine && (
                  <td>
                    {editing.value === name ? (
                      <button
                        type="button"
                        onClick={() => save(name, v.type, draft.value.value)}
                      >
                        {t("common.submit")}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => {
                          editing.value = name;
                          draft.value = {
                            name,
                            type: v.type,
                            value: format_variable_value(v.value),
                          };
                        }}
                        aria-label={t("common.edit")}
                        title={t("common.edit")}
                      >
                        <Icons.pencil />
                      </button>
                    )}
                    <button
                      type="button"
                      class="danger"
                      onClick={() => remove(name)}
                      aria-label={t("common.delete")}
                      title={t("common.delete")}
                    >
                      <Icons.trash />
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {mine ? (
        <>
          <h3>{t("tasks.variables.add")}</h3>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              save(draft.value.name.trim(), draft.value.type, draft.value.value);
            }}
          >
            <label for="new-variable-name">{t("common.name")}</label>
            <input
              id="new-variable-name"
              type="text"
              value={editing.value === null ? draft.value.name : ""}
              onInput={(e) =>
                (draft.value = { ...draft.peek(), name: e.currentTarget.value })
              }
            />
            <label for="new-variable-type">{t("common.type")}</label>
            <select
              id="new-variable-type"
              value={draft.value.type}
              onChange={(e) =>
                (draft.value = { ...draft.peek(), type: e.currentTarget.value })
              }
            >
              {VARIABLE_TYPES.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
            <label for="new-variable-value">{t("common.value")}</label>
            <input
              id="new-variable-value"
              type="text"
              value={editing.value === null ? draft.value.value : ""}
              onInput={(e) =>
                (draft.value = { ...draft.peek(), value: e.currentTarget.value })
              }
            />
            <div class="button-group">
              <button type="submit" disabled={!draft.value.name.trim()}>
                {t("tasks.variables.save")}
              </button>
            </div>
          </form>
        </>
      ) : (
        <p class="info-box">{t("tasks.form.claim-first")}</p>
      )}
    </div>
  );
};

const AttachmentsTab = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    name = useSignal(""),
    description = useSignal(""),
    file = useSignal(null),
    form_ref = useRef(null);

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
    const result = await engine_rest.task.create_attachment(
      state,
      params.task_id,
      fd,
    );
    if (result?.status !== RESPONSE_STATE.SUCCESS) return;
    name.value = "";
    description.value = "";
    file.value = null;
    // The file input is uncontrolled, so clearing the signal leaves the chosen
    // file name standing. Only a reset puts it back to "no file selected".
    form_ref.current?.reset();
    load();
  };

  const remove = async (id) => {
    await engine_rest.task.delete_attachment(state, params.task_id, id);
    load();
  };

  const delete_open = useSignal(false),
    pending_delete = useSignal(null),
    ask_remove = (attachment) => {
      pending_delete.value = attachment;
      delete_open.value = true;
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
                        download={a.name ?? undefined}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <Icons.link_out /> {a.name ?? a.id}
                      </a>
                    </td>
                    <td>{a.description}</td>
                    <td>
                      <button
                        type="button"
                        class="danger"
                        onClick={() => ask_remove(a)}
                        aria-label={t("common.delete")}
                        title={t("common.delete")}
                      >
                        <Icons.trash />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          );
        }}
      />
      <ConfirmDialog
        open={delete_open}
        message={t("tasks.attachments.delete-confirm", {
          name: pending_delete.value?.name ?? "",
        })}
        confirm_label={t("tasks.attachments.confirm-delete")}
        on_confirm={() => remove(pending_delete.value?.id)}
      />

      <h3>{t("tasks.attachments.add")}</h3>
      <form onSubmit={submit} ref={form_ref}>
        <label for="attachment-name">{t("common.name")}</label>
        <input
          id="attachment-name"
          type="text"
          value={name.value}
          onInput={(e) => (name.value = e.currentTarget.value)}
        />
        <label for="attachment-description">
          {t("tasks.attachments.description")}
        </label>
        <input
          id="attachment-description"
          type="text"
          value={description.value}
          onInput={(e) => (description.value = e.currentTarget.value)}
        />
        <label for="attachment-file">{t("tasks.attachments.file")}</label>
        <div class="file-picker">
          {/* The native control is kept for the file dialog and for assistive
              technology, but hidden: its default rendering is the browser's
              own and looks nothing like the rest of the page. The label opens
              it just as the control itself would. */}
          <label for="attachment-file" class="button">
            {t("tasks.attachments.choose")}
          </label>
          <span class="file-name">
            {file.value?.name ?? t("tasks.attachments.none-chosen")}
          </span>
          <input
            id="attachment-file"
            class="screen-hidden"
            type="file"
            onChange={(e) => {
              const chosen = e.currentTarget.files[0];
              file.value = chosen;
              // The name is what the download is saved as, and the engine
              // stores nothing else about the file. Starting from the file's
              // own name keeps its extension; it stays editable.
              if (chosen && !name.value.trim()) name.value = chosen.name;
            }}
          />
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
    nameKey: "tasks.tabs.variables",
    id: "variables",
    pos: 2,
    Component: VariablesTab,
  },
  {
    nameKey: "tasks.tabs.attachments",
    id: "attachments",
    pos: 3,
    Component: AttachmentsTab,
  },
  {
    nameKey: "tasks.tabs.diagram",
    id: "diagram",
    pos: 4,
    Component: Diagram,
  },
];

export { TasksPage };
