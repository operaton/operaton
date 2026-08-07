import { describe, it, expect, afterEach } from "vitest";
import { render, cleanup } from "@testing-library/preact";
import { Breadcrumbs } from "./Breadcrumbs.jsx";

describe("Breadcrumbs", () => {
  afterEach(cleanup);

  const paths = [
    { name: "Home", route: "/" },
    { name: "Processes", route: "/processes" },
    { name: "Invoice", route: "/processes/invoice" },
  ];

  it("renders all but the last path as links", () => {
    const { getByText } = render(<Breadcrumbs paths={paths} />);
    expect(getByText("Home").closest("a").getAttribute("href")).toBe("/");
    expect(getByText("Processes").closest("a").getAttribute("href")).toBe(
      "/processes",
    );
  });

  it("renders the last path as plain text, not a link", () => {
    const { getByText } = render(<Breadcrumbs paths={paths} />);
    const last = getByText("Invoice");
    expect(last.tagName).toBe("SPAN");
    expect(last.closest("a")).toBeNull();
  });

  it("renders a single path as just the current crumb", () => {
    const { getByText, container } = render(
      <Breadcrumbs paths={[{ name: "Only", route: "/only" }]} />,
    );
    expect(getByText("Only").tagName).toBe("SPAN");
    expect(container.querySelectorAll("a")).toHaveLength(0);
  });
});
