import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

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

// Stub the heavy BPMN diagram component so bpmn-js never loads in happy-dom.
vi.mock("../components/BPMNViewer.jsx", () => ({
  BPMNViewer: ({ xml, mode }) =>
    h("div", { "data-testid": "bpmn-viewer", "data-mode": mode }, xml),
}));

let mockParams = {};
let mockQuery = {};
const routeFn = vi.fn();
vi.mock("preact-iso", () => ({
  useRoute: () => ({
    params: mockParams,
    query: mockQuery,
    path: "/processes",
  }),
  useLocation: () => ({ route: routeFn, path: "/processes" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { ProcessesPage } from "./Processes.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(ProcessesPage, {})));

const definition_rows = [
  {
    definition: {
      id: "proc:1",
      name: "Invoice",
      key: "invoice",
      version: 2,
      tenantId: "t1",
    },
    instances: 3,
    incidents: [{ id: "i1" }],
  },
  {
    definition: {
      id: "proc:2",
      name: "Order",
      key: "order",
      version: 1,
      tenantId: null,
    },
    instances: 0,
    incidents: [],
  },
];

describe("ProcessesPage — definition list", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    mockQuery = {};
    routeFn.mockClear();
  });
  afterEach(cleanup);

  it("fetches the process definition list on mount", () => {
    renderPage(state);
    expect(engine_rest.process_definition.list).toHaveBeenCalled();
    expect(engine_rest.process_definition.list.mock.lastCall[0]).toBe(state);
  });

  it("renders the deployed-definitions heading and a deploy link", () => {
    const { getByText } = renderPage(state);
    expect(getByText("processes.deployed-definitions")).toBeTruthy();
    const deploy = getByText("processes.deploy");
    expect(deploy.getAttribute("href")).toBe("/deployments");
  });

  it("renders one row per definition with a link to its instances", () => {
    signal_response(state.api.process.definition.list, definition_rows);
    const { getByText } = renderPage(state);
    const invoice = getByText("Invoice");
    expect(invoice.getAttribute("href")).toBe("/processes/proc:1/instances");
    const order = getByText("Order");
    expect(order.getAttribute("href")).toBe("/processes/proc:2/instances");
  });

  it("shows incident and instance counts in the row", () => {
    signal_response(state.api.process.definition.list, definition_rows);
    const { getAllByRole } = renderPage(state);
    const cells = getAllByRole("cell").map((c) => c.textContent);
    expect(cells).toContain("3"); // instances
    expect(cells).toContain("invoice"); // key
  });

  it("renders the empty state when the list is an empty success with no filter", () => {
    signal_response(state.api.process.definition.list, []);
    const { getByText } = renderPage(state);
    expect(getByText("processes.empty.heading")).toBeTruthy();
    expect(getByText("processes.empty.upload").getAttribute("href")).toBe(
      "/deployments",
    );
  });
});

describe("ProcessesPage — bulk actions", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    mockQuery = {};
    routeFn.mockClear();
    signal_response(state.api.process.definition.list, definition_rows);
  });
  afterEach(cleanup);

  const select_first = (container) => {
    // The first checkbox is "select all"; the second toggles the first row.
    const boxes = container.querySelectorAll('input[type="checkbox"]');
    fireEvent.click(boxes[1]);
  };

  it("disables bulk buttons when nothing is selected", () => {
    const { getByText } = renderPage(state);
    expect(getByText("processes.bulk.activate").disabled).toBe(true);
    expect(getByText("processes.bulk.suspend").disabled).toBe(true);
    expect(getByText("processes.bulk.remove").disabled).toBe(true);
  });

  it("enables bulk buttons once a row is selected", () => {
    const { container, getByText } = renderPage(state);
    select_first(container);
    expect(getByText("processes.bulk.activate").disabled).toBe(false);
  });

  it("activate calls engine_rest.process_definition.activate with the selected id", async () => {
    engine_rest.process_definition.activate.mockResolvedValue(undefined);
    const { container, getByText } = renderPage(state);
    select_first(container);
    fireEvent.click(getByText("processes.bulk.activate"));
    await Promise.resolve();
    await Promise.resolve();
    expect(engine_rest.process_definition.activate).toHaveBeenCalled();
    const call = engine_rest.process_definition.activate.mock.lastCall;
    expect(call[0]).toBe(state);
    expect(call[1]).toBe("proc:1");
  });

  it("suspend calls engine_rest.process_definition.suspend with the selected id", async () => {
    engine_rest.process_definition.suspend.mockResolvedValue(undefined);
    const { container, getByText } = renderPage(state);
    select_first(container);
    fireEvent.click(getByText("processes.bulk.suspend"));
    await Promise.resolve();
    await Promise.resolve();
    expect(engine_rest.process_definition.suspend).toHaveBeenCalled();
    expect(engine_rest.process_definition.suspend.mock.lastCall[1]).toBe(
      "proc:1",
    );
  });

  it("remove calls engine_rest.process_definition.remove after confirmation", async () => {
    engine_rest.process_definition.remove.mockResolvedValue(undefined);
    const prev = window.confirm;
    window.confirm = vi.fn(() => true);
    const { container, getByText } = renderPage(state);
    select_first(container);
    fireEvent.click(getByText("processes.bulk.remove"));
    await Promise.resolve();
    await Promise.resolve();
    expect(engine_rest.process_definition.remove).toHaveBeenCalled();
    expect(engine_rest.process_definition.remove.mock.lastCall[1]).toBe(
      "proc:1",
    );
    window.confirm = prev;
  });

  it("remove is skipped when the confirmation is declined", () => {
    const prev = window.confirm;
    window.confirm = vi.fn(() => false);
    const { container, getByText } = renderPage(state);
    select_first(container);
    fireEvent.click(getByText("processes.bulk.remove"));
    expect(engine_rest.process_definition.remove).not.toHaveBeenCalled();
    window.confirm = prev;
  });

  it("select-all checkbox selects every row", () => {
    const { container, getByText } = renderPage(state);
    const boxes = container.querySelectorAll('input[type="checkbox"]');
    fireEvent.click(boxes[0]);
    // bulk count message should appear with all rows selected
    expect(getByText("processes.bulk.count")).toBeTruthy();
  });
});

