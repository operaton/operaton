/**
 * plugins.js
 *
 * Shared container for plugin-contributed API namespaces. It is deliberately
 * dependency-free so importing it (e.g. from the plugin registry, which
 * `state.js` pulls in) never drags in the HTTP helpers that tests mock. The
 * same object is exposed as `engine_rest.plugins`.
 */
export const plugin_apis = {};

/** Mount a plugin's API functions under engine_rest.plugins.<id>. */
export const mount_plugin_api = (id, fns) => {
  plugin_apis[id] = fns;
};
