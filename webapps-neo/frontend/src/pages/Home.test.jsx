import { describe, it, expect, afterEach } from "vitest";
import { render, cleanup } from "@testing-library/preact";
import { Home } from "./Home.jsx";

describe("Home", () => {
  afterEach(cleanup);

  it("renders the welcome heading and help/community links", () => {
    const { getByText, getByRole } = render(<Home />);
    expect(getByRole("heading", { level: 2 }).textContent).toBe("home.welcome");

    expect(getByText("home.forum").getAttribute("href")).toBe(
      "https://forum.operaton.org",
    );
    expect(getByText("home.github").getAttribute("href")).toBe(
      "https://github.com/operaton/operaton",
    );
    expect(getByText("home.bug-link").getAttribute("href")).toBe(
      "https://github.com/operaton/operaton/issues",
    );
  });
});
