import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import {
  parse_version,
  is_outdated,
  latest_release,
  flag_from_document,
  _reset_enabled,
} from "./update_check.js";

// One stub serves both requests: config.json carries the flag, the GitHub URL
// the release. `release` may be a function to change behaviour per call.
const serve = ({ flag = true, release = { tag_name: "v2.1.5" }, ok = true }) =>
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url) =>
      String(url).includes("api.github.com")
        ? Promise.resolve({
            ok,
            json: async () =>
              typeof release === "function" ? release() : release,
          })
        : Promise.resolve({ ok: true, json: async () => ({ updateCheck: flag }) }),
    ),
  );

const enabled = (on) => serve({ flag: on });

beforeEach(() => {
  localStorage.clear();
  _reset_enabled();
  serve({});
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("parse_version", () => {
  it("reads a plain, a v-prefixed and a pre-release version", () => {
    expect(parse_version("2.1.4")).toEqual([2, 1, 4]);
    expect(parse_version("v2.1.4")).toEqual([2, 1, 4]);
    expect(parse_version("2.2.0-SNAPSHOT")).toEqual([2, 2, 0]);
    expect(parse_version("2.2")).toEqual([2, 2, 0]);
  });

  it("returns null for anything it cannot read", () => {
    for (const value of ["", "next", undefined, null, {}])
      expect(parse_version(value)).toBeNull();
  });
});

describe("is_outdated", () => {
  it("compares major, minor and patch in order", () => {
    expect(is_outdated("2.1.4", "v2.1.5")).toBe(true);
    expect(is_outdated("2.1.4", "v2.2.0")).toBe(true);
    expect(is_outdated("2.1.4", "v3.0.0")).toBe(true);
    expect(is_outdated("2.1.4", "v2.1.4")).toBe(false);
    expect(is_outdated("2.9.0", "v2.10.0")).toBe(true);
  });

  it("never flags an engine that is ahead of the last release", () => {
    expect(is_outdated("2.2.0-SNAPSHOT", "v2.1.4")).toBe(false);
  });

  it("stays quiet when either side is unparsable", () => {
    expect(is_outdated("unknown", "v2.1.4")).toBe(false);
    expect(is_outdated("2.1.4", "latest")).toBe(false);
  });
});

describe("flag_from_document", () => {
  it("reads the flag as a boolean or a string", () => {
    expect(flag_from_document({ updateCheck: true })).toBe(true);
    expect(flag_from_document({ updateCheck: false })).toBe(false);
    expect(flag_from_document({ updateCheck: "true" })).toBe(true);
    expect(flag_from_document({ updateCheck: "false" })).toBe(false);
  });

  it("treats an unsubstituted placeholder and a missing key as unset", () => {
    expect(
      flag_from_document({ updateCheck: "DOCKER_RUN_PLACEHOLDER_UPDATE_CHECK" }),
    ).toBeUndefined();
    expect(flag_from_document({})).toBeUndefined();
    expect(flag_from_document(null)).toBeUndefined();
  });
});

describe("latest_release", () => {
  it("does not reach GitHub at all while the check is disabled", async () => {
    enabled(false);
    expect(await latest_release()).toBeNull();
    const hosts = fetch.mock.calls.map(([u]) => String(u));
    expect(hosts.some((u) => u.includes("api.github.com"))).toBe(false);
  });

  it("returns the tag and caches it for the next call", async () => {
    expect(await latest_release()).toBe("v2.1.5");
    expect(await latest_release()).toBe("v2.1.5");
    const github = fetch.mock.calls.filter(([u]) =>
      String(u).includes("api.github.com"),
    );
    expect(github).toHaveLength(1);
  });

  it("returns null on a rate limit, an error or an unusable tag", async () => {
    serve({ ok: false, release: { message: "rate limit exceeded" } });
    expect(await latest_release()).toBeNull();

    _reset_enabled();
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("offline")));
    expect(await latest_release()).toBeNull();

    _reset_enabled();
    serve({ release: { tag_name: "nightly" } });
    expect(await latest_release()).toBeNull();
  });
});
