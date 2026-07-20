import { describe, it, expect } from "vitest";
import { signal } from "@preact/signals";
import {
  build_migration_plan,
  build_migration_plan_with_variables,
  add_variable,
  remove_variable,
  update_variable,
  update_mapping,
  add_query_params_abstract,
} from "./migration_helpers.js";

// --- Tests ---

describe("Migrations", () => {
  describe("build_migration_plan", () => {
    const generate_data = {
      sourceProcessDefinitionId: "invoice:2:abc",
      targetProcessDefinitionId: "invoice:1:def",
      instructions: [],
      variables: {},
    };

    it("should build instructions from mappings", () => {
      const mappings = {
        approveInvoice: "approveInvoice",
        reviewInvoice: "reviewInvoice",
      };
      const plan = build_migration_plan(generate_data, mappings);

      expect(plan.instructions).toEqual([
        {
          sourceActivityIds: ["approveInvoice"],
          targetActivityIds: ["approveInvoice"],
          updateEventTrigger: false,
        },
        {
          sourceActivityIds: ["reviewInvoice"],
          targetActivityIds: ["reviewInvoice"],
          updateEventTrigger: false,
        },
      ]);
    });

    it("should preserve source and target definition IDs", () => {
      const plan = build_migration_plan(generate_data, {});
      expect(plan.sourceProcessDefinitionId).toBe("invoice:2:abc");
      expect(plan.targetProcessDefinitionId).toBe("invoice:1:def");
    });

    it("should produce empty instructions from empty mappings", () => {
      const plan = build_migration_plan(generate_data, {});
      expect(plan.instructions).toEqual([]);
    });

    it("should not mutate the original generate data", () => {
      const original = { ...generate_data, instructions: [{ id: "original" }] };
      const plan = build_migration_plan(original, { a: "b" });
      expect(original.instructions).toEqual([{ id: "original" }]);
      expect(plan.instructions).toHaveLength(1);
      expect(plan.instructions[0].sourceActivityIds).toEqual(["a"]);
    });
  });

  describe("build_migration_plan_with_variables", () => {
    const generate_data = {
      sourceProcessDefinitionId: "invoice:2:abc",
      targetProcessDefinitionId: "invoice:1:def",
      instructions: [],
      variables: {},
    };

    it("should include variables in the plan", () => {
      const variables = [
        { name: "Test", type: "String", value: "Hello World" },
        { name: "Count", type: "Integer", value: "42" },
      ];
      const plan = build_migration_plan_with_variables(
        generate_data,
        {},
        variables,
      );
      expect(plan.variables).toEqual({
        Test: { type: "String", value: "Hello World" },
        Count: { type: "Integer", value: "42" },
      });
    });

    it("should skip variables with empty names", () => {
      const variables = [
        { name: "", type: "String", value: "ignored" },
        { name: "  ", type: "String", value: "also ignored" },
        { name: "Valid", type: "String", value: "kept" },
      ];
      const plan = build_migration_plan_with_variables(
        generate_data,
        {},
        variables,
      );
      expect(Object.keys(plan.variables)).toEqual(["Valid"]);
    });

    it("should produce empty variables from empty list", () => {
      const plan = build_migration_plan_with_variables(generate_data, {}, []);
      expect(plan.variables).toEqual({});
    });

    it("should include both instructions and variables", () => {
      const mappings = { taskA: "taskB" };
      const variables = [{ name: "env", type: "String", value: "prod" }];
      const plan = build_migration_plan_with_variables(
        generate_data,
        mappings,
        variables,
      );
      expect(plan.instructions).toHaveLength(1);
      expect(plan.instructions[0].sourceActivityIds).toEqual(["taskA"]);
      expect(plan.variables).toEqual({
        env: { type: "String", value: "prod" },
      });
    });
  });

  describe("variable management", () => {
    it("should add a variable with defaults", () => {
      const state = { variables: signal([]) };
      add_variable(state);
      expect(state.variables.value).toEqual([
        { name: "", type: "String", value: "" },
      ]);
    });

    it("should append to existing variables", () => {
      const state = {
        variables: signal([{ name: "a", type: "Integer", value: "1" }]),
      };
      add_variable(state);
      expect(state.variables.value).toHaveLength(2);
      expect(state.variables.value[0]).toEqual({
        name: "a",
        type: "Integer",
        value: "1",
      });
      expect(state.variables.value[1]).toEqual({
        name: "",
        type: "String",
        value: "",
      });
    });

    it("should remove a variable by index", () => {
      const state = {
        variables: signal([
          { name: "a", type: "String", value: "1" },
          { name: "b", type: "String", value: "2" },
          { name: "c", type: "String", value: "3" },
        ]),
      };
      remove_variable(state, 1);
      expect(state.variables.value).toEqual([
        { name: "a", type: "String", value: "1" },
        { name: "c", type: "String", value: "3" },
      ]);
    });

    it("should remove the only variable", () => {
      const state = {
        variables: signal([{ name: "a", type: "String", value: "1" }]),
      };
      remove_variable(state, 0);
      expect(state.variables.value).toEqual([]);
    });

    it("should update a variable field", () => {
      const state = {
        variables: signal([{ name: "a", type: "String", value: "1" }]),
      };
      update_variable(state, 0, "name", "renamed");
      expect(state.variables.value[0].name).toBe("renamed");
      expect(state.variables.value[0].type).toBe("String");
      expect(state.variables.value[0].value).toBe("1");
    });

    it("should update type without affecting other fields", () => {
      const state = {
        variables: signal([{ name: "a", type: "String", value: "1" }]),
      };
      update_variable(state, 0, "type", "Boolean");
      expect(state.variables.value[0]).toEqual({
        name: "a",
        type: "Boolean",
        value: "1",
      });
    });

    it("should replace signal value (not mutate in place)", () => {
      const state = {
        variables: signal([{ name: "a", type: "String", value: "" }]),
      };
      const before = state.variables.value;
      update_variable(state, 0, "name", "b");
      expect(state.variables.value).not.toBe(before);
    });
  });

  describe("update_mapping", () => {
    it("should add a mapping", () => {
      const result = update_mapping("targetTask", "sourceTask", {});
      expect(result).toEqual({ sourceTask: "targetTask" });
    });

    it("should update an existing mapping", () => {
      const mappings = { sourceTask: "oldTarget" };
      const result = update_mapping("newTarget", "sourceTask", mappings);
      expect(result).toEqual({ sourceTask: "newTarget" });
    });

    it("should remove a mapping when value is empty", () => {
      const mappings = { taskA: "targetA", taskB: "targetB" };
      const result = update_mapping("", "taskA", mappings);
      expect(result).toEqual({ taskB: "targetB" });
    });

    it("should return empty object when removing the only mapping", () => {
      const mappings = { taskA: "targetA" };
      const result = update_mapping("", "taskA", mappings);
      expect(result).toEqual({});
    });

    it("should not mutate the original mappings", () => {
      const mappings = { taskA: "targetA" };
      update_mapping("targetB", "taskB", mappings);
      expect(mappings).toEqual({ taskA: "targetA" });
    });

    it("should not mutate when removing", () => {
      const mappings = { taskA: "targetA", taskB: "targetB" };
      update_mapping("", "taskA", mappings);
      expect(mappings).toEqual({ taskA: "targetA", taskB: "targetB" });
    });
  });

  describe("add_query_params_abstract", () => {
    it("should add first param to a URL with no query", () => {
      let routed_to;
      add_query_params_abstract(
        {},
        "/migrations",
        (url) => (routed_to = url),
        "/migrations",
        "source",
        "def1",
      );
      expect(routed_to).toBe("/migrations?source=def1");
    });

    it("should update an existing param", () => {
      let routed_to;
      add_query_params_abstract(
        { source: "old" },
        "/migrations?source=old",
        (url) => (routed_to = url),
        "/migrations",
        "source",
        "new",
      );
      expect(routed_to).toContain("source=new");
    });

    it("should add a second param alongside existing", () => {
      let routed_to;
      add_query_params_abstract(
        { source: "def1" },
        "/migrations?source=def1",
        (url) => (routed_to = url),
        "/migrations",
        "target",
        "def2",
      );
      expect(routed_to).toContain("source=def1");
      expect(routed_to).toContain("target=def2");
    });
  });
});
