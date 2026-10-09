import { useContext } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import { app_allowed } from "../helper/authorized_apps.js";

/**
 * Says why a page is closed, instead of rendering it empty.
 *
 * Without an authorization the engine filters every query, so the page would
 * load and show nothing at all — indistinguishable from "there is no data".
 */
const NoAccess = ({ app }) => {
  const [t] = useTranslation();
  return (
    <main id="content" class="p-3">
      <h1>{t("no-access.title")}</h1>
      <p>{t("no-access.text", { app })}</p>
      <p>{t("no-access.hint")}</p>
    </main>
  );
};

/**
 * Wraps a page so it is only rendered when the signed-in user may use `app`.
 *
 * Hiding the navigation entry is not enough on its own: the address bar, a
 * bookmark and the keyboard shortcuts all reach a page directly. This is the
 * single place where that is decided.
 */
export const require_app = (Component, app) => {
  const Guarded = (props) => {
    const state = useContext(AppState);
    return app_allowed(state.auth.authorized_apps.value, app) ? (
      <Component {...props} />
    ) : (
      <NoAccess app={app} />
    );
  };
  Guarded.displayName = `require_app(${app})`;
  return Guarded;
};
