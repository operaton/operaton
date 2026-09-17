import { useLocation, useRoute } from 'preact-iso'
import { useTranslation } from 'react-i18next'

/**
 * `query` is appended to every tab link. What the page around the tabs is
 * filtered and sorted by lives in the address, so a tab link without it drops
 * the list back to unfiltered the moment a tab is picked.
 */
const Tabs = ({
  base_url,
  tabs,
  param_name = 'tab',
  className = '',
  label,
  query = '',
}) => {
  const { params } = useRoute()
  const { route, path } = useLocation()
  const [t] = useTranslation()
  const tab = params[param_name]

  if (tab === null || tab === undefined && path === base_url) {
    route(`${path}/${tabs[0].id}${query}`)
  }

  const go_to_tab = (new_tab) => {
    document.getElementById(`${param_name}-${new_tab.id}`).focus()
    route(`${base_url}/${new_tab.id}${query}`)
  }

  const change_tab = (event, current_tab) => {
    if (event.key === 'ArrowRight') {
      go_to_tab(tabs[tabs.length !== current_tab.pos + 1 ? current_tab.pos + 1 : 0])
    } else if (event.key === 'ArrowLeft') {
      go_to_tab(tabs[0 !== current_tab.pos ? current_tab.pos - 1 : tabs.length - 1])
    } else if (event.key === 'Home') {
      event.preventDefault()
      go_to_tab(tabs[0])
    } else if (event.key === 'End') {
      event.preventDefault()
      go_to_tab(tabs[tabs.length - 1])
    }
  }

  return (
    <div class={`tabs ${className}`}>
      <div class="tab-selection" role="tablist"
           aria-label={label}>

        {tabs.map(tab_name => {
            return (
              <a key={`tablist-${tab_name.id}`}
                 id={`${param_name}-${tab_name.id}`}
                 role="tab"
                 aria-selected={tab === tab_name.id}
                 aria-controls={`tabpanel-${tab_name.id}`}
                 href={`${base_url}/${tab_name.id}${query}`}
                 tabIndex={tab !== tab_name.id ? '-1' : null}
                 // title={tab_name.name}
                 onKeyDown={(event) => change_tab(event, tab_name)}
              >
                {tab_name.nameKey ? t(tab_name.nameKey) : tab_name.name}
              </a>)
          }
        )}
      </div>
      <div class="selected-tab"
           id={`tabpanel-${tab}`}
           role="tabpanel"
           tabIndex="0"
           aria-labelledby={`${param_name}-${tab}`}>
        {(() => {
          const active = tabs.find(tab_ => tab === tab_.id)
          return active ? <active.Component /> : t("common.select-page")
        })()}
      </div>
    </div>
  )
}


export { Tabs }