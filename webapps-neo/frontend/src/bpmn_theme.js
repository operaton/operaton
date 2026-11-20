/**
 * The colours bpmn-js paints the canvas with.
 *
 * bpmn-js writes shape fill, stroke and the label colour inline on every SVG
 * node at render time, so no stylesheet can reach them — the values have to be
 * handed to the renderer when it is constructed. These are the same three
 * tokens for every viewer in the app (BPMNViewer and ThemedBpmn); keeping them
 * in one place is what stops the two from drifting apart.
 *
 * Read them off <html> rather than the viewer's own container: a custom
 * property declared on an element would win over the inherited value, and
 * --canvas-* is declared on :root (see css/style.css).
 */
export const canvas_colors = () => {
  const css = getComputedStyle(document.documentElement)
  const read = (name, fallback) => css.getPropertyValue(name).trim() || fallback
  return {
    defaultFillColor: read('--canvas-fill', '#ffffff'),
    defaultStrokeColor: read('--canvas-stroke', '#333333'),
    defaultLabelColor: read('--canvas-label', '#222222'),
  }
}