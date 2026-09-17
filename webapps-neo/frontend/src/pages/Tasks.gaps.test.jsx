import { describe, it, expect } from "vitest";

/**
 * What the task list cannot do yet.
 *
 * Each of these marks a capability the web apps are expected to have and this
 * one has not. They are written as `it.fails`, so the suite stays green while
 * the gap is open and turns red the day it is closed — at which point the test
 * is already written and only has to lose its `.fails`.
 */
describe("not built yet", () => {
  describe("sorting by more than one criterion", () => {
    it.fails("keeps a second sort criterion", () => {
      const state_query = { sortBy: "created", sortOrder: "desc" };
      expect(state_query).toHaveProperty("sortings");
    });
  });
});
