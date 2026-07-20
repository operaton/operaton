import DmnJS from 'dmn-js'
import { useLayoutEffect } from 'preact/hooks'



export const DmnViewer = ({ xml, container }) => {

  document.querySelector(container).innerText = ''

  const
    // https://github.com/operaton/operaton/blob/main/webapps/frontend/operaton-commons-ui/lib/widgets/dmn-viewer/cam-widget-dmn-viewer.js#L301
    // https://github.com/operaton/operaton/blob/main/webapps/frontend/operaton-commons-ui/lib/widgets/dmn-viewer/lib/navigatedViewer.js
    viewer = new DmnJS({
      container,
      height: 500,
      tableViewOnly: true,
      hideDetails: true,
      drd: {
        drillDown: {
          enabled: true
        }
      }
    })

  viewer.importXML(xml, (err) => {
    if (err) {
      console.log('error rendering', err)
    }
  })

  return <></>
}