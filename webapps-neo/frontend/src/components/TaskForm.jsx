import { useState, useContext, useEffect, useRef } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { useRoute, useLocation } from "preact-iso";
import { CamundaForm } from "./CamundaForm.jsx";
import {
  vars_to_form_data,
  form_data_to_vars,
  schema_variable_keys,
  rendered_form_to_schema,
  form_ref_of,
} from "./TaskForm_helpers.js";

const TaskForm = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    selectedTask = state.api.task.one.value?.data;

  if (!selectedTask)
    return <p class="info-box">{t("tasks.form.no-task-selected")}</p>;

  const formKey = selectedTask.formKey ?? "";
  const is_embedded_html_form = formKey.startsWith("embedded:");

  // Camunda Forms (form-js) are referenced by formRef, never by formKey.
  if (form_ref_of(selectedTask)) {
    return <CamundaTaskForm task={selectedTask} taskId={params.task_id} />;
  }

  // Legacy: embedded HTML form (looked up via task.form). Kept as-is for back-compat.
  if (is_embedded_html_form) {
    return <EmbeddedHtmlTaskForm task={selectedTask} formKey={formKey} />;
  }

  // No form key — the task either has a generated form (camunda:formData) or no
  // form at all. Render the engine's generated form through the same CamundaForm
  // renderer so it looks like a form-js form.
  return <GeneratedTaskForm task={selectedTask} taskId={params.task_id} />;
};

// ---- Camunda Forms (form-js) ------------------------------------------------

const CamundaTaskForm = ({ task, taskId }) => {
  const state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation(),
    [error, setError] = useState(null),
    submit_ref = useRef(null);

  useEffect(() => {
    void engine_rest.task.get_task_deployed_form(state, task.id);
    void engine_rest.task.get_task_form_variables(state, task.id);
  }, [task.id]);

  const deployed = state.api.task.deployed_form.value;
  const variables = state.api.task.form_variables.value;

  const schema = deployed?.data;
  const vars = variables?.data;
  if (
    !schema ||
    typeof schema !== "object" ||
    !Array.isArray(schema.components)
  ) {
    if (deployed?.status === "ERROR") {
      return (
        <p class="error" role="alert">
          {t("tasks.form.fetch-failed")}: {deployed.error?.message}
        </p>
      );
    }
    return <p class="fade-in-delayed">{t("common.loading")}</p>;
  }
  if (!vars) return <p class="fade-in-delayed">{t("common.loading")}</p>;

  // Only round-trip the variables the form actually binds, so untouched
  // Json/Object variables aren't echoed back and rejected on submit (see #92).
  const allowed = schema_variable_keys(schema);
  const initial_data = vars_to_form_data(vars, allowed);

  const on_submit = ({ data, errors }) => {
    if (errors && Object.keys(errors).length > 0) {
      setError(t("tasks.form.validation-error") ?? "Validation error");
      return;
    }
    setError(null);
    const payload = form_data_to_vars(data, vars, allowed);
    engine_rest.task
      .post_task_form(state, taskId, payload)
      .then(() => {
        localStorage.removeItem(`task_form_${taskId}`);
        route("/tasks");
      })
      .catch((e) => setError(e?.message ?? "Submit failed"));
  };

  return (
    <div class="task-form camunda-task-form">
      <CamundaForm
        schema={schema}
        data={initial_data}
        on_submit={on_submit}
        on_ready={(c) => {
          submit_ref.current = c.submit;
        }}
      />
      {error && <p class="error" role="alert">{error}</p>}
      <div class="form-buttons">
        <button type="button" onClick={() => submit_ref.current?.()}>
          {t("tasks.form.complete-task")}
        </button>
      </div>
    </div>
  );
};

// ---- Legacy embedded HTML form ----------------------------------------------

const EMBEDDED_APP = "embedded:app:";
const EMBEDDED_DEPLOYMENT = "embedded:deployment:";

const FORM_JS_MIGRATION_DOCS =
  "https://docs.operaton.org/documentation/user-guide/task-forms/";

