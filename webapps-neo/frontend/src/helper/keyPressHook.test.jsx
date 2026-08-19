import { describe, it, expect, vi, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";
import { useKeyPress } from "./keyPressHook.jsx";

const Probe = ({ keys, cb, modifier }) => {
  useKeyPress(keys, cb, modifier);
  return <div>probe</div>;
};

describe("useKeyPress", () => {
  afterEach(cleanup);

  it("invokes the callback when a watched key is pressed (with a modifier set)", () => {
    const cb = vi.fn();
    render(<Probe keys={["k"]} cb={cb} modifier={["Alt"]} />);

    fireEvent.keyDown(document, { key: "k" });
    expect(cb).toHaveBeenCalledTimes(1);
  });

  it("ignores keys that are not in the watched list", () => {
    const cb = vi.fn();
    render(<Probe keys={["k"]} cb={cb} modifier={["Alt"]} />);

    fireEvent.keyDown(document, { key: "j" });
    expect(cb).not.toHaveBeenCalled();
  });

  it("does not fire when no modifier is configured (current guard behavior)", () => {
    const cb = vi.fn();
    render(<Probe keys={["k"]} cb={cb} modifier={[]} />);

    fireEvent.keyDown(document, { key: "k" });
    expect(cb).not.toHaveBeenCalled();
  });

  it("removes its listener on unmount", () => {
    const cb = vi.fn();
    const { unmount } = render(
      <Probe keys={["k"]} cb={cb} modifier={["Alt"]} />,
    );
    unmount();

    fireEvent.keyDown(document, { key: "k" });
    expect(cb).not.toHaveBeenCalled();
  });
});
