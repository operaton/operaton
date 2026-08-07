import { useTranslation } from "react-i18next";

export const Home = () => {
  const [t] = useTranslation();

  return (
    <div class="p-3">
      <h2>{t("home.welcome")}</h2>
      <p>
        {t("home.info-text")} <a href="https://operaton.org">{t("home.operaton-org")}</a>.
      </p>

      <h3>{t("home.help-title")}</h3>
      <p>{t("home.help-text")}</p>
      <ul>
        <li>
          <a href="/help">{t("home.help-page")}</a>
        </li>
        <li>
          <a href="http://operaton.org/getting-started">{t("home.getting-started")}</a>
        </li>
      </ul>

      <h3>{t("home.web-apps-title")}</h3>
      <p>{t("home.web-apps-text")}</p>

      <h3>{t("home.community-title")}</h3>
      <p>
        {t("home.community-text")}
      </p>
      <ul>
        <li>
          <a href="https://forum.operaton.org">{t("home.forum")}</a>
        </li>
        <li>
          <a href="https://github.com/operaton/operaton">{t("home.github")}</a>
        </li>
      </ul>

      <h4>{t("home.bug-title")}</h4>
      <p>{t("home.bug-text")}</p>
      <ul>
        <li>
          <a href="https://github.com/operaton/operaton/issues">{t("home.bug-link")}</a>
        </li>
      </ul>

      <h4>{t("home.feature-title")}</h4>
      <p>
        {t("home.feature-text")}
      </p>
      <ul>
        <li>
          <a href="https://forum.operaton.org">{t("home.feature-forum")}</a>
        </li>
        <li>
          <a href="https://github.com/operaton/operaton/issues">{t("home.feature-github")}</a>
        </li>
      </ul>
    </div>
  );
};
