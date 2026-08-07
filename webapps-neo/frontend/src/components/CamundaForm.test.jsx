import { describe, it, expect, vi, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// @bpmn-io/feelin pulls in a heavy parser that does not run cleanly in the test
// environment; stub `evaluate` so FEEL expressions resolve deterministically.
// `=true` -> true, `=false` -> false, otherwise the literal after `=`.
vi.mock("@bpmn-io/feelin", () => ({
  evaluate: (expr) => {
    if (expr === "true") return { value: true };
    if (expr === "false") return { value: false };
    return { value: expr };
  },
}));

import { CamundaForm } from "./CamundaForm.jsx";

const schema = (components) => ({ components });

describe("CamundaForm", () => {
  afterEach(cleanup);

  it("returns null when given no/invalid schema", () => {
    const { container } = render(<CamundaForm schema={null} />);
    expect(container.querySelector("form")).toBe(null);
  });

  it("renders a form with a field per component", () => {
    const { getByLabelText, getByText } = render(
      <CamundaForm
        schema={schema([
          { id: "f1", key: "name", type: "textfield", label: "Name" },
          { id: "f2", key: "age", type: "number", label: "Age" },
        ])}
        data={{ name: "Bob" }}
      />,
    );
    expect(getByLabelText("Name").value).toBe("Bob");
    expect(getByText("Age")).toBeTruthy();
  });

  it("calls on_ready with a submit control on mount", () => {
    const on_ready = vi.fn();
    render(
      <CamundaForm
        schema={schema([
          { id: "f1", key: "name", type: "textfield", label: "Name" },
        ])}
        on_ready={on_ready}
      />,
    );
    expect(on_ready).toHaveBeenCalled();
    expect(typeof on_ready.mock.lastCall[0].submit).toBe("function");
  });

  it("submits collected data via on_submit", () => {
    const on_submit = vi.fn();
    const { getByLabelText, container } = render(
      <CamundaForm
        schema={schema([
          { id: "f1", key: "name", type: "textfield", label: "Name" },
        ])}
        data={{ name: "Bob" }}
        on_submit={on_submit}
      />,
    );
    fireEvent.input(getByLabelText("Name"), { target: { value: "Alice" } });
    fireEvent.submit(container.querySelector("form"));
    expect(on_submit).toHaveBeenCalled();
    const arg = on_submit.mock.lastCall[0];
    expect(arg.data.name).toBe("Alice");
    expect(arg.errors).toEqual({});
  });

  it("reports a required-field error when empty on submit", () => {
    const on_submit = vi.fn();
    render(
      <CamundaForm
        schema={schema([
          {
            id: "f1",
            key: "name",
            type: "textfield",
            label: "Name",
            validate: { required: true },
          },
        ])}
        on_ready={({ submit }) => submit()}
        on_submit={on_submit}
      />,
    );
    expect(on_submit).toHaveBeenCalled();
    expect(on_submit.mock.lastCall[0].errors.name).toEqual(["required"]);
  });

  it("marks the required field with an asterisk", () => {
    const { getByText } = render(
      <CamundaForm
        schema={schema([
          {
            id: "f1",
            key: "name",
            type: "textfield",
            label: "Name",
            validate: { required: true },
          },
        ])}
      />,
    );
    expect(getByText("*")).toBeTruthy();
  });

  it("hides a field whose conditional.hide FEEL expression is truthy", () => {
    const { queryByLabelText } = render(
      <CamundaForm
        schema={schema([
          {
            id: "f1",
            key: "secret",
            type: "textfield",
            label: "Secret",
            conditional: { hide: "=true" },
          },
        ])}
      />,
    );
    expect(queryByLabelText("Secret")).toBe(null);
  });

  it("renders text components as markdown headings", () => {
    const { getByText } = render(
      <CamundaForm
        schema={schema([{ id: "t1", type: "text", text: "## Heading" }])}
      />,
    );
    const h = getByText("Heading");
    expect(h.tagName).toBe("H2");
  });

  it("falls back to an 'unsupported field' notice for unknown types", () => {
    const { getByText } = render(
      <CamundaForm
        schema={schema([{ id: "u1", key: "x", type: "mystery", label: "X" }])}
      />,
    );
    expect(getByText(/Unsupported field/)).toBeTruthy();
  });
});
