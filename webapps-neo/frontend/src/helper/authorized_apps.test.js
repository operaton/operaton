import { describe, it, expect } from "vitest";
import { app_allowed, page_allowed, PAGE_APPS } from "./authorized_apps.js";

describe("authorized_apps", () => {
  describe("app_allowed", () => {
    it("allows an application the server listed", () => {
      expect(app_allowed(["welcome", "tasklist"], "tasklist")).toBe(true);
    });

    it("refuses one it did not list", () => {
      expect(app_allowed(["welcome", "tasklist"], "cockpit")).toBe(false);
      expect(app_allowed(["welcome", "tasklist"], "admin")).toBe(false);
    });

    it("allows everything when the server told us nothing", () => {
      // A backend configured elsewhere has no session to ask. Hiding pages on
      // a guess would be worse than showing them — the engine filters anyway.
      expect(app_allowed(null, "admin")).toBe(true);
      expect(app_allowed(undefined, "admin")).toBe(true);
    });

    it("allows a page that belongs to no application", () => {
      expect(app_allowed(["welcome"], undefined)).toBe(true);
    });
  });

  describe("page_allowed", () => {
    it("maps every cockpit page onto the cockpit application", () => {
      const cockpit_pages = Object.entries(PAGE_APPS)
        .filter(([, app]) => app === "cockpit")
        .map(([href]) => href);
      expect(cockpit_pages).toContain("/processes");
      for (const href of cockpit_pages) {
        expect(page_allowed(["welcome", "tasklist"], href)).toBe(false);
        expect(page_allowed(["welcome", "cockpit"], href)).toBe(true);
      }
    });

    it("keeps pages that belong to no application available", () => {
      for (const href of ["/", "/account", "/help"]) {
        expect(page_allowed(["welcome"], href)).toBe(true);
      }
    });

    it("lets a tasklist-only user keep just the task list", () => {
      const apps = ["neo", "welcome", "tasklist"];
      expect(page_allowed(apps, "/tasks")).toBe(true);
      expect(page_allowed(apps, "/admin")).toBe(false);
      expect(page_allowed(apps, "/deployments")).toBe(false);
    });
  });
});
