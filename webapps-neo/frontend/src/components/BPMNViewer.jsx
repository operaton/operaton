import engine_rest from '../api/engine_rest.jsx'
import { useContext, useEffect } from 'preact/hooks'
import { AppState } from '../state.js'
import { useRoute } from 'preact-iso'
import { BpmnVisualization } from 'bpmn-visualization'
import * as bpmnvisu from 'bpmn-visualization'

// class BpmnVisualizationCustom extends bpmnvisu.BpmnVisualization {
//
//   constructor (containerId) {
//     super({ container: containerId })
//     this.configureStyle()
//   }
//
//   configureStyle () {
//     const styleSheet = this.graph.getStylesheet() // mxStylesheet
//     // edge
//     const defaultEdgeStyle = styleSheet.getDefaultEdgeStyle()
//     defaultEdgeStyle['fontFamily'] = 'IBM Plex Sans,sans-serif'
//     defaultEdgeStyle['fontSize'] = 12
//     // defaultEdgeStyle['fontStyle'] = 2; // 1 = bold, 2 = italic
//     // vertex
//     const defaultVertexStyle = styleSheet.getDefaultVertexStyle()
//     defaultVertexStyle['fontFamily'] = 'IBM Plex Sans,sans-serif'
//     defaultVertexStyle['fontSize'] = 12
//     // defaultVertexStyle['fontStyle'] = 2; // 1 = bold, 2 = italic
//   }
// }

/**
 * BPMN Diagram Viewer
 * @params props
 * @param xml a xml string of a bpmn diagram
 * @param container the html id for an element which gets filled with the diagram
 * @param tokens elements shown on the diagram
 * @returns {Element}
 * @constructor
 */
export const BPMNViewer = ({ xml, container, tokens }) => {

  const
    state = useContext(AppState),
    { params: { definition_id } } = useRoute(),

    viewer = new BpmnVisualization({
      container,
      navigation: { enabled: true },
      // zoom: { throttleDelay: 80, debounceDelay: 80 }
    }),
    viewerElements = viewer.bpmnElementsRegistry,
    style_running = {
      font: {
        color: '#ffffff',
        size: '16',
      },
      fill: {
        color: '#126bbe',
      },
      stroke: {
        color: '#126bbe',
      }
    },
    style_incidents = {
      font: {
        color: '#ffffff',
        size: '16',
      },
      fill: {
        color: '#bb2511',
      },
      stroke: {
        color: '#bb2511',
      }
    }

  useEffect(() => {
    try {
      viewer.load(xml, { fit: { type: bpmnvisu.FitType.Center, margin: 20 } })
      // viewer.load(xml)
    } catch (error) {
      console.error('Error loading BPMN content', error)
    }


    tokens?.map(({ id, instances, incidents }) => {
      viewerElements.addOverlays(id, {
        position: 'bottom-left',
        label: `${instances}`, style: style_running
      })

      if (incidents.length > 0) {
        viewerElements.addOverlays(id, {
          position: 'top-left',
          label: `${incidents.length}`, style: style_incidents
        })
      }

      const callActivityElement = viewer.bpmnElementsRegistry.getElementsByIds([id])[0].htmlElement
      callActivityElement.onclick = () => {
        const actions_list = []

        if (callActivityElement.classList.contains('bpmn-call-activity')) {
          // fixme: also filter for instance
          actions_list.push(
            {
              label: 'Show called activity (sub-process)',
              url: `/processes/${definition_id}/called_definitions`
            })
        }

        if (incidents.length > 0) {
          // fixme: also filter for instance
          actions_list.push(
            {
              label: 'Show all incidents for process definition',
              url: `/processes/${definition_id}/incidents`
            })
        }

        if (actions_list.length === 0) {
          void engine_rest.process_instance.by_activity_ids(state, definition_id, [id])
        } else {
          const modal = document.getElementById("digagram-modal")
          modal.showModal()
          document.getElementById("show_running_instances").addEventListener('click', () => {
            void engine_rest.process_instance.by_activity_ids(state, definition_id, [id])
            modal.close()
          })
          if (incidents.length > 0) {
            document.getElementById('show_incidents').disabled = false
          }
          if (callActivityElement.classList.contains('bpmn-call-activity')) {
            document.getElementById('show_called_activities').disabled = false
          }
        }
      }
      viewer.bpmnElementsRegistry.addCssClasses([id], 'c-hand')
    })

    return () => viewer.graph.destroy()
    // return () => document.getElementById(container).innerText = ""
  }, [container, definition_id, state, style_incidents, style_running, tokens, viewer, viewerElements, xml])



  return <>
    <dialog id="digagram-modal" className="digagram-modal">
      <h3>Available Actions for this task</h3>

      <button id="show_running_instances">Show running instances</button>
      <br />
      <button id="show_incidents" disabled>Show running instances</button>
      <br />
      <button id="show_called_activities" disabled>Show called activity (sub-process)</button>
      <br />
      <br />

      <form method="dialog">
        <button>Close</button>
      </form>

    </dialog>
  </>
}