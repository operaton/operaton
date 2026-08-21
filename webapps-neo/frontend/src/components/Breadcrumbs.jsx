import { useTranslation } from 'react-i18next'

const Breadcrumbs = ({ paths }) => {
  const [t] = useTranslation()
  return (
    <nav aria-label={t("common.breadcrumb")}>
      <ol class="breadcrumbs">
        {paths.slice(0, -1).map(({ name, route }, i) => (
          <li key={i}><a href={route}>{name}</a></li>))}
        <li><span aria-current="page">{paths.at(-1).name}</span></li>
      </ol>
    </nav>
  )
}

export { Breadcrumbs }