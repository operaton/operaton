import { useTranslation } from "react-i18next";

export function NotFound() {
	const [t] = useTranslation();

	return (
		<section class="p-3">
			<h1>{t("not-found.title")}</h1>
			<p>{t("not-found.text")}</p>
			<p>{t("not-found.hint")}</p>
			<p>
				{t("not-found.goto-hint-prefix")}	<code>ALT + K</code> {t("not-found.goto-hint-suffix")}
			</p>
		</section>
	);
}
