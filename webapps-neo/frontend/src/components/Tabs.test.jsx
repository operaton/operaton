import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";

const route = vi.fn();
let mockParams = {};
let mockPath = "/tasks/t1";
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route, path: mockPath }),
}));

import { Tabs } from "./Tabs.jsx";

const TabA = () => <p>content-a</p>;
const TabB = () => <p>content-b</p>;
const tabs = [
  { id: "form", name: "Form", pos: 0, Component: TabA },
  { id: "history", name: "History", pos: 1, Component: TabB },
];

describe("Tabs", () => {
  beforeEach(() => {
    mockParams = { tab: "form" };
    mockPath = "/tasks/t1";
    route.mockClear();
  });
  afterEach(cleanup);

  it("marks the active tab with aria-selected and renders its component", () => {
    const { getByText } = render(<Tabs base_url="/tasks/t1" tabs={tabs} />);
    expect(getByText("Form").getAttribute("aria-selected")).toBe("true");
    expect(getByText("History").getAttribute("aria-selected")).toBe("false");
    expect(getByText("content-a")).toBeTruthy();
  });

  it("renders each tab as a link to its url", () => {
    const { getByText } = render(<Tabs base_url="/tasks/t1" tabs={tabs} />);
    expect(getByText("History").getAttribute("href")).toBe("/tasks/t1/history");
  });

  it("ArrowRight moves to the next tab", () => {
    const { getByText } = render(<Tabs base_url="/tasks/t1" tabs={tabs} />);
    fireEvent.keyDown(getByText("Form"), { key: "ArrowRight" });
    expect(route).toHaveBeenCalledWith("/tasks/t1/history");
  });

  it("ArrowLeft from the first tab wraps to the last", () => {
    const { getByText } = render(<Tabs base_url="/tasks/t1" tabs={tabs} />);
    fireEvent.keyDown(getByText("Form"), { key: "ArrowLeft" });
    expect(route).toHaveBeenCalledWith("/tasks/t1/history");
  });

  it("redirects to the first tab when no tab is selected at the base url", () => {
    mockParams = {};
    mockPath = "/tasks/t1";
    render(<Tabs base_url="/tasks/t1" tabs={tabs} />);
    expect(route).toHaveBeenCalledWith("/tasks/t1/form");
  });
});
