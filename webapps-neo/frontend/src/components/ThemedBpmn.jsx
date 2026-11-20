import { useEffect, useRef } from "preact/hooks";
import NavigatedViewer from "bpmn-js/lib/NavigatedViewer";
import { canvas_colors } from "../bpmn_theme.js";
import { fit_view_left } from "../bpmn_viewbox.js";

/**
 * A read-only BPMN diagram that follows the app theme.
 *
 * bpmn-js inlines fill and stroke on every SVG node at render time, so no
 * stylesheet can recolour it — the colours have to be handed to the renderer
 * when it is built. `canvas_colors()` reads the same `--canvas-*` tokens
 * BPMNViewer uses, which is what keeps the migration previews and the process
 * definition diagram consistent.
 *
 * The theme switch itself is not handled here: the caller renders this component
 * with `key={resolved_theme.value}`, so a change remounts it and the diagram is
 * imported again with the new colours. Re-importing into the same viewer would
 * be cheaper but leaves the old paint in place for every element bpmn-js does
 * not touch.
 */
export const ThemedBpmn = ({ diagramXML }) => {
  const ref = useRef(null);

  useEffect(() => {
    const node = ref.current;
    if (!node || !diagramXML) return;

    const viewer = new NavigatedViewer({
      container: node,
      bpmnRenderer: canvas_colors(),
    });

    void viewer.importXML(diagramXML).then(() => {
      // `fit_view_left` rather than a bare fit-viewport: these two previews are
      // read from their first node, so leftover width goes to the right edge.
      fit_view_left(viewer.get("canvas"));
    });

    return () => viewer.destroy();
  }, [diagramXML]);

  return <div class="bpmn-preview" ref={ref} />;
};