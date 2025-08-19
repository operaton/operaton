import { useLocation, useRoute } from 'preact-iso'

const Tabs = ({ base_url, tabs, param_name = 'tab', className = '' }) => {
  const { params } = useRoute()
  const { route, path } = useLocation()
  const tab = params[param_name]

  if (tab === null || tab === undefined && path === base_url) {
    route(`${path}/${tabs[0].id}`)
  }

  const change_tab = (event, current_tab) => {
    if (event.key === 'ArrowRight') {
      const new_tab = tabs[
        tabs.length !== current_tab.pos + 1
          ? current_tab.pos + 1
          : 0]
      document.getElementById(`${param_name}-${new_tab.id}`).focus()
      route(`${base_url}/${new_tab.id}`)
    } else if (event.key === 'ArrowLeft') {
      const new_tab = tabs[
        0 !== current_tab.pos
          ? current_tab.pos - 1
          : tabs.length - 1]
      document.getElementById(`${param_name}-${new_tab.id}`).focus()
      route(`${base_url}/${new_tab.id}`)
    }
  }

  return (
    <div class={`tabs ${className}`}>
      <div class="tab-selection" role="tablist"
           aria-labelledby="tablist-1">

        {tabs.map(tab_name => {
            return (
              <a key={`tablist-${tab_name.id}`}
                 id={`${param_name}-${tab_name.id}`}
                 role="tab"
                 aria-selected={tab === tab_name.id}
                 aria-controls={`tabpanel-${tab_name.id}}`}
                 href={`${base_url}/${tab_name.id}`}
                 tabIndex={tab !== tab_name.id ? '-1' : null}
                 // title={tab_name.name}
                 onKeyDown={(event) => change_tab(event, tab_name)}
              >
                {tab_name.name}
              </a>)
          }
        )}
      </div>
      <div class="selected-tab"
           id={`tabpanel-${tab}`}
           role="tabpanel"
           tabIndex="0"
           aria-labelledby={`${param_name}-${tab}`}>
        {tabs.find(tab_ => tab === tab_.id)?.target || 'Select a tab'}
      </div>
    </div>
  )
}


export { Tabs }