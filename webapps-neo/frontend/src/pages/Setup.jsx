import { useContext } from "preact/hooks";
import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { RESPONSE_STATE } from "../api/helper.jsx";

/**
 * First-run screen: creates the initial administrator when the engine has none.
 *
 * Reached only when the server reports that setup is still available, which it stops doing
 * the moment any user belongs to `operaton-admin`. Deliberately reuses the login screen's
 * layout classes so the two first-contact screens look like one another.
 *
 * @param {Object} props
 * @param {Function} props.on_created - called once the administrator exists
 */
export const SetupPage = ({ on_created }) => {
  const { t } = useTranslation(),
    state = useContext(AppState),
    {
      api: {
        setup: { create_initial_user: create_response },
      },
    } = state,
    form = useSignal({ profile: {}, credentials: {} }),
    password_repeat = useSignal(""),
    mismatch = useSignal(false);

  const set_value = (group, key, event) =>
      (form.value = {
        ...form.peek(),
        [group]: { ...form.peek()[group], [key]: event.currentTarget.value },
      }),
    set_profile = (key, event) => set_value("profile", key, event),
    set_credentials = (key, event) => set_value("credentials", key, event),
    on_submit = (event) => {
      event.preventDefault();
      if (form.value.credentials.password !== password_repeat.value) {
        mismatch.value = true;
        return;
      }
      mismatch.value = false;
      void engine_rest.setup.create_initial_user(state, form.value).then(() => {
        if (create_response.value?.status === RESPONSE_STATE.SUCCESS) {
          on_created?.();
        }
      });
    };

  const failed = create_response.value?.status === RESPONSE_STATE.ERROR;

  return (
    <section class="login-page setup-page">
      <img class="login-logo" src="/operaton-logo.svg" alt="Operaton" />

      <div class="login-content">
        <h1>{t("setup.title")}</h1>
        <p>{t("setup.intro")}</p>

        <div class="login-card">
          <div aria-live="polite">
            {mismatch.value ? (
              <p class="error">{t("setup.password-mismatch")}</p>
            ) : null}
            {failed ? <p class="error">{t("setup.failed")}</p> : null}
          </div>

          <form onSubmit={on_submit}>
            <label for="setup-user-id">{t("setup.user-id")}</label>
            <input
              id="setup-user-id"
              name="username"
              type="text"
              autocomplete="username"
              onInput={(e) => set_profile("id", e)}
              required
            />

            <label for="setup-password">{t("setup.password")}</label>
            <input
              id="setup-password"
              type="password"
              autocomplete="new-password"
              onInput={(e) => set_credentials("password", e)}
              required
            />

            <label for="setup-password-repeat">
              {t("setup.password-repeated")}
            </label>
            <input
              id="setup-password-repeat"
              type="password"
              autocomplete="new-password"
              onInput={(e) => (password_repeat.value = e.currentTarget.value)}
              required
            />

            <label for="setup-first-name">{t("setup.first-name")}</label>
            <input
              id="setup-first-name"
              type="text"
              autocomplete="given-name"
              onInput={(e) => set_profile("firstName", e)}
              required
            />

            <label for="setup-last-name">{t("setup.last-name")}</label>
            <input
              id="setup-last-name"
              type="text"
              autocomplete="family-name"
              onInput={(e) => set_profile("lastName", e)}
              required
            />

            <label for="setup-email">{t("setup.email")}</label>
            <input
              id="setup-email"
              type="email"
              autocomplete="email"
              onInput={(e) => set_profile("email", e)}
              required
            />

            <button type="submit">{t("setup.submit")}</button>
          </form>
        </div>
      </div>
    </section>
  );
};
