import { describe, it, expect, vi, beforeEach } from "vitest";
import { h } from "preact";
import { render, fireEvent } from "@testing-library/preact";

// Spy every engine_rest function but keep RESPONSE_STATE real.
vi.mock("../api/engine_rest.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  const spyify = (o) =>
    Object.fromEntries(
      Object.entries(o).map(([k, v]) => [
        k,
        typeof v === "function"
          ? vi.fn()
          : v && typeof v === "object"
            ? spyify(v)
            : v,
      ]),
    );
  return { ...actual, default: spyify(actual.default) };
});

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { RESPONSE_STATE } from "../api/helper.jsx";
import { SetupPage } from "./Setup.jsx";
import { create_mock_state } from "../test/helpers.js";

const render_setup = (state, on_created = vi.fn()) =>
  render(h(AppState.Provider, { value: state }, h(SetupPage, { on_created })));

const fill = (container, values) => {
  for (const [selector, value] of Object.entries(values)) {
    fireEvent.input(container.querySelector(selector), { target: { value } });
  }
};

const complete_form = {
  "#setup-user-id": "admin",
  "#setup-password": "s3cret",
  "#setup-password-repeat": "s3cret",
  "#setup-first-name": "Ada",
  "#setup-last-name": "Lovelace",
  "#setup-email": "ada@example.com",
};

describe("pages/Setup", () => {
  let state;

  beforeEach(() => {
    state = create_mock_state();
    engine_rest.setup.create_initial_user.mockResolvedValue(undefined);
  });

  it("submits the profile and credentials the setup resource expects", () => {
    const { container } = render_setup(state);
    fill(container, complete_form);

    fireEvent.submit(container.querySelector("form"));

    expect(engine_rest.setup.create_initial_user).toHaveBeenCalled();
    const call = engine_rest.setup.create_initial_user.mock.lastCall;
    expect(call[0]).toBe(state);
    expect(call[1].profile).toEqual({
      id: "admin",
      firstName: "Ada",
      lastName: "Lovelace",
      email: "ada@example.com",
    });
    expect(call[1].credentials.password).toBe("s3cret");
  });

  it("does not submit when the passwords differ", () => {
    const { container, getByText } = render_setup(state);
    fill(container, {
      ...complete_form,
      "#setup-password-repeat": "different",
    });

    fireEvent.submit(container.querySelector("form"));

    expect(engine_rest.setup.create_initial_user).not.toHaveBeenCalled();
    expect(getByText("setup.password-mismatch")).toBeTruthy();
  });

  it("reports back once the administrator exists", async () => {
    const on_created = vi.fn();
    state.api.setup.create_initial_user.value = {
      status: RESPONSE_STATE.SUCCESS,
      data: "admin",
    };

    const { container } = render_setup(state, on_created);
    fill(container, complete_form);
    fireEvent.submit(container.querySelector("form"));

    await vi.waitFor(() => expect(on_created).toHaveBeenCalled());
  });

  it("surfaces a failure instead of reporting success", async () => {
    const on_created = vi.fn();
    state.api.setup.create_initial_user.value = {
      status: RESPONSE_STATE.ERROR,
      error: new Error("nope"),
    };

    const { container, getByText } = render_setup(state, on_created);
    fill(container, complete_form);
    fireEvent.submit(container.querySelector("form"));

    expect(getByText("setup.failed")).toBeTruthy();
    await vi.waitFor(() =>
      expect(engine_rest.setup.create_initial_user).toHaveBeenCalled(),
    );
    expect(on_created).not.toHaveBeenCalled();
  });

  it("labels every input so the form is usable without sight", () => {
    const { container } = render_setup(state);
    for (const input of container.querySelectorAll("input")) {
      expect(container.querySelector(`label[for="${input.id}"]`)).toBeTruthy();
    }
  });
});
