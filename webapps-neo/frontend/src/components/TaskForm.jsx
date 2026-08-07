import { useState, useContext, useEffect, useRef } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import engine_rest, { RequestState } from "../api/engine_rest.jsx";
import * as Icons from "../assets/icons.jsx";
import { useRoute, useLocation } from "preact-iso";
import { CamundaForm } from "./CamundaForm.jsx";
import {
  vars_to_form_data,
  form_data_to_vars,
  build_legacy_form_data,
  parse_html,
} from "./TaskForm_helpers.js";

const TaskForm = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    selectedTask = state.api.task.one.value?.data,
    refName = state.server.value.c7_mode ? "camundaFormRef" : "operatonFormRef";

  if (!selectedTask)
    return <p class="info-box">{t("tasks.form.no-task-selected")}</p>;

  const formKey = selectedTask.formKey ?? "";
  const has_camunda_form_ref = !!selectedTask[refName];
  const is_camunda_form_via_key =
    formKey.startsWith("camunda-forms:") ||
    formKey.startsWith("operaton-forms:");
  const is_embedded_html_form = formKey.startsWith("embedded:");

  // Camunda Forms (form-js) — either by formRef or by camunda-forms:* formKey.
  if (has_camunda_form_ref || is_camunda_form_via_key) {
    return <CamundaTaskForm task={selectedTask} taskId={params.task_id} />;
  }

  // Legacy: embedded HTML form (looked up via task.form). Kept as-is for back-compat.
  if (is_embedded_html_form) {
    return <EmbeddedHtmlTaskForm task={selectedTask} formKey={formKey} />;
  }

  // No form configured — render auto-generated form for variables.
  return <RenderedFallbackForm task={selectedTask} taskId={params.task_id} />;
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
        <p class="error">
          {t("tasks.form.fetch-failed")}: {deployed.error?.message}
        </p>
      );
    }
    return <p class="fade-in-delayed">{t("common.loading")}</p>;
  }
  if (!vars) return <p class="fade-in-delayed">{t("common.loading")}</p>;

  const initial_data = vars_to_form_data(vars);

  const on_submit = ({ data, errors }) => {
    if (errors && Object.keys(errors).length > 0) {
      setError(t("tasks.form.validation-error") ?? "Validation error");
      return;
    }
    setError(null);
    const payload = form_data_to_vars(data, vars);
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
      {error && <p class="error">{error}</p>}
      <div class="form-buttons">
        <button type="button" onClick={() => submit_ref.current?.()}>
          {t("tasks.form.complete-task")}
        </button>
      </div>
    </div>
  );
};

// ---- Legacy embedded HTML form ----------------------------------------------

const EmbeddedHtmlTaskForm = ({ formKey }) => {
  const state = useContext(AppState);

  if (!state.api.task.form.value) {
    void engine_rest.task.get_task_form(state, formKey.substring(13));
  }

  return (
    <RequestState
      signal={state.api.task.form}
      on_success={() => (
        // eslint-disable-next-line react/no-danger
        <div
          dangerouslySetInnerHTML={{ __html: state.api.task.form.value.data }}
        />
      )}
    />
  );
};

// ---- Auto-generated fallback (no form configured) ---------------------------

const RenderedFallbackForm = ({ task, taskId }) => {
  const state = useContext(AppState),
    [t] = useTranslation(),
    [generated, setGenerated] = useState(""),
    [error, setError] = useState(null);

  if (!state.api.task.rendered_form.value) {
    void engine_rest.task.get_task_rendered_form(state, task.id);
  }
  const rendered_form = state.api.task.rendered_form.value;
  if (rendered_form?.data && generated === "") {
    setGenerated(parse_html(state, rendered_form.data));
  }

  return (
    <>
      <div style="margin-bottom: 8px;">{t("tasks.form.required-field")}</div>
      <div id="generated-form" class="task-form">
        <form onSubmit={(e) => submit_legacy_form(e, state, setError, taskId)}>
          <div
            class="form-fields"
            dangerouslySetInnerHTML={{ __html: generated }}
          />
          <div class={`error ${error ? "show" : "hidden"}`}>
            <span class="icon">
              <Icons.exclamation_triangle />
            </span>
            <span class="error-text">{error}</span>
          </div>
          <div class="form-buttons">
            <button type="submit">{t("tasks.form.complete-task")}</button>
            <button
              type="button"
              class="secondary"
              onClick={() => store_data(state)}
            >
              {t("tasks.form.save-form")}
            </button>
          </div>
        </form>
      </div>
    </>
  );
};

const submit_legacy_form = (e, state, setError, taskId) => {
  e.preventDefault();
  setError(null);
  const data = build_legacy_form_data();
  engine_rest.task
    .post_task_form(state, taskId, data)
    .then(() => {
      localStorage.removeItem(`task_form_${taskId}`);
      window.location.href = "/tasks";
    })
    .catch((error) => {
      console.error("Submit failed:", error);
      setError(error?.message || "An unknown error occurred.");
    });
};

const store_data = (state) => {
  localStorage.setItem(
    `task_form_${state.api.task.one.value?.data?.id}`,
    JSON.stringify(build_legacy_form_data(true)),
  );
};

export { TaskForm };
