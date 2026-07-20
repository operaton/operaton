/**
 * Pure builders for the BPMN modification requests, kept out of the component so
 * they can be unit-tested without a real diagram / drag events (which jsdom and
 * happy-dom do not support). See BPMNViewer_helpers.test.js.
 */

/**
 * Single-instance modification body (POST /process-instance/{id}/modification):
 * cancel the given concrete activity-instance ids, then start before the target.
 */
export const build_single_modification = (cancel_ids, target_activity_id) => ({
  skipCustomListeners: false,
  skipIoMappings: false,
  instructions: [
    ...cancel_ids.map((aid) => ({ type: "cancel", activityInstanceId: aid })),
    { type: "startBeforeActivity", activityId: target_activity_id },
  ],
});

/**
 * Batch modification instructions act by activityId across instances: cancel the
 * tokens on the source activity and start before the target.
 */
export const build_batch_instructions = (
  source_activity_id,
  target_activity_id,
) => [
  {
    type: "cancel",
    activityId: source_activity_id,
    cancelCurrentActiveActivityInstances: true,
  },
  { type: "startBeforeActivity", activityId: target_activity_id },
];

/** Query selecting all running instances currently on the source activity. */
export const build_batch_query = (definition_id, source_activity_id) => ({
  processDefinitionId: definition_id,
  activityIdIn: [source_activity_id],
});

/**
 * Full async-batch modification body (POST /modification/executeAsync), used for
 * the request payload preview in the dialog.
 */
export const build_batch_modification = (
  definition_id,
  source_activity_id,
  target_activity_id,
) => ({
  processDefinitionId: definition_id,
  instructions: build_batch_instructions(
    source_activity_id,
    target_activity_id,
  ),
  processInstanceQuery: build_batch_query(definition_id, source_activity_id),
  skipCustomListeners: false,
  skipIoMappings: false,
});
