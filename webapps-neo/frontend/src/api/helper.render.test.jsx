import { describe, it, expect, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup } from "@testing-library/preact";
import { signal } from "@preact/signals";

import { RequestState, RESPONSE_STATE } from "./helper.jsx";

const in_table = (node) =>
  render(
    <table>
      <thead>
        <tr>
          <th scope="col">a</th>
          <th scope="col">b</th>
        </tr>
      </thead>
      <tbody>{node}</tbody>
    </table>,
  );

describe("api/helper RequestState", () => {
  afterEach(cleanup);

  it("renders a status paragraph without cell_span", () => {
    const state = signal({ status: RESPONSE_STATE.LOADING });
    const { container } = render(
      <RequestState signal={state} on_success={() => null} />,
    );

    expect(container.querySelector("p")).not.toBeNull();
  });

  // Column headers need data cells to describe (WCAG 1.3.1).
  it("renders the status as a table row with cell_span", () => {
    const state = signal({ status: RESPONSE_STATE.LOADING });
    const { container } = in_table(
      <RequestState signal={state} cell_span={2} on_success={() => null} />,
    );

    const cell = container.querySelector("tbody > tr > td");
    expect(cell).not.toBeNull();
    expect(cell.getAttribute("colspan")).toBe("2");
    expect(container.querySelector("tbody > p")).toBeNull();
  });

  it("wraps the error status too", () => {
    const state = signal({
      status: RESPONSE_STATE.ERROR,
      error: new Error("boom"),
    });
    const { container } = in_table(
      <RequestState signal={state} cell_span={2} on_success={() => null} />,
    );

    expect(container.querySelector("tbody > tr > td")).not.toBeNull();
    expect(container.textContent).toContain("boom");
  });

  it("leaves rendered rows untouched", () => {
    const state = signal({ status: RESPONSE_STATE.SUCCESS, data: [1] });
    const { container } = in_table(
      <RequestState
        signal={state}
        cell_span={2}
        on_success={() => (
          <tr>
            <td>one</td>
            <td>two</td>
          </tr>
        )}
      />,
    );

    expect(container.querySelectorAll("tbody > tr")).toHaveLength(1);
    expect(container.querySelector("td[colspan]")).toBeNull();
  });
});
