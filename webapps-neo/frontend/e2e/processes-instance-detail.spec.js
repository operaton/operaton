import { test, expect } from "./fixtures.js";

// Regression coverage for the runtime/history signal split
// (process.instance.{one,list,variables} vs history.*). Rendering a stale
// wrong-shape payload used to throw mid-render and blank the page, so these
// tests drive the live↔history flows and fail on ANY uncaught exception or
// (non-network) console error — the exact failure mode of the original bug.

// Collect uncaught exceptions and console errors for the whole test.
const watch_errors = (page) => {
  const page_errors = [],
    console_errors = [];
  page.on("pageerror", (e) => page_errors.push(e.message));
  page.on("console", (msg) => {
    if (msg.type() === "error") console_errors.push(msg.text());
  });
  return { page_errors, console_errors };
};

// Failed resource loads (optional plugin assets, favicon, backend 404s for
// endpoints an instance legitimately lacks) are not the render bug we guard.
const is_render_error = (text) =>
  !/favicon|plugin|Failed to load resource|net::ERR|status of 40\d/i.test(text);

const assert_clean = ({ page_errors, console_errors }) => {
  expect(page_errors, "uncaught exceptions during navigation").toEqual([]);
  expect(
    console_errors.filter(is_render_error),
    "render-time console errors",
  ).toEqual([]);
};

// Ask the engine for a real instance so the UI drive is deterministic, then
// build the same detail href the instance list would render. Returns null if
// the fixture engine has no instances (test then skips).
const BACKEND = process.env.E2E_BACKEND ?? "http://localhost:8084/engine-rest";
const find_instance_link = async (page) => {
  const res = await page.request.get(
    `${BACKEND}/history/process-instance?sortBy=startTime&sortOrder=desc&maxResults=1`,
    {
      headers: {
        Authorization:
          "Basic " + Buffer.from("demo:demo").toString("base64"),
      },
    },
  );
  if (!res.ok()) return null;
  const list = await res.json();
  if (!list.length) return null;
  const { id, processDefinitionId } = list[0];
  return `/processes/${processDefinitionId}/instances/${id}/vars`;
};

const with_history = (url) => {
  const u = new URL(url, "http://x");
  u.searchParams.set("history", "true");
  return u.pathname + u.search;
};

test.describe("processes instance detail (runtime/history signal split)", () => {
  test("live and history instance views render without throwing", async ({
    page,
  }) => {
    const errors = watch_errors(page);
    const instance_href = await find_instance_link(page);
    test.skip(!instance_href, "no seeded process instances");

    // Instance list rendered rows (process.instance.list → history list).
    const instances_url = instance_href.substring(
      0,
      instance_href.indexOf("/instances") + "/instances".length,
    );
    await page.goto(instances_url);
    await expect(page.locator('a[href*="/instances/"]').first()).toBeVisible();

    // Live detail: description (instance.one) + variables (instance.variables).
    await page.goto(instance_href);
    await expect(page.locator(".selected-instance .entity-id")).toBeVisible();
    await expect(page.locator("table").first()).toBeVisible();

    // Toggle the same instance to history mode: the historic payload has a
    // different shape and used to leak through the shared signal.
    await page.goto(with_history(instance_href));
    await expect(page.locator(".selected-instance .entity-id")).toBeVisible();
    await expect(page.locator("table").first()).toBeVisible();

    assert_clean(errors);
  });

  test("instance list renders in both live and history mode", async ({
    page,
  }) => {
    const errors = watch_errors(page);
    const instance_href = await find_instance_link(page);
    test.skip(!instance_href, "no seeded process instances");

    const instances_url = instance_href.substring(
      0,
      instance_href.indexOf("/instances") + "/instances".length,
    );
    await page.goto(instances_url);
    await expect(page.locator('a[href*="/instances/"]').first()).toBeVisible();
    await page.goto(with_history(instances_url));
    await expect(page.locator('a[href*="/instances/"]').first()).toBeVisible();

    assert_clean(errors);
  });

  test("starting a task then opening processes keeps the page rendered", async ({
    page,
  }) => {
    // The original report: start a task, switch to /processes → blank page.
    const errors = watch_errors(page);
    await page.goto("/tasks/start");
    await expect(page.locator("#start-task")).toBeVisible();
    const startable = page.locator('#start-task a[href^="/tasks/start/"]');
    if (await startable.count()) await startable.first().click();

    await page.goto("/processes");
    await expect(page.locator("main.processes")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: /deployed process definitions/i }),
    ).toBeVisible();

    assert_clean(errors);
  });
});
