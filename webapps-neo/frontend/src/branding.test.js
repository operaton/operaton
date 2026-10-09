import { describe, it, expect, afterEach } from "vitest";
import { set_config } from "./config.js";
import {
  build_css,
  app_name,
  logo_url,
  logo_alt,
  apply_branding,
} from "./branding.js";

afterEach(() => {
  set_config(null);
  document.getElementById("operaton-branding")?.remove();
});

describe("branding accessors", () => {
  it("falls back to Operaton defaults without a branding block", () => {
    set_config({});
    expect(app_name()).toBe("Operaton");
    expect(logo_url()).toBe("/operaton-logo.svg");
    expect(logo_alt()).toBe("Operaton");
  });

  it("uses configured values", () => {
    set_config({
      branding: { appName: "Acme", logoUrl: "/b/l.svg", logoAlt: "Acme Logo" },
    });
    expect(app_name()).toBe("Acme");
    expect(logo_url()).toBe("/b/l.svg");
    expect(logo_alt()).toBe("Acme Logo");
  });

  it("rejects an unsafe logo url and keeps the default", () => {
    set_config({ branding: { logoUrl: "javascript:alert(1)" } });
    expect(logo_url()).toBe("/operaton-logo.svg");
  });

  it("logoAlt falls back to appName", () => {
    set_config({ branding: { appName: "Acme" } });
    expect(logo_alt()).toBe("Acme");
  });
});

describe("build_css", () => {
  it("emits valid colour tokens and hue in :root", () => {
    const css = build_css({
      colors: { primary: "#f15200", link: "#c14200" },
      hue: 40,
    });
    expect(css).toContain("--color-primary: #f15200;");
    expect(css).toContain("--color-link: #c14200;");
    expect(css).toContain("--hue: 40;");
  });

  it("skips invalid hex values but keeps valid siblings", () => {
    const css = build_css({ colors: { primary: "red", danger: "#bb2511" } });
    expect(css).not.toContain("red");
    expect(css).toContain("--color-danger: #bb2511;");
  });

  it("emits a dark-mode block for colorsDark", () => {
    const css = build_css({ colorsDark: { primary: "#ff8a4c" } });
    expect(css).toContain("prefers-color-scheme: dark");
    expect(css).toContain("--color-primary: #ff8a4c;");
  });

  it("returns an empty string when nothing valid is present", () => {
    expect(build_css({})).toBe("");
    expect(build_css({ colors: { primary: "nope" }, hue: "big" })).toBe("");
  });
});

describe("apply_branding", () => {
  it("injects the style, swaps the favicon and sets the title", () => {
    set_config({
      branding: {
        appName: "Acme",
        faviconUrl: "/b/fav.ico",
        colors: { primary: "#f15200" },
      },
    });
    apply_branding();

    expect(document.getElementById("operaton-branding")?.textContent).toContain(
      "--color-primary: #f15200;",
    );
    expect(
      document.querySelector('link[rel~="icon"]')?.getAttribute("href"),
    ).toBe("/b/fav.ico");
    expect(document.title).toBe("Acme");
  });

  it("is a no-op style-wise for an empty branding block", () => {
    set_config({});
    apply_branding();
    expect(document.getElementById("operaton-branding")).toBeNull();
  });
});
