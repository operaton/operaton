import { describe, it, expect, vi } from "vitest";

vi.mock("../helper.jsx", () => ({
  POST: vi.fn(),
}));

import { POST } from "../helper.jsx";
import migration from "./migration.js";

const create_mock_state = () => ({
  api: {
    migration: {
      generate: { value: null },
      validation: { value: null },
      execution: { value: null },
    },
  },
});

describe("api/resources/migration", () => {
  describe("generate", () => {
    it("should POST to /migration/generate with definition IDs", () => {
      const state = create_mock_state();
      migration.generate(state, "source:1:abc", "target:2:def");

      expect(POST).toHaveBeenCalledWith(
        "/migration/generate",
        {
          sourceProcessDefinitionId: "source:1:abc",
          targetProcessDefinitionId: "target:2:def",
          variables: {},
          updateEventTriggers: true,
        },
        state,
        state.api.migration.generate,
      );
    });

    it("should pass custom variables", () => {
      const state = create_mock_state();
      const variables = { env: { type: "String", value: "prod" } };
      migration.generate(state, "src:1:a", "tgt:2:b", variables);

      expect(POST).toHaveBeenCalledWith(
        "/migration/generate",
        expect.objectContaining({ variables }),
        state,
        state.api.migration.generate,
      );
    });

    it("should pass update_event_triggers as false", () => {
      const state = create_mock_state();
      migration.generate(state, "src:1:a", "tgt:2:b", {}, false);

      expect(POST).toHaveBeenCalledWith(
        "/migration/generate",
        expect.objectContaining({ updateEventTriggers: false }),
        state,
        state.api.migration.generate,
      );
    });
  });

  describe("validate", () => {
    it("should POST migration plan to /migration/validate", () => {
      const state = create_mock_state();
      const plan = {
        sourceProcessDefinitionId: "src:1:a",
        targetProcessDefinitionId: "tgt:2:b",
        instructions: [
          {
            sourceActivityIds: ["taskA"],
            targetActivityIds: ["taskB"],
            updateEventTrigger: false,
          },
        ],
      };
      migration.validate(state, plan);

      expect(POST).toHaveBeenCalledWith(
        "/migration/validate",
        plan,
        state,
        state.api.migration.validation,
      );
    });
  });

  describe("execute", () => {
    it("should POST to /migration/execute by default", () => {
      const state = create_mock_state();
      const plan = { sourceProcessDefinitionId: "src:1:a" };
      migration.execute(state, plan);

      expect(POST).toHaveBeenCalledWith(
        "/migration/execute",
        {
          migrationPlan: plan,
          processInstanceIds: null,
          processInstanceQuery: null,
          skipCustomListeners: false,
          skipIoMappings: false,
        },
        state,
        state.api.migration.execution,
      );
    });

    it("should POST to /migration/executeAsync when async is true", () => {
      const state = create_mock_state();
      const plan = { sourceProcessDefinitionId: "src:1:a" };
      migration.execute(state, plan, null, null, false, false, true);

      expect(POST).toHaveBeenCalledWith(
        "/migration/executeAsync",
        expect.any(Object),
        state,
        state.api.migration.execution,
      );
    });

    it("should pass process instance IDs", () => {
      const state = create_mock_state();
      const plan = { sourceProcessDefinitionId: "src:1:a" };
      const instances = ["instance-1", "instance-2"];
      migration.execute(state, plan, instances);

      expect(POST).toHaveBeenCalledWith(
        "/migration/execute",
        expect.objectContaining({ processInstanceIds: instances }),
        state,
        state.api.migration.execution,
      );
    });

    it("should pass process instance query", () => {
      const state = create_mock_state();
      const plan = { sourceProcessDefinitionId: "src:1:a" };
      const query = { processDefinitionId: "src:1:a" };
      migration.execute(state, plan, null, query);

      expect(POST).toHaveBeenCalledWith(
        "/migration/execute",
        expect.objectContaining({ processInstanceQuery: query }),
        state,
        state.api.migration.execution,
      );
    });

    it("should pass skip options", () => {
      const state = create_mock_state();
      const plan = { sourceProcessDefinitionId: "src:1:a" };
      migration.execute(state, plan, null, null, true, true);

      expect(POST).toHaveBeenCalledWith(
        "/migration/execute",
        expect.objectContaining({
          skipCustomListeners: true,
          skipIoMappings: true,
        }),
        state,
        state.api.migration.execution,
      );
    });
  });
});
