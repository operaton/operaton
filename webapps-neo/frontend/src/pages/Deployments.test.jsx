import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup } from "@testing-library/preact";

// Spy all engine_rest API functions but keep RequestState/RESPONSE_STATE real.
vi.mock("../api/engine_rest.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  const spyify = (o) =>
    Object.fromEntries(
      Object.entries(o).map(([k, v]) => [
        k,
        typeof v === "function"
          ? vi.fn()
          : v && typeof v === "object"
            ? spyify(v)
            : v,
      ]),
    );
  return { ...actual, default: spyify(actual.default) };
});

// Stub the heavy diagram/form child components.
vi.mock("../components/BPMNViewer.jsx", () => ({
  BPMNViewer: ({ xml }) => h("div", { "data-testid": "bpmn-viewer" }, xml),
}));
vi.mock("../components/DMNViewer.jsx", () => ({
  DmnViewer: ({ xml }) => h("div", { "data-testid": "dmn-viewer" }, xml),
}));
vi.mock("../components/CamundaForm.jsx", () => ({
  CamundaForm: ({ schema }) =>
    h("div", { "data-testid": "camunda-form" }, JSON.stringify(schema)),
}));

let mockParams = {};
const routeFn = vi.fn();
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: routeFn, path: "/deployments" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { DeploymentsPage } from "./Deployments.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(DeploymentsPage, {})));

describe("DeploymentsPage", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    routeFn.mockClear();
  });
  afterEach(cleanup);

  it("fetches the deployment list on mount", () => {
    renderPage(state);
    expect(engine_rest.deployment.all).toHaveBeenCalled();
    expect(engine_rest.deployment.all.mock.lastCall[0]).toBe(state);
  });

  it("redirects to the first deployment when none is selected", () => {
    signal_response(state.api.deployment.all, [
      { id: "dep1", deploymentTime: "2024-01-01T00:00:00Z" },
      { id: "dep2", deploymentTime: "2024-01-02T00:00:00Z" },
    ]);
    renderPage(state);
    expect(routeFn).toHaveBeenCalledWith("/deployments/dep1", true);
  });

  it("renders the deployment list with links", () => {
    signal_response(state.api.deployment.all, [
      { id: "dep1", name: "My Deployment", deploymentTime: "2024-01-01" },
    ]);
    mockParams = { deployment_id: "dep1" };
    const { getByText } = renderPage(state);
    const link = getByText("My Deployment");
    expect(link.getAttribute("href")).toBe("/deployments/dep1");
  });

  it("fetches resources for the selected deployment", () => {
    mockParams = { deployment_id: "dep1" };
    renderPage(state);
    expect(engine_rest.deployment.resources).toHaveBeenCalled();
    expect(engine_rest.deployment.resources.mock.lastCall[0]).toBe(state);
    expect(engine_rest.deployment.resources.mock.lastCall[1]).toBe("dep1");
  });

  it("renders the resource list for the active deployment", () => {
    mockParams = { deployment_id: "dep1" };
    signal_response(state.api.deployment.all, [
      { id: "dep1", deploymentTime: "2024-01-01T00:00:00Z" },
    ]);
    signal_response(state.api.deployment.resources, [
      { id: "r1", name: "process.bpmn" },
    ]);
    const { getByText } = renderPage(state);
    const link = getByText("process.bpmn");
    expect(link.getAttribute("href")).toBe("/deployments/dep1/process.bpmn");
  });

  it("fetches resource content + definition + instance count when a resource is selected", () => {
    mockParams = { deployment_id: "dep1", resource_name: "process.bpmn" };
    signal_response(state.api.deployment.resources, [
      { id: "r1", name: "process.bpmn" },
    ]);
    renderPage(state);
    expect(engine_rest.deployment.resource).toHaveBeenCalled();
    expect(engine_rest.process_definition.by_deployment_id).toHaveBeenCalled();
    expect(engine_rest.process_instance.count).toHaveBeenCalled();
  });

  it("renders the BPMN viewer stub for a .bpmn resource", () => {
    mockParams = { deployment_id: "dep1", resource_name: "process.bpmn" };
    signal_response(state.api.deployment.resources, [
      { id: "r1", name: "process.bpmn" },
    ]);
    signal_response(state.api.deployment.resource, "<bpmn>xml</bpmn>");
    const { getByTestId, queryByTestId } = renderPage(state);
    expect(getByTestId("bpmn-viewer").textContent).toBe("<bpmn>xml</bpmn>");
    expect(queryByTestId("dmn-viewer")).toBeNull();
  });

  it("renders the DMN viewer stub for a .dmn resource", () => {
    mockParams = { deployment_id: "dep1", resource_name: "decision.dmn" };
    signal_response(state.api.deployment.resources, [
      { id: "r1", name: "decision.dmn" },
    ]);
    signal_response(state.api.deployment.resource, "<dmn>xml</dmn>");
    const { getByTestId } = renderPage(state);
    expect(getByTestId("dmn-viewer").textContent).toBe("<dmn>xml</dmn>");
  });

  it("renders the Camunda form preview stub for a .form resource", () => {
    mockParams = { deployment_id: "dep1", resource_name: "my.form" };
    signal_response(state.api.deployment.resources, [
      { id: "r1", name: "my.form" },
    ]);
    signal_response(
      state.api.deployment.resource,
      JSON.stringify({ components: [] }),
    );
    const { getByTestId } = renderPage(state);
    expect(getByTestId("camunda-form")).toBeTruthy();
  });

  it("prompts to select a resource when only a deployment is chosen", () => {
    mockParams = { deployment_id: "dep1" };
    const { getByText } = renderPage(state);
    expect(getByText("deployments.select-deployment-resource")).toBeTruthy();
  });
});
