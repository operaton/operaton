import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";

const route = vi.fn();
let mockParams = {};
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route, path: "/processes/p1" }),
}));

import { Accordion } from "./Accordion.jsx";

const sections = [
  { id: "a", name: "Alpha", target: <p>panel-a</p> },
  { id: "b", name: "Beta", target: <p>panel-b</p> },
];

describe("Accordion", () => {
  beforeEach(() => {
    mockParams = {};
    route.mockClear();
  });
  afterEach(cleanup);

  it("renders a details/summary per section with the panel content", () => {
    const { getByText } = render(
      <Accordion
        sections={sections}
        accordion_name="acc"
        base_path="/processes/p1"
      />,
    );
    expect(getByText("Alpha")).toBeTruthy();
    expect(getByText("panel-b")).toBeTruthy();
  });

  it("opens the section matching the active panel param", () => {
    mockParams = { panel: "b" };
    const { getByText } = render(
      <Accordion
        sections={sections}
        accordion_name="acc"
        base_path="/processes/p1"
      />,
    );
    const detailsB = getByText("Beta").closest("details");
    const detailsA = getByText("Alpha").closest("details");
    expect(detailsB.hasAttribute("open")).toBe(true);
    expect(detailsA.hasAttribute("open")).toBe(false);
  });

  it("navigates to the section path when a summary is clicked", () => {
    const { getByText } = render(
      <Accordion
        sections={sections}
        accordion_name="acc"
        base_path="/processes/p1"
      />,
    );
    fireEvent.click(getByText("Beta"));
    expect(route).toHaveBeenCalledWith("/processes/p1/b");
  });

  it("supports a custom param_name", () => {
    mockParams = { sub_panel: "a" };
    const { getByText } = render(
      <Accordion
        sections={sections}
        accordion_name="acc"
        param_name="sub_panel"
        base_path="/x"
      />,
    );
    expect(getByText("Alpha").closest("details").hasAttribute("open")).toBe(
      true,
    );
  });
});
