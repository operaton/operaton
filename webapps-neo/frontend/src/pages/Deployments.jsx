import { useContext, useEffect } from "preact/hooks";
import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import { useLocation, useRoute } from "preact-iso";
import engine_rest, {
  RequestState,
  RESPONSE_STATE,
} from "../api/engine_rest.jsx";
import { BPMNViewer } from "../components/BPMNViewer.jsx";
import { CamundaForm } from "../components/CamundaForm.jsx";
import { Dialog } from "../components/Dialog.jsx";
import { DmnViewer } from "../components/DMNViewer.jsx";
import { formatRelativeDate } from "../helper/date_formatter.js";
import { ListFilter } from "../components/ListFilter.jsx";
import { ManageFilters } from "../components/ManageFilters.jsx";
import {
  filter_share_link,
  parse_list_query,
  with_manage,
  without_manage,
  write_list_query,
} from "../helper/list_query.js";
import {
  create_saved_filter,
  delete_saved_filter,
  hydrate_signal,
  update_saved_filter,
} from "../helper/saved_filters.js";

const RESOURCE_TYPE = "deployment";

const SORT_OPTIONS = [
  { key: "deploymentTime", nameKey: "deployments.sort.deploymentTime" },
  { key: "name", nameKey: "deployments.sort.name" },
  { key: "id", nameKey: "deployments.sort.id" },
  { key: "tenantId", nameKey: "deployments.sort.tenantId" },
];

const FILTER_KEYS = [
  { key: "name", nameKey: "deployments.filter_keys.name", type: "string" },
  {
    key: "nameLike",
    nameKey: "deployments.filter_keys.nameLike",
    type: "string",
  },
  { key: "source", nameKey: "deployments.filter_keys.source", type: "string" },
  {
    key: "tenantIdIn",
    nameKey: "deployments.filter_keys.tenantIdIn",
    type: "string",
  },
  { key: "after", nameKey: "deployments.filter_keys.after", type: "date" },
  { key: "before", nameKey: "deployments.filter_keys.before", type: "date" },
];

const DEPLOYMENT_DEFAULTS = { sortBy: "deploymentTime", sortOrder: "desc" };

const find_saved = (signal, id) => {
  if (!id || id === "all") return null;
  return (signal.value?.data ?? []).find((f) => f.id === id) ?? null;
};

const load_deployments = (state, query) => {
  const { saved_filter_id, sortBy, sortOrder, criteria } =
    parse_list_query(query);
  const saved = find_saved(state.api.deployment.saved_filters, saved_filter_id);
  const params = {
    ...DEPLOYMENT_DEFAULTS,
    ...(saved?.query ?? {}),
    ...criteria,
    ...(sortBy ? { sortBy } : {}),
    ...(sortOrder ? { sortOrder } : {}),
  };
  void engine_rest.deployment.all(state, params);
};

