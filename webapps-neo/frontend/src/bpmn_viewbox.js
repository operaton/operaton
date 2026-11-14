/**
 * Fitting a diagram into its container without centring it.
 *
 * `canvas.zoom('fit-viewport', 'auto')` puts the space a diagram does not fill
 * equally on both sides. On the migration page that is the wrong default: two
 * definitions sit side by side in half-width panes and are read from their
 * first node, so the leftover width belongs on the right only.
 *
 * The viewbox is shifted, not re-fitted. `fit-viewport` has already picked a
 * scale that shows the whole diagram; only the left edge moves, which keeps the
 * vertical placement (still centred) untouched. `viewbox()` reports the drawn
 * content as `inner` in diagram coordinates and the visible area as
 * `width`/`height` in those same coordinates at the current scale, so the pixel
 * padding is converted with that scale.
 */
export const FIT_CONTENT_PADDING = 12;

export const fit_view_left = (canvas, padding = FIT_CONTENT_PADDING) => {
  canvas.zoom("fit-viewport", "auto");

  const viewbox = canvas.viewbox();
  const content = viewbox.inner;
  const scale = viewbox.scale || 1;

  // Nothing was drawn (an empty layer has no bbox): leave the canvas alone.
  if (!content || !content.width) return;

  canvas.viewbox({
    x: content.x - padding / scale,
    y: viewbox.y,
    width: viewbox.width,
    height: viewbox.height,
  });
};