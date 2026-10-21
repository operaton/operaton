import { useSignal } from "@preact/signals";
import {
  formatAbsolute,
  formatRelativeDate,
} from "../helper/date_formatter.js";

/**
 * A timestamp read as "in 2 days", with the exact moment on hover.
 *
 * Not the title attribute: the browser decides when that appears, which is
 * about a second, and it cannot be styled. This one is positioned against the
 * viewport rather than the row, so a scrolling ancestor cannot clip it.
 */
export const RelativeTime = ({ datetime }) => {
  const anchor = useSignal(null);

  const show = (e) => {
    const box = e.currentTarget.getBoundingClientRect();
    anchor.value = { x: box.left + box.width / 2, y: box.top };
  };
  const hide = () => (anchor.value = null);

  return (
    <time
      datetime={datetime}
      class="relative-time"
      onMouseEnter={show}
      onMouseLeave={hide}
    >
      {formatRelativeDate(datetime)}
      {anchor.value && (
        <span
          class="time-tooltip"
          role="tooltip"
          style={`left:${anchor.value.x}px;top:${anchor.value.y}px`}
        >
          {formatAbsolute(datetime)}
        </span>
      )}
    </time>
  );
};
