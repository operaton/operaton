import { describe, it, expect, afterEach } from "vitest";
import { render, cleanup } from "@testing-library/preact";
import { NotFound } from "./_404.jsx";

// react-i18next is globally mocked in src/test/setup.js so t(key) returns key.
describe("NotFound (_404)", () => {
  afterEach(cleanup);

  it("renders the not-found title and text", () => {
    const { getByText, getByRole } = render(<NotFound />);
    expect(getByRole("heading")).toHaveProperty(
      "textContent",
      "not-found.title",
    );
    expect(getByText("not-found.text")).toBeTruthy();
  });

  it("hints at the ALT + K global search shortcut", () => {
    const { getByText } = render(<NotFound />);
    expect(getByText("ALT + K")).toBeTruthy();
  });
});
