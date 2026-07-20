import { describe, it, expect } from "vitest";
import {
  build_single_modification,
  build_batch_instructions,
  build_batch_query,
  build_batch_modification,
} from "./BPMNViewer_helpers.js";

describe("BPMNViewer modification builders", () => {
  describe("build_single_modification", () => {
    it("cancels the given activity-instance ids then starts before the target", () => {
      expect(build_single_modification(["ai1", "ai2"], "taskB")).toEqual({
        skipCustomListeners: false,
        skipIoMappings: false,
        instructions: [
          { type: "cancel", activityInstanceId: "ai1" },
          { type: "cancel", activityInstanceId: "ai2" },
          { type: "startBeforeActivity", activityId: "taskB" },
        ],
      });
    });

    it("supports a single picked instance", () => {
      const body = build_single_modification(["ai1"], "taskB");
      expect(body.instructions).toHaveLength(2);
    });
  });

  describe("build_batch_instructions", () => {
    it("cancels by activityId (across instances) and starts before the target", () => {
      expect(build_batch_instructions("taskA", "taskB")).toEqual([
        {
          type: "cancel",
          activityId: "taskA",
          cancelCurrentActiveActivityInstances: true,
        },
        { type: "startBeforeActivity", activityId: "taskB" },
      ]);
    });
  });

  describe("build_batch_query", () => {
    it("targets all instances on the source activity of the definition", () => {
      expect(build_batch_query("invoice:1:abc", "taskA")).toEqual({
        processDefinitionId: "invoice:1:abc",
        activityIdIn: ["taskA"],
      });
    });
  });

  describe("build_batch_modification", () => {
    it("assembles the executeAsync payload", () => {
      expect(
        build_batch_modification("invoice:1:abc", "taskA", "taskB"),
      ).toEqual({
        processDefinitionId: "invoice:1:abc",
        instructions: [
          {
            type: "cancel",
            activityId: "taskA",
            cancelCurrentActiveActivityInstances: true,
          },
          { type: "startBeforeActivity", activityId: "taskB" },
        ],
        processInstanceQuery: {
          processDefinitionId: "invoice:1:abc",
          activityIdIn: ["taskA"],
        },
        skipCustomListeners: false,
        skipIoMappings: false,
      });
    });
  });
});
