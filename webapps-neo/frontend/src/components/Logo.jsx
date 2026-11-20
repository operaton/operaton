import { is_default_logo, logo_alt, logo_url } from "../branding.js";

/**
 * The installation's logo.
 *
 * A configured logo is a plain <img>: its bytes are the caller's business.
 * The bundled mark is different — it is a single-colour SVG that recolours
 * itself with its own `prefers-color-scheme` query, which an <img> cannot
 * escape, so it would follow the OS instead of the chosen theme. Rendering it
 * as a mask instead paints it with `background-color` (the --text token), which
 * makes it follow html[data-theme] in every combination of OS and choice.
 *
 * The mask URL travels in a custom property because it has to go through
 * app_path(): a sub-path deployment serves the asset one directory deeper.
 */
export const Logo = ({ class: cls }) => {
  if (!is_default_logo())
    return <img class={cls} src={logo_url()} alt={logo_alt()} />;

  return (
    <span
      class={cls ? `${cls} logo-default` : "logo-default"}
      role="img"
      aria-label={logo_alt()}
      style={{ "--logo-mask": `url("${logo_url()}")` }}
    />
  );
};
