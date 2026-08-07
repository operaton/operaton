import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

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

let mockParams = {};
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: vi.fn(), path: "/batches" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { BatchesPage } from "./Batches.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(BatchesPage, {})));

const sample_batch = (over = {}) => ({
  id: "batch-1234abcd",
  type: "instance-modification",
  totalJobs: 10,
  completedJobs: 4,
  remainingJobs: 6,
  failedJobs: 1,
  suspended: false,
  createUserId: "demo",
  ...over,
});

describe("BatchesPage", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
  });
  afterEach(cleanup);

  it("fetches the batch list on mount", () => {
    renderPage(state);
    expect(engine_rest.batch.all).toHaveBeenCalled();
  });

  it("renders an empty state when there are no batches", () => {
    signal_response(state.api.batch.list, []);
    const { getByText } = renderPage(state);
    expect(getByText("batches.empty")).toBeTruthy();
  });

  it("renders a row per batch linking to its detail", () => {
    signal_response(state.api.batch.list, [sample_batch()]);
    const { getByText } = renderPage(state);
    const link = getByText("batch-12"); // first 8 chars of the id
    expect(link.getAttribute("href")).toBe("/batches/batch-1234abcd");
  });

  it("fetches the selected batch when a batch_id is in the route", () => {
    mockParams = { batch_id: "batch-1234abcd" };
    renderPage(state);
    expect(engine_rest.batch.one).toHaveBeenCalled();
  });

  it("renders batch detail and a Suspend action for a running batch", () => {
    mockParams = { batch_id: "batch-1234abcd" };
    signal_response(state.api.batch.one, [sample_batch()]);
    const { getByText } = renderPage(state);
    expect(getByText("batches.suspend")).toBeTruthy();
  });

  it("suspends a running batch via set_suspended(true)", () => {
    mockParams = { batch_id: "batch-1234abcd" };
    signal_response(state.api.batch.one, [sample_batch()]);
    engine_rest.batch.set_suspended.mockResolvedValue(undefined);
    const { getByText } = renderPage(state);
    fireEvent.click(getByText("batches.suspend"));
    expect(engine_rest.batch.set_suspended).toHaveBeenCalled();
    const [st, id, suspended] = engine_rest.batch.set_suspended.mock.lastCall;
    expect(st).toBe(state);
    expect(id).toBe("batch-1234abcd");
    expect(suspended).toBe(true);
  });

  it("offers Resume for a suspended batch", () => {
    mockParams = { batch_id: "batch-1234abcd" };
    signal_response(state.api.batch.one, [sample_batch({ suspended: true })]);
    engine_rest.batch.set_suspended.mockResolvedValue(undefined);
    const { getByText } = renderPage(state);
    fireEvent.click(getByText("batches.resume"));
    expect(engine_rest.batch.set_suspended.mock.lastCall[2]).toBe(false);
  });

  it("deletes a batch after confirming in the dialog", () => {
    mockParams = { batch_id: "batch-1234abcd" };
    signal_response(state.api.batch.one, [sample_batch()]);
    engine_rest.batch.delete.mockResolvedValue(undefined);
    const { getAllByText } = renderPage(state);

    // Both the row "Delete" button and the ConfirmDialog's danger button reuse
    // the "batches.delete" label; the row button is first, the confirm last.
    const deleteButtons = getAllByText("batches.delete");
    fireEvent.click(deleteButtons[0]); // open the confirm dialog
    fireEvent.click(deleteButtons[deleteButtons.length - 1]); // confirm

    expect(engine_rest.batch.delete).toHaveBeenCalled();
    expect(engine_rest.batch.delete.mock.lastCall[1]).toBe("batch-1234abcd");
  });
});
