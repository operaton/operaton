import { describe, it, expect, vi } from "vitest";
import { fit_view_left, FIT_CONTENT_PADDING } from "./bpmn_viewbox.js";

// The canvas is stubbed: the only thing under test is the viewbox arithmetic,
// and bpmn-js cannot run in happy-dom.
const stub_canvas = ({ inner, scale = 1, width = 600, height = 400 }) => {
  const zoom = vi.fn();
  const set_viewbox = vi.fn();
  const canvas = {
    zoom,
    viewbox: vi.fn((box) => {
      if (box) {
        set_viewbox(box);
        return box;
      }
      return { x: 0, y: 0, width, height, scale, inner };
    }),
  };
  return { canvas, zoom, set_viewbox };
};

describe("fit_view_left", () => {
  it("fits the whole diagram first", () => {
    const { canvas, zoom } = stub_canvas({ inner: { x: 0, y: 0, width: 10, height: 10 } });
    fit_view_left(canvas);
    expect(zoom).toHaveBeenCalledWith("fit-viewport", "auto");
  });

  it("moves the left edge of the content to the padding", () => {
    const { canvas, set_viewbox } = stub_canvas({
      inner: { x: 300, y: 80, width: 200, height: 100 },
      scale: 1,
    });

    fit_view_left(canvas);

    // 300 - 12px at scale 1; the other three components are handed back
    // untouched, which is what keeps the zoom fit-viewport picked.
    expect(set_viewbox).toHaveBeenCalledWith({
      x: 300 - FIT_CONTENT_PADDING,
      y: 0,
      width: 600,
      height: 400,
    });
  });

  it("converts the padding through the current scale", () => {
    const { canvas, set_viewbox } = stub_canvas({
      inner: { x: 300, y: 80, width: 200, height: 100 },
      scale: 0.5,
    });

    fit_view_left(canvas);

    const box = set_viewbox.mock.lastCall[0];
    // (inner.x - box.x) * scale === padding, whatever the scale is.
    expect((300 - box.x) * 0.5).toBe(FIT_CONTENT_PADDING);
  });

  it("keeps the vertical placement fit-viewport chose", () => {
    const { canvas, set_viewbox } = stub_canvas({
      inner: { x: 40, y: 20, width: 100, height: 50 },
    });

    fit_view_left(canvas);

    expect(set_viewbox.mock.lastCall[0].y).toBe(0);
  });

  it("leaves an empty canvas alone", () => {
    const { canvas, set_viewbox } = stub_canvas({ inner: { x: 0, y: 0, width: 0, height: 0 } });
    fit_view_left(canvas);
    expect(set_viewbox).not.toHaveBeenCalled();
  });
});