describe("ProcessesPage — definition detail fetches", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    mockQuery = {};
    routeFn.mockClear();
  });
  afterEach(cleanup);

  it("fetches the definition, diagram and statistics when a definition_id is routed", () => {
    mockParams = { definition_id: "proc:1" };
    renderPage(state);
    expect(engine_rest.process_definition.one).toHaveBeenCalled();
    expect(engine_rest.process_definition.diagram).toHaveBeenCalled();
    expect(engine_rest.process_definition.statistics).toHaveBeenCalled();
    expect(engine_rest.process_definition.one.mock.lastCall[1]).toBe("proc:1");
  });

  it("renders the definition overview (no panel) from the definition.one signal", () => {
    mockParams = { definition_id: "proc:1" };
    signal_response(state.api.process.definition.one, {
      id: "proc:1",
      name: "Invoice",
      key: "invoice",
      version: 2,
    });
    signal_response(state.api.process.definition.statistics, [
      { instances: 5, incidents: [{ id: "x" }] },
    ]);
    const { getAllByText } = renderPage(state);
    // Heading shows the definition name.
    expect(getAllByText("Invoice").length).toBeGreaterThan(0);
  });
});

describe("ProcessesPage — definition tabs", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockQuery = {};
    routeFn.mockClear();
    signal_response(state.api.process.definition.one, {
      id: "proc:1",
      name: "Invoice",
      key: "invoice",
      version: 2,
    });
  });
  afterEach(cleanup);

  it("instances tab fetches unfinished instances in live mode", () => {
    mockParams = { definition_id: "proc:1", panel: "instances" };
    renderPage(state);
    expect(
      engine_rest.history.process_instance.all_unfinished,
    ).toHaveBeenCalled();
  });

  it("instances tab fetches all (finished+unfinished) instances in history mode", () => {
    mockParams = { definition_id: "proc:1", panel: "instances" };
    mockQuery = { history: "true" };
    renderPage(state);
    expect(engine_rest.history.process_instance.all).toHaveBeenCalled();
  });

  it("instances tab renders rows from the instance list signal", () => {
    mockParams = { definition_id: "proc:1", panel: "instances" };
    signal_response(state.api.process.instance.list, [
      {
        id: "abcdef1234567890",
        startTime: "2024-01-01T00:00:00Z",
        state: "ACTIVE",
        businessKey: "BK-1",
      },
    ]);
    const { getByText } = renderPage(state);
    const link = getByText("abcdef12");
    expect(link.getAttribute("href")).toBe(
      "/processes/proc:1/instances/abcdef1234567890/vars",
    );
    expect(getByText("BK-1")).toBeTruthy();
  });

  it("incidents tab fetches and renders definition incidents", () => {
    mockParams = { definition_id: "proc:1", panel: "incidents" };
    signal_response(state.api.history.incident.by_process_definition, [
      {
        id: "inc1",
        incidentMessage: "boom",
        incidentType: "failedJob",
        configuration: "cfg",
      },
    ]);
    const { getByText } = renderPage(state);
    expect(
      engine_rest.history.incident.by_process_definition,
    ).toHaveBeenCalled();
    expect(getByText("boom")).toBeTruthy();
    expect(getByText("cfg")).toBeTruthy();
  });

  it("called-definitions tab fetches and renders called definitions", () => {
    mockParams = { definition_id: "proc:1", panel: "called_definitions" };
    signal_response(state.api.process.definition.called, [
      {
        id: "called:1",
        name: "Sub Process",
        suspended: false,
        calledFromActivityIds: ["task_a"],
      },
    ]);
    const { getByText } = renderPage(state);
    expect(engine_rest.process_definition.called).toHaveBeenCalled();
    const link = getByText("Sub Process");
    expect(link.getAttribute("href")).toBe("/processes/called:1");
  });

  it("jobs tab fetches and renders job definitions", () => {
    mockParams = { definition_id: "proc:1", panel: "jobs" };
    signal_response(state.api.job_definition.all.by_process_definition, [
      {
        id: "jd1",
        suspended: true,
        jobType: "timer",
        jobConfiguration: "R/PT5M",
        overridingJobPriority: 10,
      },
    ]);
    const { getByText } = renderPage(state);
    expect(
      engine_rest.job_definition.all.by_process_definition,
    ).toHaveBeenCalled();
    expect(getByText("timer")).toBeTruthy();
    expect(getByText("R/PT5M")).toBeTruthy();
  });
});

