import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// bpmn-js (and its CSS assets) cannot run in happy-dom. Stub the NavigatedViewer
// with a class whose service registry (`get`) returns lightweight spies, so the
// component can wire up overlays/events/canvas without rendering a real diagram.
// Created via vi.hoisted so the spies exist when the hoisted vi.mock factory runs.
const {
  NavigatedViewer,
  importXML,
  overlays_add,
  eventBus_on,
  canvas_zoom,
  stepZoom,
} = vi.hoisted(() => {
  const importXML = vi.fn(() => Promise.resolve());
  const destroy = vi.fn();
  const overlays_add = vi.fn();
  const eventBus_on = vi.fn();
  const canvas_zoom = vi.fn();
  const stepZoom = vi.fn();
  const fakeGfx = () => ({ classList: { add: vi.fn(), remove: vi.fn() } });
  const elementRegistry = {
    get: vi.fn((id) => ({ id, type: "bpmn:UserTask", width: 100, height: 80 })),
    getGraphics: vi.fn(() => fakeGfx()),
  };
  const services = {
    zoomScroll: { stepZoom },
    canvas: { zoom: canvas_zoom },
    overlays: { add: overlays_add },
    eventBus: { on: eventBus_on },
    elementRegistry,
  };
  const NavigatedViewer = vi.fn(function () {
    this.importXML = importXML;
    this.destroy = destroy;
    this.get = vi.fn((name) => services[name]);
  });
  return {
    NavigatedViewer,
    importXML,
    overlays_add,
    eventBus_on,
    canvas_zoom,
    stepZoom,
  };
});

vi.mock("bpmn-js/lib/NavigatedViewer", () => ({ default: NavigatedViewer }));
vi.mock("bpmn-js/dist/assets/diagram-js.css", () => ({}));
vi.mock("bpmn-js/dist/assets/bpmn-js.css", () => ({}));
vi.mock("bpmn-js/dist/assets/bpmn-font/css/bpmn.css", () => ({}));

const { route } = vi.hoisted(() => ({ route: vi.fn() }));
let mockRoute = { params: { definition_id: "def1" }, query: {} };
vi.mock("preact-iso", () => ({
  useRoute: () => mockRoute,
  useLocation: () => ({ route }),
}));

// Spy all engine_rest API functions but keep RESPONSE_STATE real.
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

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { BPMNViewer } from "./BPMNViewer.jsx";
import { create_mock_state } from "../test/helpers.js";

const renderViewer = (props) => {
  const state = create_mock_state();
  // The component reads/writes container styles and runs an async importXML.
  document.body.innerHTML = '<div id="diagram"></div>';
  const result = render(
    h(AppState.Provider, { value: state }, h(BPMNViewer, props)),
  );
  return { ...result, state, containerEl: document.getElementById("diagram") };
};

const flush = () => new Promise((r) => setTimeout(r, 0));

describe("BPMNViewer", () => {
  beforeEach(() => {
    mockRoute = { params: { definition_id: "def1" }, query: {} };
    route.mockClear();
    NavigatedViewer.mockClear();
    importXML.mockClear();
    canvas_zoom.mockClear();
    overlays_add.mockClear();
    eventBus_on.mockClear();
  });
  afterEach(cleanup);

  it("mounts without throwing and constructs the viewer with the container element", () => {
    const { containerEl } = renderViewer({
      xml: "<bpmn/>",
      container: "diagram",
    });
    expect(NavigatedViewer).toHaveBeenCalledTimes(1);
    expect(NavigatedViewer.mock.lastCall[0].container).toBe(containerEl);
  });

  it("imports the supplied xml and fits the viewport", async () => {
    renderViewer({ xml: "<bpmn>diagram</bpmn>", container: "diagram" });
    await flush();
    expect(importXML).toHaveBeenCalledWith("<bpmn>diagram</bpmn>");
    expect(canvas_zoom).toHaveBeenCalledWith("fit-viewport", "auto");
  });

  it("adds overlays for tokens in definition mode", async () => {
    renderViewer({
      xml: "<bpmn/>",
      container: "diagram",
      tokens: [{ id: "act1", instances: 2, incidents: [] }],
    });
    await flush();
    expect(overlays_add).toHaveBeenCalled();
    // definition mode wires an element.click handler on the event bus
    expect(eventBus_on).toHaveBeenCalledWith(
      "element.click",
      expect.any(Function),
    );
  });

  it("triggers an instance fetch when an action button is clicked (definition mode)", async () => {
    const { containerEl, state } = renderViewer({
      xml: "<bpmn/>",
      container: "diagram",
      tokens: [{ id: "act1", instances: 3, incidents: [] }],
    });
    await flush();
    // Simulate the delegated click on a rendered action button.
    const btn = document.createElement("button");
    btn.className = "bpmn-action-btn";
    btn.dataset.action = "instances";
    btn.dataset.activityId = "act1";
    containerEl.appendChild(btn);
    fireEvent.click(btn);
    expect(engine_rest.process_instance.by_activity_ids).toHaveBeenCalled();
    const call = engine_rest.process_instance.by_activity_ids.mock.lastCall;
    expect(call[0]).toBe(state);
    expect(call[1]).toBe("def1");
    expect(call[2]).toEqual(["act1"]);
    expect(route).toHaveBeenCalledWith("/processes/def1/instances");
  });

  it("renders the zoom controls into the container portal", () => {
    const { containerEl } = renderViewer({
      xml: "<bpmn/>",
      container: "diagram",
    });
    expect(containerEl.querySelector(".bpmn-controls")).toBeTruthy();
  });

  it("zooms via the control buttons", () => {
    const { containerEl } = renderViewer({
      xml: "<bpmn/>",
      container: "diagram",
    });
    const [zoom_in_btn] = containerEl.querySelectorAll(".bpmn-controls button");
    fireEvent.click(zoom_in_btn);
    expect(stepZoom).toHaveBeenCalledWith(1);
  });

  it("renders draggable token handles in instance mode", async () => {
    renderViewer({
      xml: "<bpmn/>",
      container: "diagram",
      mode: "instance",
      instance_id: "inst1",
      tokens: [{ id: "act1", instances: 1, activity_instance_ids: ["ai1"] }],
    });
    await flush();
    expect(overlays_add).toHaveBeenCalled();
    const html = overlays_add.mock.calls[0][1].html;
    expect(html).toContain("bpmn-token-handle");
  });

  it("also renders a drag handle in definition mode (alongside action buttons)", async () => {
    renderViewer({
      xml: "<bpmn/>",
      container: "diagram",
      tokens: [{ id: "act1", instances: 3, incidents: [] }],
    });
    await flush();
    const htmls = overlays_add.mock.calls.map((c) => c[1].html);
    expect(htmls.some((h) => h.includes("bpmn-token-handle"))).toBe(true);
    expect(htmls.some((h) => h.includes("bpmn-actions"))).toBe(true);
    // The definition handle carries the running-instance count.
    const handle = htmls.find((h) => h.includes("bpmn-token-handle"));
    expect(handle).toContain('data-instances="3"');
  });

  // NOTE: the full drag→drop→batch flow can't be simulated here — happy-dom
  // does not support synthetic drag events. The batch request shape the dialog
  // builds is covered by BPMNViewer_helpers.test.js, and the API wiring by
  // process_instance.test.js (modify_async).
});
