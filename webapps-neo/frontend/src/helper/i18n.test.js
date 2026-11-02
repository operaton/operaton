import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { with_base_uri } from "../test/helpers.js";

/**
 * The i18n singleton is stubbed globally (src/test/setup.js) so component tests
 * never spin up i18next-http-backend. Here we want the real module, to check the
 * one thing it configures on our behalf: where translations are fetched from.
 */
const real_i18n = async (base) =>
  with_base_uri(base, async () => {
    vi.resetModules();
    return (await vi.importActual("./i18n.js")).default;
  });

describe("i18n", () => {
  beforeEach(() => {
    // i18next loads the initial language as soon as it is initialised.
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue({ ok: true, status: 200, text: async () => "{}" }),
    );
    vi.spyOn(console, "log").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => vi.unstubAllGlobals());

  it("loads translations from the server root by default", async () => {
    const i18n = await real_i18n("http://localhost:3000/");

    expect(i18n.options.backend.loadPath).toBe("/locales/{{lng}}/{{ns}}.json");
  });

  it("loads translations from the application root under a sub-path", async () => {
    const i18n = await real_i18n("http://localhost:3000/app-neo/");

    expect(i18n.options.backend.loadPath).toBe(
      "/app-neo/locales/{{lng}}/{{ns}}.json",
    );
  });
});