describe("ProcessesPage — instance details", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockQuery = {};
    routeFn.mockClear();
    signal_response(state.api.process.definition.one, {
      id: "proc:1",
      name: "Invoice",
      key: "invoice",
      version: 2,
    });
    signal_response(state.api.process.instance.one, {
      id: "inst-9999",
      businessKey: "BK-9",
    });
  });
  afterEach(cleanup);

  it("renders the instance description from instance.one", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "vars",
    };
    const { getByText } = renderPage(state);
    expect(getByText("inst-9999")).toBeTruthy();
    expect(getByText("BK-9")).toBeTruthy();
  });

  it("variables sub-panel fetches and renders live variables", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "vars",
    };
    signal_response(state.api.process.instance.variables, {
      amount: { type: "Integer", value: 42 },
    });
    const { getByText } = renderPage(state);
    expect(engine_rest.process_instance.variables).toHaveBeenCalled();
    expect(getByText("amount")).toBeTruthy();
    expect(getByText("42")).toBeTruthy();
  });

  it("variables sub-panel fetches history variables in history mode", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "vars",
    };
    mockQuery = { history: "true" };
    signal_response(state.api.process.instance.variables, [
      { name: "amount", type: "Integer", value: 7 },
    ]);
    const { getByText } = renderPage(state);
    expect(
      engine_rest.history.variable_instance.by_process_instance,
    ).toHaveBeenCalled();
    expect(getByText("amount")).toBeTruthy();
  });

  it("instance-incidents sub-panel fetches and renders incidents", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "instance_incidents",
    };
    signal_response(state.api.history.incident.by_process_instance, [
      {
        id: "ii1",
        incidentMessage: "instance boom",
        processInstanceId: "inst-9999",
        createTime: "2024-01-01T00:00:00Z",
        activityId: "act1",
        incidentType: "failedJob",
      },
    ]);
    const { getByText } = renderPage(state);
    expect(engine_rest.history.incident.by_process_instance).toHaveBeenCalled();
    expect(getByText("instance boom")).toBeTruthy();
  });

  it("called-instances sub-panel fetches and renders called instances", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "called_instances",
    };
    signal_response(state.api.process.instance.called, [
      { id: "child-1", suspended: false, definitionId: "child:def" },
    ]);
    const { getByText } = renderPage(state);
    expect(engine_rest.process_instance.called).toHaveBeenCalled();
    const link = getByText("child-1");
    expect(link.getAttribute("href")).toBe("/processes/child-1");
  });

  it("user-tasks sub-panel fetches and renders tasks", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "user_tasks",
    };
    signal_response(state.api.task.by_process_instance, [
      {
        id: "task-1",
        name: "Approve",
        assignee: "demo",
        owner: "boss",
        priority: 50,
      },
    ]);
    const { getByText } = renderPage(state);
    expect(engine_rest.task.get_process_instance_tasks).toHaveBeenCalled();
    expect(getByText("Approve")).toBeTruthy();
    expect(getByText("boss")).toBeTruthy();
  });

  it("fetches activity-instances for a live selected instance", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "vars",
    };
    renderPage(state);
    expect(engine_rest.process_instance.activity_instances).toHaveBeenCalled();
  });
});

