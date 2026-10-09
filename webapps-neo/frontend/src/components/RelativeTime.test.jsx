import { describe, it, expect, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";
import { RelativeTime } from "./RelativeTime.jsx";

const WHEN = "2026-07-01T12:00:00.000+0200";

describe("RelativeTime", () => {
  afterEach(cleanup);

  it("reads as a relative time and keeps the machine value", () => {
    const { container } = render(<RelativeTime datetime={WHEN} />);
    const time = container.querySelector("time");
    expect(time.getAttribute("datetime")).toBe(WHEN);
    expect(time.textContent.length).toBeGreaterThan(0);
  });

  it("carries no title, so no browser delay decides when it shows", () => {
    const { container } = render(<RelativeTime datetime={WHEN} />);
    expect(container.querySelector("time").getAttribute("title")).toBeNull();
  });

  it("shows the exact moment on hover and drops it again on leave", () => {
    const { container } = render(<RelativeTime datetime={WHEN} />);
    const time = container.querySelector("time");
    expect(container.querySelector(".time-tooltip")).toBeNull();

    fireEvent.mouseEnter(time);
    const tip = container.querySelector(".time-tooltip");
    expect(tip).toBeTruthy();
    expect(tip.getAttribute("role")).toBe("tooltip");
    expect(tip.textContent).toContain("2026");

    fireEvent.mouseLeave(time);
    expect(container.querySelector(".time-tooltip")).toBeNull();
  });

  it("positions itself against the viewport, not the row", () => {
    const { container } = render(<RelativeTime datetime={WHEN} />);
    const time = container.querySelector("time");
    time.getBoundingClientRect = () => ({ left: 100, width: 40, top: 200 });

    fireEvent.mouseEnter(time);
    const style = container
      .querySelector(".time-tooltip")
      .getAttribute("style");
    expect(style).toContain("left: 120px");
    expect(style).toContain("top: 200px");
  });
});
