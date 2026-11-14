import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup } from "@testing-library/preact";

// bpmn-js cannot run in happy-dom: stub the viewer with spies. The point of this
// suite is what the renderer is constructed with, not what it draws.
const { NavigatedViewer, importXML, destroy, canvas_stub, fit_view_left } = vi.hoisted(
  () => {
    const importXML = vi.fn(() => Promise.resolve());
    const destroy = vi.fn();
    const canvas_stub = { zoom: vi.fn() };
    const fit_view_left = vi.fn();
    const NavigatedViewer = vi.fn(function () {
      this.importXML = importXML;
      this.destroy = destroy;
      this.get = vi.fn(() => canvas_stub);
    });
    return { NavigatedViewer, importXML, destroy, canvas_stub, fit_view_left };
  },
);

vi.mock("bpmn-js/lib/NavigatedViewer", () => ({ default: NavigatedViewer }));
// The placement itself (shift the viewbox, keep the fit) is covered by
// bpmn_viewbox.test.js; here it only has to be applied to the right canvas.
vi.mock("../bpmn_viewbox.js", () => ({ fit_view_left }));

import { ThemedBpmn } from "./ThemedBpmn.jsx";
import { resolved_theme } from "../theme.js";

const flush = () => new Promise((r) => setTimeout(r, 0));

// The migration previews are themed by remounting on a key change, so the two
// things worth guarding are: the renderer gets the --canvas-* colours (no
// stylesheet can reach bpmn-js output), and a theme change builds a second
// viewer rather than repainting the first.
describe("ThemedBpmn", () => {
  const root = document.documentElement;

  beforeEach(() => {
    NavigatedViewer.mockClear();
    importXML.mockClear();
    destroy.mockClear();
    fit_view_left.mockClear();
  });
  afterEach(() => {
    cleanup();
    resolved_theme.value = "light";
    root.style.removeProperty("--canvas-fill");
  });

  it("passes the canvas colours to the renderer", async () => {
    root.style.setProperty("--canvas-fill", "#151c25");
    resolved_theme.value = "dark";
    render(h(ThemedBpmn, { diagramXML: "<bpmn/>" }));
    await flush();

    expect(NavigatedViewer).toHaveBeenCalledTimes(1);
    expect(NavigatedViewer.mock.lastCall[0].bpmnRenderer.defaultFillColor).toBe(
      "#151c25",
    );
    expect(importXML).toHaveBeenCalledWith("<bpmn/>");
    expect(fit_view_left).toHaveBeenCalledWith(canvas_stub);
  });

  it("imports nothing and builds no viewer without xml", () => {
    render(h(ThemedBpmn, { diagramXML: null }));
    expect(NavigatedViewer).not.toHaveBeenCalled();
    expect(importXML).not.toHaveBeenCalled();
  });

  it("destroys the viewer when it unmounts", async () => {
    const { unmount } = render(h(ThemedBpmn, { diagramXML: "<bpmn/>" }));
    await flush();
    unmount();
    expect(destroy).toHaveBeenCalled();
  });
});