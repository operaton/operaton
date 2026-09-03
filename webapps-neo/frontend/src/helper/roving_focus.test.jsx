import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/preact";

import { useRovingFocus } from "./roving_focus.js";

const Menu = ({ current = "/tasks", ...options }) => {
  const roving = useRovingFocus(options);
  return (
    <nav>
      <menu {...roving}>
        {["/", "/tasks", "/processes"].map((href) => (
          <li key={href}>
            <a href={href} aria-current={href === current ? "page" : undefined}>
              {href}
            </a>
          </li>
        ))}
      </menu>
    </nav>
  );
};

const Table = ({ selected = null, ...options }) => {
  const roving = useRovingFocus({ mode: "rows", ...options });
  return (
    <table {...roving}>
      <tbody>
        {["a", "b", "c"].map((id) => (
          <tr key={id} aria-selected={id === selected ? "true" : undefined}>
            <td>
              <input type="checkbox" aria-label={`select ${id}`} />
            </td>
            <th scope="row">
              <a href={`/row/${id}`}>row {id}</a>
            </th>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

const tabindex_of = (element) => element.getAttribute("tabindex");

describe("useRovingFocus — list mode", () => {
  it("leaves only the current entry in the tab order", () => {
    render(<Menu current="/tasks" />);
    expect(tabindex_of(screen.getByRole("link", { name: "/tasks" }))).toBe(
      null,
    );
    expect(tabindex_of(screen.getByRole("link", { name: "/" }))).toBe("-1");
    expect(tabindex_of(screen.getByRole("link", { name: "/processes" }))).toBe(
      "-1",
    );
  });

  it("falls back to the first entry when nothing is current", () => {
    render(<Menu current={null} />);
    expect(tabindex_of(screen.getByRole("link", { name: "/" }))).toBe(null);
    expect(tabindex_of(screen.getByRole("link", { name: "/tasks" }))).toBe(
      "-1",
    );
  });

  it("moves forward and back with the arrow keys", () => {
    render(<Menu current="/" />);
    const first = screen.getByRole("link", { name: "/" });
    first.focus();

    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(document.activeElement).toBe(
      screen.getByRole("link", { name: "/tasks" }),
    );

    fireEvent.keyDown(document.activeElement, { key: "ArrowUp" });
    expect(document.activeElement).toBe(first);
  });

  it("wraps around the ends", () => {
    render(<Menu current="/" />);
    const first = screen.getByRole("link", { name: "/" });
    first.focus();

    fireEvent.keyDown(first, { key: "ArrowUp" });
    expect(document.activeElement).toBe(
      screen.getByRole("link", { name: "/processes" }),
    );
  });

  it("stops at the ends when wrapping is off", () => {
    render(<Menu current="/" wrap={false} />);
    const first = screen.getByRole("link", { name: "/" });
    first.focus();

    fireEvent.keyDown(first, { key: "ArrowUp" });
    expect(document.activeElement).toBe(first);
  });

  it("jumps to the first and last entry with Home and End", () => {
    render(<Menu current="/tasks" />);
    const middle = screen.getByRole("link", { name: "/tasks" });
    middle.focus();

    fireEvent.keyDown(middle, { key: "End" });
    expect(document.activeElement).toBe(
      screen.getByRole("link", { name: "/processes" }),
    );

    fireEvent.keyDown(document.activeElement, { key: "Home" });
    expect(document.activeElement).toBe(
      screen.getByRole("link", { name: "/" }),
    );
  });

  it("honours a horizontal orientation", () => {
    render(<Menu current="/" orientation="horizontal" />);
    const first = screen.getByRole("link", { name: "/" });
    first.focus();

    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(document.activeElement).toBe(first); // vertical keys ignored

    fireEvent.keyDown(first, { key: "ArrowRight" });
    expect(document.activeElement).toBe(
      screen.getByRole("link", { name: "/tasks" }),
    );
  });
});

describe("useRovingFocus — rows mode", () => {
  it("leaves only the selected row in the tab order, controls and all", () => {
    render(<Table selected="b" />);
    const link_b = screen.getByRole("link", { name: "row b" }),
      link_a = screen.getByRole("link", { name: "row a" }),
      box_b = screen.getByRole("checkbox", { name: "select b" }),
      box_a = screen.getByRole("checkbox", { name: "select a" });

    // The whole selected row stays reachable, so Tab still moves between the
    // checkbox and the link once you are in it.
    expect(tabindex_of(link_b)).toBe(null);
    expect(tabindex_of(box_b)).toBe(null);
    expect(tabindex_of(link_a)).toBe("-1");
    expect(tabindex_of(box_a)).toBe("-1");
  });

  it("moves between rows, not between controls within a row", () => {
    render(<Table selected="a" />);
    const link_a = screen.getByRole("link", { name: "row a" });
    link_a.focus();

    fireEvent.keyDown(link_a, { key: "ArrowDown" });
    // Lands on the next ROW's first control, skipping row a's own link/checkbox.
    expect(document.activeElement).toBe(
      screen.getByRole("checkbox", { name: "select b" }),
    );
  });

  it("moves rows even when focus is on a control inside the row", () => {
    render(<Table selected="a" />);
    const box_a = screen.getByRole("checkbox", { name: "select a" });
    box_a.focus();

    fireEvent.keyDown(box_a, { key: "ArrowDown" });
    expect(document.activeElement).toBe(
      screen.getByRole("checkbox", { name: "select b" }),
    );
  });

  it("leaves the arrow keys to controls that use them", () => {
    const WithField = () => {
      const roving = useRovingFocus({ mode: "rows" });
      return (
        <table {...roving}>
          <tbody>
            <tr>
              <td>
                <input aria-label="search" type="search" />
              </td>
            </tr>
            <tr>
              <td>
                <a href="/row/b">row b</a>
              </td>
            </tr>
          </tbody>
        </table>
      );
    };
    render(<WithField />);
    const field = screen.getByRole("searchbox", { name: "search" });
    field.focus();
    fireEvent.keyDown(field, { key: "ArrowDown" });
    // A text field keeps its own caret movement.
    expect(document.activeElement).toBe(field);
  });

  it("does nothing when the table is empty", () => {
    const Empty = () => {
      const roving = useRovingFocus({ mode: "rows" });
      return (
        <table {...roving}>
          <tbody />
        </table>
      );
    };
    expect(() => render(<Empty />)).not.toThrow();
  });
});
