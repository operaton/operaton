import { useContext, useEffect } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import { useLocation, useRoute } from "preact-iso";
import engine_rest, { RequestState } from "../api/engine_rest.jsx";
import { BPMNViewer } from "../components/BPMNViewer.jsx";
import { CamundaForm } from "../components/CamundaForm.jsx";
import { DmnViewer } from "../components/DMNViewer.jsx";
import { formatRelativeDate } from "../helper/date_formatter.js";

const DeploymentsPage = () => {
  const state = useContext(AppState),
    {
      deployments_page: { selected_resource },
    } = state,
    {
      params: { deployment_id, resource_name },
    } = useRoute(),
    { route } = useLocation(),
    [t] = useTranslation();

  // Load the deployment list once on mount.
  useEffect(() => {
    if (state.api.deployment.all.value === null) {
      void engine_rest.deployment.all(state);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
      state.api.process.definition.one.value = null;
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deployment_id, resource_name, state.api.deployment.resources.value]);

  return (
    <main id="content" class="deployments fade-in">
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
    { params } = useRoute(),
    [t] = useTranslation();

  return (
    <div>
      <table>
        <thead>
          <tr>
            <th>{t("common.name")}</th>
            <th>{t("deployments.deployed")}</th>
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
    <div>
      <table>
        <thead>
          <tr>
            <th>{t("deployments.resource")}</th>
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
          definition: { one: process_definition },
          instance: { count: instance_count },
        },
        deployment: { resource },
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

export { DeploymentsPage };