describe("ProcessesPage — BPMN diagram", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockQuery = {};
    routeFn.mockClear();
  });
  afterEach(cleanup);

  it("does not render the viewer without a definition selected", () => {
    const { queryByTestId } = renderPage(state);
    expect(queryByTestId("bpmn-viewer")).toBeNull();
  });

  it("renders the BPMNViewer stub in definition mode when xml + statistics are present", () => {
    mockParams = { definition_id: "proc:1" };
    signal_response(state.api.process.definition.one, {
      id: "proc:1",
      key: "invoice",
    });
    signal_response(state.api.process.definition.diagram, {
      bpmn20Xml: "<bpmn>graph</bpmn>",
    });
    signal_response(state.api.process.definition.statistics, [
      { id: "act", instances: 1, incidents: [] },
    ]);
    const { getByTestId } = renderPage(state);
    const viewer = getByTestId("bpmn-viewer");
    expect(viewer.textContent).toBe("<bpmn>graph</bpmn>");
    expect(viewer.getAttribute("data-mode")).toBe("definition");
  });

  it("renders the BPMNViewer in instance mode for a live selected instance", () => {
    mockParams = {
      definition_id: "proc:1",
      panel: "instances",
      selection_id: "inst-9999",
      sub_panel: "vars",
    };
    signal_response(state.api.process.definition.one, {
      id: "proc:1",
      key: "invoice",
    });
    signal_response(state.api.process.definition.diagram, {
      bpmn20Xml: "<bpmn>graph</bpmn>",
    });
    signal_response(state.api.process.instance.one, {
      id: "inst-9999",
      businessKey: "BK-9",
    });
    signal_response(state.api.process.instance.activity_instances, {
      id: "root",
      childActivityInstances: [],
    });
    const { getByTestId } = renderPage(state);
    expect(getByTestId("bpmn-viewer").getAttribute("data-mode")).toBe(
      "instance",
    );
  });
});

describe("ProcessesPage — sub navigation", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    mockQuery = {};
    routeFn.mockClear();
  });
  afterEach(cleanup);

  it("disables the child nav entries on the definitions list", () => {
    const { getByText } = renderPage(state);
    // On the bare list, instances/incidents are rendered as disabled spans.
    const instances = getByText("processes.subnav.instances");
    expect(instances.tagName.toLowerCase()).toBe("span");
    expect(instances.getAttribute("class")).toContain("disabled");
  });

  it("links the child nav entries once a definition is selected", () => {
    mockParams = { definition_id: "proc:1" };
    signal_response(state.api.process.definition.one, {
      id: "proc:1",
      key: "invoice",
    });
    const { getByText } = renderPage(state);
    const instances = getByText("processes.subnav.instances");
    expect(instances.tagName.toLowerCase()).toBe("a");
    expect(instances.getAttribute("href")).toBe("/processes/proc:1/instances");
  });

  it("history toggle routes to the same path with the history query", () => {
    const { getByText } = renderPage(state);
    fireEvent.click(getByText("processes.enable-history-mode"));
    expect(routeFn).toHaveBeenCalledWith("/processes?history=true");
  });
});
