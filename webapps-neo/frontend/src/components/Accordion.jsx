import { useLocation, useRoute } from 'preact-iso'
import { useTranslation } from 'react-i18next'

const Accordion = ({ className, sections, accordion_name, param_name = 'panel', base_path }) => {
  const { params } = useRoute()
  const active_panel = params[param_name]
  const { route } = useLocation()
  const [t] = useTranslation()

  return (
    <div class={`accordion ${className || " "}`}>
      {sections.map(section => {
          return (
            <details key={section.id}
                     id={section.id}
                     name={accordion_name}
                     open={section.id === active_panel}>
              <summary
                onClick={(e) => {
                  e.preventDefault();
                  route(`${base_path}/${section.id}`)}}
              >{section.nameKey ? t(section.nameKey) : section.name}</summary>
              <div class="panel">
                {section.target}
              </div>
            </details>)
        }
      )}
    </div>
  )
}

export { Accordion }