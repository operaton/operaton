import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, cleanup } from "@testing-library/preact";

// dmn-js does not run in happy-dom; replace it with a lightweight stub whose
// methods are spies so we can assert how the component wires it up. The spies
// are created via vi.hoisted so they exist when the hoisted vi.mock factory runs.
const { DmnJS, importXML } = vi.hoisted(() => {
  const importXML = vi.fn();
  const DmnJS = vi.fn(function () {
    this.importXML = importXML;
  });
  return { DmnJS, importXML };
});

vi.mock("dmn-js", () => ({ default: DmnJS }));

import { DmnViewer } from "./DMNViewer.jsx";

describe("DmnViewer", () => {
  beforeEach(() => {
    DmnJS.mockClear();
    importXML.mockClear();
    document.body.innerHTML = '<div id="diagram">stale content</div>';
  });
  afterEach(cleanup);

  it("instantiates DmnJS with the given container option", () => {
    render(<DmnViewer xml="<dmn/>" container="#diagram" />);
    expect(DmnJS).toHaveBeenCalledTimes(1);
    expect(DmnJS.mock.lastCall[0].container).toBe("#diagram");
  });

  it("imports the supplied xml into the viewer", () => {
    render(<DmnViewer xml="<dmn>content</dmn>" container="#diagram" />);
    expect(importXML).toHaveBeenCalled();
    expect(importXML.mock.lastCall[0]).toBe("<dmn>content</dmn>");
  });

  it("clears the container's text before rendering", () => {
    render(<DmnViewer xml="<dmn/>" container="#diagram" />);
    expect(document.querySelector("#diagram").innerText).toBe("");
  });

  it("renders without throwing", () => {
    expect(() =>
      render(<DmnViewer xml="<dmn/>" container="#diagram" />),
    ).not.toThrow();
  });
});
