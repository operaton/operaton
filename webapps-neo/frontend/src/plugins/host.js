/**
 * host.js
 *
 * Exposes the host's own framework instances on `window.__OPERATON_PLUGIN_HOST__`
 * so that *remote, no-build* plugins can share the app's single Preact instance
 * (bundling their own copy would break `useContext(AppState)`). Such plugins are
 * plain JS that read from this object and author markup with `html` (htm bound
 * to the host's `h`). Bundled plugins share the app bundle and ignore this.
 */
import { h, Fragment } from "preact";
import * as hooks from "preact/hooks";
import * as signals from "@preact/signals";
import htm from "htm";
import { AppState } from "../state.js";
import engine_rest, {
  RequestState,
  RESPONSE_STATE,
} from "../api/engine_rest.jsx";
import { GET, POST, PUT, DELETE, has_data } from "../api/helper.jsx";
import { register } from "./registry.js";
import { use_plugin_api } from "./plugin_api.jsx";
import { PLUGIN_POINTS } from "./points.js";

export const install_plugin_host = () => {
  if (typeof window === "undefined") return;
  window.__OPERATON_PLUGIN_HOST__ = {
    h,
    Fragment,
    html: htm.bind(h),
    hooks,
    signals,
    AppState,
    engine_rest,
    RequestState,
    RESPONSE_STATE,
    has_data,
    GET,
    POST,
    PUT,
    DELETE,
    register,
    use_plugin_api,
    PLUGIN_POINTS,
  };
};
