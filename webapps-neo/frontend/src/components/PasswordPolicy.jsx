import { useContext, useEffect } from "preact/hooks";
import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { RESPONSE_STATE } from "../api/helper.jsx";

/**
 * The engine can enforce a password policy. Where it does, a password that
 * breaks a rule is refused with a message that names nothing useful, so the
 * rules are shown up front and the password is checked before it is sent.
 *
 * An engine without a policy answers 404 — that is not an error, it means
 * there are no rules, and nothing is rendered.
 */

/** Fetch the policy once; `null` when the engine enforces none. */
export const usePasswordPolicy = () => {
  const state = useContext(AppState),
    rules = useSignal(null);

  useEffect(() => {
    void engine_rest.user.password_policy(state).then((answer) => {
      rules.value =
        answer?.status === RESPONSE_STATE.SUCCESS
          ? (answer.data?.rules ?? [])
          : null;
    });
  }, [state, rules]);

  return rules;
};

/**
 * Ask the engine whether a password satisfies the policy.
 * @returns the rules it breaks, empty when it satisfies them (or none exist).
 */
export const broken_rules = async (state, password, user_id) => {
  const answer = await engine_rest.user.check_password(
    state,
    password,
    user_id,
  );
  if (answer?.status !== RESPONSE_STATE.SUCCESS) return [];
  return (answer.data?.rules ?? []).filter((rule) => rule.valid === false);
};

export const PasswordPolicyRules = ({ rules, broken = [] }) => {
  const [t] = useTranslation();
  if (!rules || rules.length === 0) return null;

  const broken_placeholders = new Set(broken.map((rule) => rule.placeholder));

  return (
    <div class="password-policy">
      <p>{t("admin.password-policy.title")}</p>
      <ul>
        {rules.map((rule) => (
          <li
            key={rule.placeholder}
            class={broken_placeholders.has(rule.placeholder) ? "broken" : null}
          >
            {t(`admin.password-policy.${rule.placeholder}`, {
              ...rule.parameter,
              defaultValue: rule.placeholder,
            })}
          </li>
        ))}
      </ul>
    </div>
  );
};
