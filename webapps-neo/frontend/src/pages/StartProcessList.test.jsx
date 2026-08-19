import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// Spy all engine_rest API functions but keep RequestState/RESPONSE_STATE real.
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

vi.mock("../components/Breadcrumbs.jsx", () => ({
  Breadcrumbs: () => h("nav", { "data-testid": "breadcrumbs" }),
}));

// Stub CamundaForm (avoids feelin), expose the schema it receives and wire up
// on_ready/on_submit so the page's Start button submits a valid form.
vi.mock("../components/CamundaForm.jsx", () => ({
  CamundaForm: ({ schema, data, on_submit, on_ready }) => {
    on_ready?.({ submit: () => on_submit?.({ data: data ?? {}, errors: {} }) });
    return h("div", { "data-testid": "camunda-form" }, JSON.stringify(schema));
  },
}));

let mockParams = {};
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: vi.fn(), path: "/tasks/start" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { StartProcessList } from "./StartProcessList.jsx";
import {
  create_mock_state,
  signal_response,
  signal_error,
} from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(StartProcessList, {})));

describe("StartProcessList", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = { tab: null };
    // The form dispatch chains start_form().then(...), so it must resolve.
    engine_rest.process_definition.start_form.mockResolvedValue(undefined);
  });
  afterEach(cleanup);

  it("fetches the startable process definitions on render", () => {
    renderPage(state);
    expect(engine_rest.process_definition.list_startable).toHaveBeenCalled();
    expect(engine_rest.process_definition.list_startable.mock.lastCall[0]).toBe(
      state,
    );
  });

  it("renders the startable process list with links to start each one", () => {
    signal_response(state.api.process.definition.list_startable, [
      { id: "p1", name: "Invoice", version: 1, description: "d", key: "inv" },
      {
        id: "p2",
        name: "Onboarding",
        version: 2,
        description: "x",
        key: "onb",
      },
    ]);
    const { getByText } = renderPage(state);
    expect(getByText("Invoice").getAttribute("href")).toBe("/tasks/start/p1");
    expect(getByText("Onboarding").getAttribute("href")).toBe(
      "/tasks/start/p2",
    );
  });

  it("filters the list by the search term", () => {
    signal_response(state.api.process.definition.list_startable, [
      { id: "p1", name: "Invoice", version: 1, description: "d", key: "inv" },
      {
        id: "p2",
        name: "Onboarding",
        version: 2,
        description: "x",
        key: "onb",
      },
    ]);
    const { getByText, queryByText, container } = renderPage(state);
    const input = container.querySelector("#process-popup-search-input");
    fireEvent.change(input, { target: { value: "invo" } });
    expect(getByText("Invoice")).toBeTruthy();
    expect(queryByText("Onboarding")).toBeNull();
  });

  it("prompts to select a definition when no tab is in the route", () => {
    mockParams = {};
    const { getByText } = renderPage(state);
    expect(getByText("tasks.start-process.select-definition")).toBeTruthy();
  });

  it("fetches the selected process definition when a tab is in the route", () => {
    mockParams = { tab: "p1" };
    renderPage(state);
    expect(engine_rest.process_definition.one).toHaveBeenCalled();
    expect(engine_rest.process_definition.one.mock.lastCall[1]).toBe("p1");
  });

  it("loads the start form once the selected definition resolves", () => {
    mockParams = { tab: "p1" };
    signal_response(state.api.process.definition.one, { id: "p1", key: "inv" });
    // The form dispatch fetches the start form metadata by definition id, then
    // resolves the source from its form key.
    signal_response(state.api.process.definition.start_form, {
      key: "embedded:app:inv",
    });
    renderPage(state);
    expect(engine_rest.process_definition.start_form).toHaveBeenCalled();
    expect(engine_rest.process_definition.start_form.mock.lastCall[1]).toBe(
      "p1",
    );
  });

  it("shows the migration notice for a legacy (embedded:app) start form", async () => {
    mockParams = { tab: "p1" };
    signal_response(state.api.process.definition.one, { id: "p1", key: "inv" });
    signal_response(state.api.process.definition.start_form, {
      key: "embedded:app:inv",
    });
    engine_rest.process_definition.start_form.mockResolvedValue(undefined);
    const { findByText } = renderPage(state);
    expect(await findByText(/legacy-html-unsupported/)).toBeTruthy();
  });

  it("renders a generated start form (embedded:engine key) through CamundaForm", async () => {
    mockParams = { tab: "p1" };
    signal_response(state.api.process.definition.one, { id: "p1", key: "kyc" });
    signal_response(state.api.process.definition.start_form, {
      key: "embedded:engine://engine/:engine/process-definition/p1/rendered-form",
    });
    engine_rest.process_definition.start_form.mockResolvedValue(undefined);
    // The dispatch resets rendered_form then fetches it; the mock populates it.
    engine_rest.process_definition.rendered_start_form.mockImplementation(() => {
      signal_response(
        state.api.process.definition.rendered_form,
        '<form><div class="form-group"><label>Full name</label>' +
          '<input cam-variable-name="fullName" cam-variable-type="String" type="text" value="Erika" required/>' +
          "</div></form>",
      );
    });
    const { findByTestId } = renderPage(state);
    const schema = JSON.parse((await findByTestId("camunda-form")).textContent);
    expect(schema.components.map((c) => c.key)).toEqual(["fullName"]);
    expect(schema.components[0].validate.required).toBe(true);
  });

  it("dispatches to the form-js start form on a formRef, not a form key", async () => {
    mockParams = { tab: "p1" };
    signal_response(state.api.process.definition.one, { id: "p1", key: "inv" });
    signal_response(state.api.process.definition.start_form, {
      key: null,
      operatonFormRef: { key: "claim-start", binding: "deployment" },
    });
    engine_rest.process_definition.start_form.mockResolvedValue(undefined);
    engine_rest.process_definition.get_deployed_start_form.mockImplementation(
      () =>
        signal_response(state.api.process.definition.deployed_start_form, {
          type: "default",
          components: [{ type: "textfield", key: "claimType" }],
        }),
    );
    const { findByTestId } = renderPage(state);
    const schema = JSON.parse((await findByTestId("camunda-form")).textContent);
    expect(schema.components.map((c) => c.key)).toEqual(["claimType"]);
    expect(
      engine_rest.process_definition.get_deployed_start_form.mock.lastCall[1],
    ).toBe("p1");
  });

  // The business key is a process-instance property, so no start form carries
  // it — every start mode has to offer it (regression: it went missing when the
  // hand-rolled start form was replaced by CamundaForm).
  describe("business key", () => {
    const generated_form = () => {
      signal_response(state.api.process.definition.start_form, {
        key: "embedded:engine://engine/:engine/process-definition/p1/rendered-form",
      });
      engine_rest.process_definition.rendered_start_form.mockImplementation(
        () =>
          signal_response(
            state.api.process.definition.rendered_form,
            '<form><div class="form-group"><label>Name</label>' +
              '<input cam-variable-name="name" cam-variable-type="String" type="text"/>' +
              "</div></form>",
          ),
      );
    };

    const form_js_form = () => {
      signal_response(state.api.process.definition.start_form, {
        key: null,
        operatonFormRef: { key: "claim-start", binding: "deployment" },
      });
      engine_rest.process_definition.get_deployed_start_form.mockImplementation(
        () =>
          signal_response(state.api.process.definition.deployed_start_form, {
            type: "default",
            components: [{ type: "textfield", key: "claimType" }],
          }),
      );
    };

    const no_form = () => {
      signal_response(state.api.process.definition.start_form, { key: null });
      engine_rest.process_definition.rendered_start_form.mockImplementation(
        () => signal_error(state.api.process.definition.rendered_form),
      );
    };

    beforeEach(() => {
      mockParams = { tab: "p1" };
      signal_response(state.api.process.definition.one, { id: "p1" });
      engine_rest.process_definition.start_form.mockResolvedValue(undefined);
    });

    it.each([
      ["a generated form", generated_form],
      ["a form-js form", form_js_form],
      ["no form at all", no_form],
    ])("offers the business key input for %s", async (_label, setup) => {
      setup();
      const { findByTestId, container } = renderPage(state);
      await findByTestId("camunda-form");
      const input = container.querySelector('input[name="business_key"]');
      expect(input).toBeTruthy();
      expect(
        container.querySelector("label.business-key").textContent,
      ).toContain("tasks.start-process.business-key");
    });

    it.each([
      ["a generated form", generated_form],
      ["a form-js form", form_js_form],
    ])("submits the typed business key alongside the variables for %s",
      async (_label, setup) => {
        setup();
        engine_rest.process_definition.submit_form.mockResolvedValue({
          status: "SUCCESS",
        });
        const { findByTestId, container, getByText } = renderPage(state);
        await findByTestId("camunda-form");
        fireEvent.input(container.querySelector('input[name="business_key"]'), {
          target: { value: "BK-42" },
        });
        fireEvent.click(getByText("tasks.start-process.start"));
        const payload =
          engine_rest.process_definition.submit_form.mock.lastCall[2];
        expect(payload.businessKey).toBe("BK-42");
        expect(payload.variables).toBeTruthy();
      });

    it("omits businessKey from the payload when left empty", async () => {
      generated_form();
      engine_rest.process_definition.submit_form.mockResolvedValue({
        status: "SUCCESS",
      });
      const { findByTestId, getByText } = renderPage(state);
      await findByTestId("camunda-form");
      fireEvent.click(getByText("tasks.start-process.start"));
      const payload =
        engine_rest.process_definition.submit_form.mock.lastCall[2];
      expect("businessKey" in payload).toBe(false);
    });
  });
});