const EmbeddedHtmlTaskForm = ({ task, formKey }) => {
  const state = useContext(AppState),
    [t] = useTranslation();

  // Legacy AngularJS embedded HTML forms are not supported by the new web
  // apps and won't be. We still issue the correct fetch by prefix (see #96) —
  // embedded:app: forms are app-served, embedded:deployment: forms live in the
  // deployment — but render a migration notice instead of the HTML.
  useEffect(() => {
    if (formKey.startsWith(EMBEDDED_DEPLOYMENT)) {
      void engine_rest.task.get_task_deployed_form_html(state, task.id);
    } else {
      const path = formKey.startsWith(EMBEDDED_APP)
        ? formKey.substring(EMBEDDED_APP.length)
        : formKey.replace(/^embedded:/, "");
      void engine_rest.task.get_task_form(state, path);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formKey, task.id]);

  return (
    <p class="info-box">
      {t("tasks.form.legacy-html-unsupported")}{" "}
      <a href={FORM_JS_MIGRATION_DOCS} target="_blank" rel="noreferrer">
        {t("tasks.form.legacy-html-migrate-link")}
      </a>
    </p>
  );
};

// ---- Generated task form (camunda:formData, or no form) ---------------------

// A generated form has no formKey: the engine renders it from the task's
// camunda:formData fields. We fetch that rendered HTML only to read the field
// descriptors, turn them into a form-js schema, and render it with the same
// CamundaForm component as a deployed form — so both look identical.
const GeneratedTaskForm = ({ task, taskId }) => {
  const state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation(),
    [error, setError] = useState(null),
    submit_ref = useRef(null);

  useEffect(() => {
    void engine_rest.task.get_task_rendered_form(state, task.id);
    void engine_rest.task.get_task_form_variables(state, task.id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [task.id]);

  const rendered = state.api.task.rendered_form.value;
  const variables = state.api.task.form_variables.value;

  if (rendered?.status === "ERROR") {
    return (
      <p class="error" role="alert">
        {t("tasks.form.fetch-failed")}: {rendered.error?.message}
      </p>
    );
  }
  if (!rendered?.data || !variables) {
    return <p class="fade-in-delayed">{t("common.loading")}</p>;
  }

  const schema = rendered_form_to_schema(rendered.data);
  const vars = variables.data;
  const allowed = schema_variable_keys(schema);
  const initial_data = vars_to_form_data(vars, allowed);
  const has_fields = schema.components.length > 0;

  const on_submit = ({ data, errors }) => {
    if (errors && Object.keys(errors).length > 0) {
      setError(t("tasks.form.validation-error"));
      return;
    }
    setError(null);
    const payload = form_data_to_vars(data, vars, allowed);
    engine_rest.task
      .post_task_form(state, taskId, payload)
      .then(() => {
        localStorage.removeItem(`task_form_${taskId}`);
        route("/tasks");
      })
      .catch((e) => setError(e?.message ?? "Submit failed"));
  };

  return (
    <div class="task-form camunda-task-form">
      <CamundaForm
        schema={schema}
        data={initial_data}
        on_submit={on_submit}
        on_ready={(c) => {
          submit_ref.current = c.submit;
        }}
      />
      {error && (
        <p class="error" role="alert">
          {error}
        </p>
      )}
      <div class="form-buttons">
        {has_fields ? (
          <button type="button" onClick={() => submit_ref.current?.()}>
            {t("tasks.form.complete-task")}
          </button>
        ) : (
          <button
            type="button"
            onClick={() => complete_directly(state, setError, taskId, route)}
          >
            {t("tasks.form.complete-directly")}
          </button>
        )}
      </div>
    </div>
  );
};

// Complete a task without submitting a form, via the dedicated /complete
// endpoint (used when the task has no form fields).
const complete_directly = (state, setError, taskId, route) => {
  setError(null);
  engine_rest.task
    .complete_task(state, taskId)
    .then(() => {
      localStorage.removeItem(`task_form_${taskId}`);
      route("/tasks");
    })
    .catch((error) => setError(error?.message || "Complete failed"));
};

export { TaskForm };