const DeploymentsPage = () => {
  const state = useContext(AppState),
    // Ephemeral, single-page selection state — not a fetched resource.
    selected_resource = useSignal(null),
    {
      params: { deployment_id, resource_name },
      query,
    } = useRoute(),
    { route } = useLocation(),
    [t] = useTranslation();

  // Load the deployment list once on mount.
  useEffect(() => {
    hydrate_signal(RESOURCE_TYPE, state.api.deployment.saved_filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    load_deployments(state, query);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(query)]);

  // When the deployment list has loaded and the user is on /deployments with
  // no specific deployment selected, redirect to the first one.
  useEffect(() => {
    const list = state.api.deployment.all.value?.data;
    if (!deployment_id && list?.length) {
      route(`/deployments/${list[0].id}`, true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deployment_id, state.api.deployment.all.value]);

  // Load the resources for the active deployment; clear stale per-deployment
  // data on navigation so the next deployment's panel can't briefly render
  // against the previous deployment's signals.
  useEffect(() => {
    if (deployment_id) {
      void engine_rest.deployment.resources(state, deployment_id);
    }
    return () => {
      selected_resource.value = null;
      state.api.deployment.resources.value = null;
      state.api.deployment.resource.value = null;
      state.api.deployment.process_definition.value = null;
      state.api.process.instance.count.value = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deployment_id]);

  // Once resources are available and a specific resource is in the URL, look
  // up the resource by name and fetch its content + matching definition +
  // instance count.
  useEffect(() => {
    const resources_data = state.api.deployment.resources.value?.data;
    if (deployment_id && resource_name && resources_data) {
      const resource = resources_data.find((r) => r.name === resource_name);
      if (resource) {
        selected_resource.value = resource;
        void engine_rest.deployment.resource(state, deployment_id, resource.id);
        void engine_rest.process_definition.by_deployment_id(
          state,
          deployment_id,
          resource_name,
        );
        void engine_rest.process_instance.count(state, deployment_id);
      }
    }
    // Clear the resource-scoped signals whenever the selected resource changes
    // (or is cleared). Navigating between resources of the same deployment does
    // not re-run the deployment-scoped cleanup above, so without this a
    // re-selected resource would still hold stale SUCCESS data — mounting the
    // diagram viewer synchronously before its container element exists (crash).
    return () => {
      selected_resource.value = null;
      state.api.deployment.resource.value = null;
      state.api.deployment.process_definition.value = null;
      state.api.process.instance.count.value = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deployment_id, resource_name, state.api.deployment.resources.value]);

  return (
    <main id="content" class="deployments fade-in">
      <h1 class="screen-hidden">{t("nav.deployments")}</h1>
      <DeploymentsList />
      <ResourcesList />
      {resource_name ? (
        <ResourceDetails />
      ) : (
        <div class="deployment-empty">
          {t("deployments.select-deployment-resource")}
        </div>
      )}
    </main>
  );
};

const DeploymentsList = () => {
  const state = useContext(AppState),
    { params, query } = useRoute(),
    { route } = useLocation(),
    [t] = useTranslation();

  const parsed = parse_list_query(query);
  const list_current = {
    saved_filter_id: parsed.saved_filter_id,
    sortBy: parsed.sortBy ?? DEPLOYMENT_DEFAULTS.sortBy,
    sortOrder: parsed.sortOrder ?? DEPLOYMENT_DEFAULTS.sortOrder,
    criteria: parsed.criteria,
  };

  const apply_patch = (patch) => {
    route(write_list_query(window.location.href, patch), true);
  };
  const open_manage = () => route(with_manage(), false);

  if (query?.filters === "manage") return <DeploymentsManage />;

  return (
    <div class="deployment-list">
      <div class="deployment-toolbar">
        <ListFilter
          sort_options={SORT_OPTIONS}
          saved_filters_signal={state.api.deployment.saved_filters}
          current={list_current}
          defaults={DEPLOYMENT_DEFAULTS}
          on_change={apply_patch}
          on_manage={open_manage}
        />
        <DeploymentUpload />
      </div>
      <table>
        <thead>
          <tr>
            <th scope="col">{t("common.name")}</th>
            <th scope="col">{t("deployments.deployed")}</th>
          </tr>
        </thead>
        <tbody>
          <RequestState
            signal={state.api.deployment.all}
            on_success={() =>
              state.api.deployment.all.value?.data.map((deployment) => (
                <tr
                  key={deployment.id}
                  aria-selected={params.deployment_id === deployment.id}
                >
                  <th scope="row">
                    <a href={`/deployments/${deployment.id}`}>
                      {deployment?.name || deployment?.id}
                    </a>
                  </th>
                  <td>
                    <time datetime={deployment.deploymentTime}>
                      {formatRelativeDate(deployment.deploymentTime)}
                    </time>
                  </td>
                </tr>
              ))
            }
          />
        </tbody>
      </table>
    </div>
  );
};

const DeploymentUpload = () => {
  const state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation(),
    open = useSignal(false),
    name = useSignal(""),
    dedup = useSignal(false),
    changed_only = useSignal(false),
    files = useSignal(null),
    error = useSignal(null),
    busy = useSignal(false);

  const submit = async () => {
    if (!files.value?.length) return;
    const fd = new FormData();
    fd.append("deployment-name", name.value || "");
    fd.append("enable-duplicate-filtering", String(dedup.value));
    if (dedup.value)
      fd.append("deploy-changed-only", String(changed_only.value));
    for (const file of files.value) fd.append(file.name, file);

    busy.value = true;
    error.value = null;
    await engine_rest.deployment.create(state, fd);
    busy.value = false;

    const result = state.api.deployment.create.value;
    if (result?.status === RESPONSE_STATE.ERROR) {
      error.value = result.error?.message ?? t("common.error");
      return;
    }
    open.value = false;
    name.value = "";
    files.value = null;
    dedup.value = false;
    changed_only.value = false;
    void engine_rest.deployment.all(state);
    if (result?.data?.id) route(`/deployments/${result.data.id}`);
  };

  return (
    <>
      <div class="button-group">
        <button type="button" class="primary" onClick={() => (open.value = true)}>
          {t("deployments.upload.title")}
        </button>
      </div>
      <Dialog open={open} title={t("deployments.upload.title")}>
        <div class="dialog-fields">
          <label>
            {t("deployments.upload.name")}
            <input
              type="text"
              value={name.value}
              onInput={(e) => (name.value = e.target.value)}
            />
          </label>
          <label>
            {t("deployments.upload.files")}
            <input
              type="file"
              multiple
              onChange={(e) => (files.value = e.target.files)}
            />
          </label>
        </div>
        <label>
          <input
            type="checkbox"
            checked={dedup.value}
            onChange={(e) => (dedup.value = e.target.checked)}
          />
          {t("deployments.upload.duplicate-filtering")}
        </label>
        {dedup.value && (
          <label>
            <input
              type="checkbox"
              checked={changed_only.value}
              onChange={(e) => (changed_only.value = e.target.checked)}
            />
            {t("deployments.upload.deploy-changed-only")}
          </label>
        )}
        {error.value && <p class="error">{error.value}</p>}
        <div class="button-group">
          <button
            type="button"
            onClick={submit}
            disabled={busy.value || !files.value?.length}
          >
            {t("deployments.upload.submit")}
          </button>
          <button type="button" onClick={() => (open.value = false)}>
            {t("common.cancel")}
          </button>
        </div>
      </Dialog>
    </>
  );
};

const ResourcesList = () => {
  const state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation();

  if (!params.deployment_id) {
    return (
      <div class="deployment-empty">{t("deployments.select-deployment")}</div>
    );
  }

  return (
    <div class="resource-list">
      <table>
        <thead>
          <tr>
            <th scope="col">{t("deployments.resource")}</th>
          </tr>
        </thead>
        <tbody>
          <RequestState
            signal={state.api.deployment.resources}
            on_success={() =>
              state.api.deployment.resources.value?.data.map((resource) => (
                <tr
                  key={resource.id}
                  aria-selected={params.resource_name === resource.name}
                >
                  <th scope="row">
                    <a
                      href={`/deployments/${params.deployment_id}/${resource.name}`}
                    >
                      {resource.name.includes("/")
                        ? resource.name.split("/").pop().trim()
                        : resource.name || "N/A"}
                    </a>
                  </th>
                </tr>
              ))
            }
          />
        </tbody>
      </table>
    </div>
  );
};

const ResourceDetails = () => {
  const state = useContext(AppState),
    {
      api: {
        process: {
          instance: { count: instance_count },
        },
        deployment: { resource, process_definition },
      },
    } = state,
    {
      params: { resource_name },
    } = useRoute(),
    [t] = useTranslation(),
    resource_file_type = resource_name?.split(".").pop();

  return (
    <div class="process-details">
      <RequestState
        signal={resource}
        on_nothing={() => (
          <p class="info-box">{t("deployments.no-resource")}</p>
        )}
        on_success={() =>
          process_definition.value?.data?.length > 0 ? (
            <div>
              <h3>
                {process_definition.value?.data[0].name ||
                  t("deployments.no-process-name")}
              </h3>
              <p
                class={
                  process_definition.value?.data[0].suspended
                    ? "status-suspended"
                    : "status-active"
                }
              >
                {process_definition.value?.data[0].suspended
                  ? t("common.suspended")
                  : t("common.active")}
              </p>
              <dl>
                <dt>{t("common.name")}</dt>
                <dd>{process_definition.value?.data[0].name || "?"}</dd>
                <dt>{t("common.key")}</dt>
                <dd>{process_definition.value?.data[0].key || "?"}</dd>
                <dt>{t("deployments.instance-count")}</dt>
                <dd>
                  <RequestState
                    signal={instance_count}
                    on_success={() => instance_count.value?.data.count}
                  />
                </dd>
              </dl>
            </div>
          ) : null
        }
      />
      {(resource_file_type === "bpmn" || resource_file_type === "dmn") && (
        <div id="diagram-container" />
      )}
      <RequestState
        signal={resource}
        on_success={() =>
          resource.value.data !== null
            ? {
                bpmn: (
                  <BPMNViewer
                    xml={resource.value.data}
                    container={"diagram-container"}
                  />
                ),
                dmn: (
                  <DmnViewer
                    xml={resource.value.data}
                    container={"#diagram-container"}
                  />
                ),
                form: <FormPreview data={resource.value.data} />,
              }[resource_file_type]
            : null
        }
      />
    </div>
  );
};

/**
 * Renders a Camunda Forms (`.form`) deployment resource: the form-js schema
 * rendered read-only above the raw JSON for reference.
 */
const FormPreview = ({ data }) => {
  const [t] = useTranslation();
  let schema;
  try {
    schema = typeof data === "string" ? JSON.parse(data) : data;
  } catch (e) {
    return <p class="error">{e.message}</p>;
  }
  return (
    <>
      <h3>{t("deployments.form-preview")}</h3>
      <CamundaForm schema={schema} disabled />
      <hr />
      <h3>{t("deployments.raw-data-json")}</h3>
      <pre>{JSON.stringify(schema, null, 2)}</pre>
    </>
  );
};

const DeploymentsManage = () => {
  const state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation();

  const refresh = () =>
    hydrate_signal(RESOURCE_TYPE, state.api.deployment.saved_filters);
  return (
    <div class="fade-in">
      <ManageFilters
        title={t("deployments.filter.manage_title")}
        saved_filters_signal={state.api.deployment.saved_filters}
        filter_keys={FILTER_KEYS}
        sort_options={SORT_OPTIONS}
        on_save={(f) => {
          create_saved_filter(RESOURCE_TYPE, f);
          refresh();
        }}
        on_update={(id, f) => {
          update_saved_filter(RESOURCE_TYPE, id, f);
          refresh();
        }}
        on_delete={(id) => {
          delete_saved_filter(RESOURCE_TYPE, id);
          refresh();
        }}
        on_close={() => route(without_manage(), true)}
        build_share_link={(f) => filter_share_link(window.location.href, f)}
      />
    </div>
  );
};

export { DeploymentsPage };
