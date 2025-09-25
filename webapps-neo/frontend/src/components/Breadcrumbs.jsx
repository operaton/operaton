const Breadcrumbs = ({ paths }) =>
  <ol className="breadcrumbs">
    {paths.slice(0, -1).map(({ name, route }) => (
      <li key={route}><a href={route}>{name}</a></li>))}
    <li><span>{paths.at(-1).name}</span></li>
  </ol>

export { Breadcrumbs }