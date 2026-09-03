import { beforeEach, describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/preact";
import { LocationProvider } from "preact-iso";

import {
  DetailHeading,
  PageHeading,
  mark_initial_navigation_done,
  page_of,
  reset_focus_tracking,
} from "./Heading.jsx";

// The module tracks "have we navigated yet" and "which page were we on" outside
// any component, because a page's <h1> unmounts with its page. Every test has
// to start from a clean slate.
beforeEach(() => reset_focus_tracking());

const at = (path) => {
  window.history.replaceState(null, "", path);
  return ({ children }) => <LocationProvider>{children}</LocationProvider>;
};

describe("page_of", () => {
  it("reduces a path to the page it belongs to", () => {
    expect(page_of("/tasks")).toBe("tasks");
    expect(page_of("/tasks/abc-123/form")).toBe("tasks");
    expect(page_of("/")).toBe("");
  });

  it("treats a deep selection as the same page as its list", () => {
    // This is the whole reason PageHeading does not key on the full path:
    // selecting a task must not drag focus back up to the page heading.
    expect(page_of("/tasks")).toBe(page_of("/tasks/abc-123/form"));
    expect(page_of("/processes")).toBe(
      page_of("/processes/def:1:xyz/instances/abc/vars"),
    );
  });
});

describe("PageHeading", () => {
  it("renders an h1 that is focusable but not in the tab order", () => {
    render(<PageHeading>Tasks</PageHeading>, { wrapper: at("/tasks") });
    const heading = screen.getByRole("heading", { level: 1 });
    expect(heading.tagName).toBe("H1");
    expect(heading.getAttribute("tabindex")).toBe("-1");
  });

  it("does not take focus on the first render", () => {
    // On load the skip link has to stay the first Tab stop.
    render(<PageHeading>Tasks</PageHeading>, { wrapper: at("/tasks") });
    expect(document.activeElement).not.toBe(
      screen.getByRole("heading", { level: 1 }),
    );
  });

  it("takes focus once the page changes", async () => {
    const { unmount } = render(<PageHeading>Tasks</PageHeading>, {
      wrapper: at("/tasks"),
    });
    mark_initial_navigation_done();
    unmount();

    render(<PageHeading>Processes</PageHeading>, { wrapper: at("/processes") });
    await waitFor(() =>
      expect(document.activeElement).toBe(
        screen.getByRole("heading", { level: 1 }),
      ),
    );
  });

  it("stays put when only the selection below the page changes", async () => {
    const { unmount } = render(<PageHeading>Tasks</PageHeading>, {
      wrapper: at("/tasks"),
    });
    mark_initial_navigation_done();
    unmount();

    render(<PageHeading>Tasks</PageHeading>, {
      wrapper: at("/tasks/abc-123/form"),
    });
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(document.activeElement).not.toBe(
      screen.getByRole("heading", { level: 1 }),
    );
  });

  it("passes class through so a page can hide it visually", () => {
    render(<PageHeading class="screen-hidden">Tasks</PageHeading>, {
      wrapper: at("/tasks"),
    });
    expect(
      screen
        .getByRole("heading", { level: 1 })
        .classList.contains("screen-hidden"),
    ).toBe(true);
  });
});

describe("DetailHeading", () => {
  it("renders an h2 by default and honours an explicit level", () => {
    const { unmount } = render(
      <DetailHeading focus_key="a">One</DetailHeading>,
    );
    expect(screen.getByRole("heading").tagName).toBe("H2");
    unmount();

    render(
      <DetailHeading focus_key="a" level={3}>
        One
      </DetailHeading>,
    );
    expect(screen.getByRole("heading").tagName).toBe("H3");
  });

  it("does not take focus on the first render", () => {
    render(<DetailHeading focus_key="task-1">Order 4711</DetailHeading>);
    expect(document.activeElement).not.toBe(screen.getByRole("heading"));
  });

  it("takes focus when the selected entry changes", async () => {
    mark_initial_navigation_done();
    const { rerender } = render(
      <DetailHeading focus_key="task-1">Order 4711</DetailHeading>,
    );
    await waitFor(() =>
      expect(document.activeElement).toBe(screen.getByRole("heading")),
    );

    screen.getByRole("heading").blur();
    rerender(<DetailHeading focus_key="task-2">Order 4712</DetailHeading>);
    await waitFor(() =>
      expect(document.activeElement).toBe(screen.getByRole("heading")),
    );
  });

  it("does not re-take focus while the same entry re-renders", async () => {
    mark_initial_navigation_done();
    const { rerender } = render(
      <DetailHeading focus_key="task-1">Order 4711</DetailHeading>,
    );
    await waitFor(() =>
      expect(document.activeElement).toBe(screen.getByRole("heading")),
    );

    screen.getByRole("heading").blur();
    rerender(
      <DetailHeading focus_key="task-1">Order 4711 (updated)</DetailHeading>,
    );
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(document.activeElement).not.toBe(screen.getByRole("heading"));
  });

  it("waits for the heading text before focusing", async () => {
    // Detail headings are filled from an async fetch. Focusing an empty
    // heading announces nothing, so it must wait for the render with content.
    mark_initial_navigation_done();
    const { rerender } = render(
      <DetailHeading focus_key="task-1">{undefined}</DetailHeading>,
    );
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(document.activeElement).not.toBe(screen.getByRole("heading"));

    rerender(<DetailHeading focus_key="task-1">Order 4711</DetailHeading>);
    await waitFor(() =>
      expect(document.activeElement).toBe(screen.getByRole("heading")),
    );
  });

  it("does nothing without a focus key", async () => {
    mark_initial_navigation_done();
    render(
      <DetailHeading focus_key={undefined}>Nothing selected</DetailHeading>,
    );
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(document.activeElement).not.toBe(screen.getByRole("heading"));
  });
});
