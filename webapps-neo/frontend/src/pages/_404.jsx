import { useTranslation } from "react-i18next";
import { PageHeading } from "../components/Heading.jsx";

export function NotFound() {
	const [t] = useTranslation();

	return (
		<main id="content" class="p-3">
			<PageHeading>{t("not-found.title")}</PageHeading>
			<p>{t("not-found.text")}</p>
			<p>{t("not-found.hint")}</p>
			<p>
				{t("not-found.goto-hint-prefix")}	<code>ALT + K</code> {t("not-found.goto-hint-suffix")}
			</p>
		</main>
	);
}
