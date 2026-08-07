import { describe, it, expect, vi, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";
import { signal } from "@preact/signals";
import { Dialog, ConfirmDialog } from "./Dialog.jsx";

// Keep `open` false in tests so we don't depend on happy-dom's <dialog>.showModal();
// the dialog's children render regardless, so the handlers are still exercisable.
describe("Dialog", () => {
  afterEach(cleanup);

  it("renders the title and children", () => {
    const open = signal(false);
    const { getByText } = render(
      <Dialog open={open} title="My Title">
        <p>body content</p>
      </Dialog>,
    );
    expect(getByText("My Title")).toBeTruthy();
    expect(getByText("body content")).toBeTruthy();
  });

  it("closes (sets the open signal to false) when the close button is clicked", () => {
    const open = signal(true);
    const { getByLabelText } = render(
      <Dialog open={open} title="X">
        <p>x</p>
      </Dialog>,
    );
    fireEvent.click(getByLabelText("common.close"));
    expect(open.value).toBe(false);
  });
});

describe("ConfirmDialog", () => {
  afterEach(cleanup);

  it("renders the message and a default delete label", () => {
    const open = signal(false);
    const { getByText } = render(
      <ConfirmDialog
        open={open}
        message="Really delete?"
        on_confirm={vi.fn()}
      />,
    );
    expect(getByText("Really delete?")).toBeTruthy();
    expect(getByText("common.delete")).toBeTruthy();
  });

  it("uses a custom confirm label when provided", () => {
    const open = signal(false);
    const { getByText } = render(
      <ConfirmDialog
        open={open}
        message="m"
        confirm_label="common.remove"
        on_confirm={vi.fn()}
      />,
    );
    expect(getByText("common.remove")).toBeTruthy();
  });

  it("calls on_confirm and closes when the danger button is clicked", () => {
    const open = signal(true);
    const on_confirm = vi.fn();
    const { getByText } = render(
      <ConfirmDialog open={open} message="m" on_confirm={on_confirm} />,
    );
    fireEvent.click(getByText("common.delete"));
    expect(on_confirm).toHaveBeenCalledTimes(1);
    expect(open.value).toBe(false);
  });

  it("closes without confirming when cancel is clicked", () => {
    const open = signal(true);
    const on_confirm = vi.fn();
    const { getByText } = render(
      <ConfirmDialog open={open} message="m" on_confirm={on_confirm} />,
    );
    fireEvent.click(getByText("common.cancel"));
    expect(on_confirm).not.toHaveBeenCalled();
    expect(open.value).toBe(false);
  });
});
