/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { test, expect } from '@playwright/test';

// A large deployment, including historical versions of only 14 process keys.
// Most historical versions have no running instances and need no count call.
const definitions = Array.from({ length: 2369 }, (_, index) => {
  const key_number = String((index % 14) + 1).padStart(2, '0');
  const version = Math.floor(index / 14) + 1;
  return {
    id: `flow-${key_number}:${version}:deployment-${index}`,
    definition: {
      id: `flow-${key_number}:${version}:deployment-${index}`,
      key: `flow-${key_number}`,
      name: `Flow ${key_number}`,
      version,
      tenantId: null,
      suspended: false,
    },
    instances: index % 3 === 0 ? 3 : 0,
    failedJobs: 0,
    incidents: [],
  };
});
const by_id = new Map(definitions.map((row) => [row.definition.id, row]));
const by_name = [...definitions].sort((a, b) =>
  a.definition.name.localeCompare(b.definition.name),
);
const ids = (rows) => rows.map((row) => row.definition.id);
const counted_ids = (rows) => ids(rows.filter((row) => row.instances > 0));
const table_rows = (page) => page.locator('main.processes tbody > tr');
const paginator = (page) =>
  page.getByRole('navigation', { name: 'Process definition pages' });

const visible_ids = (page) =>
  table_rows(page).evaluateAll((rows) =>
    rows.map((row) => {
      const path = new URL(row.querySelector('a').href).pathname;
      return decodeURIComponent(path.split('/')[2]);
    }),
  );

const mock_backend = async (page, { fail_counts = new Set() } = {}) => {
  const calls = { counts: [], suspensions: [], unexpected: [], errors: [] };
  page.on('pageerror', (error) => calls.errors.push(error.message));
  await page.addInitScript(() => {
    localStorage.setItem('i18nextLng', 'en-US');
  });
  await page.route('**/config.json', (route) =>
    route.fulfill({
      json: {
        authMode: 'oauth2',
        oauth: { flow: 'session', login: '/login', logout: '/logout' },
        user: { id: 'regression-user' },
        engine: 'default',
        backends: [{ name: 'Regression', url: '' }],
        remotePluginsEnabled: false,
      },
    }),
  );
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/api/admin/auth/user/default') {
      return route.fulfill({
        json: {
          userId: 'regression-user',
          authorizedApps: ['neo', 'cockpit'],
        },
      });
    }
    const resource = url.pathname.replace('/api/engine/engine/default', '');
    if (resource === '/process-definition/statistics') {
      // Deliberately return all versions even when nameLike/sortBy is supplied:
      // the real statistics endpoint ignores those list-query parameters.
      return route.fulfill({ json: definitions });
    }
    if (resource === '/process-instance/count') {
      const id = url.searchParams.get('processDefinitionId');
      calls.counts.push(id);
      expect(url.searchParams.get('suspended')).toBe('true');
      expect(by_id.has(id)).toBe(true);
      if (fail_counts.has(id)) return route.abort('failed');
      return route.fulfill({ json: { count: 1 } });
    }
    if (request.method() === 'PUT' && resource.endsWith('/suspended')) {
      calls.suspensions.push(
        decodeURIComponent(resource.split('/')[2]),
      );
      return route.fulfill({ status: 204 });
    }
    calls.unexpected.push(`${request.method()} ${url.pathname}`);
    return route.fulfill({ status: 500, json: { message: 'Unexpected API' } });
  });
  return calls;
};

const open_processes = async (page) => {
  await page.goto('/processes');
  await expect(
    page.getByRole('heading', { name: /deployed process definitions/i }),
  ).toBeVisible();
  await expect(table_rows(page)).toHaveCount(50);
  await page.waitForLoadState('networkidle');
};

test('2369 versions only count visible nonempty rows', async ({ page }) => {
  const calls = await mock_backend(page);
  await open_processes(page);

  expect(await visible_ids(page)).toEqual(ids(by_name.slice(0, 50)));
  expect(calls.counts.sort()).toEqual(counted_ids(by_name.slice(0, 50)).sort());
  await expect(paginator(page)).toContainText('2369');
  await expect(
    paginator(page).getByRole('button', { name: 'Previous' }),
  ).toBeDisabled();

  calls.counts.length = 0;
  await paginator(page).getByRole('button', { name: 'Next' }).click();
  await expect.poll(() => visible_ids(page))
    .toEqual(ids(by_name.slice(50, 100)));
  await page.waitForLoadState('networkidle');
  expect(calls.counts.sort())
    .toEqual(counted_ids(by_name.slice(50, 100)).sort());
  await expect(table_rows(page)).toHaveCount(50);

  await paginator(page).getByRole('button', { name: 'Previous' }).click();
  await expect.poll(() => visible_ids(page)).toEqual(ids(by_name.slice(0, 50)));
  expect(calls.unexpected).toEqual([]);
  expect(calls.errors).toEqual([]);
});

test('search and sorting preserve historical versions', async ({ page }) => {
  const calls = await mock_backend(page);
  await open_processes(page);
  await paginator(page).getByRole('button', { name: 'Next' }).click();
  await page.getByRole('searchbox', { name: 'Search by name' }).fill('flow-14');
  const matching = definitions.filter(
    (row) => row.definition.key === 'flow-14',
  );
  await expect.poll(() => visible_ids(page))
    .toEqual(ids(matching.slice(0, 50)));
  await expect(
    paginator(page).getByRole('button', { name: 'Previous' }),
  ).toBeDisabled();

  await page.getByLabel('Sort by', { exact: true }).selectOption('version');
  await page.getByLabel('Sort order', { exact: true }).selectOption('desc');
  const descending = [...matching].reverse();
  await expect.poll(() => visible_ids(page))
    .toEqual(ids(descending.slice(0, 50)));
  for (let number = 1; number <= 3; number++) {
    await paginator(page).getByRole('button', { name: 'Next' }).click();
    await expect.poll(() => visible_ids(page)).toEqual(
      ids(descending.slice(number * 50, (number + 1) * 50)),
    );
  }
  await expect(table_rows(page)).toHaveCount(19);
  await expect(
    paginator(page).getByRole('button', { name: 'Next' }),
  ).toBeDisabled();
  expect((await visible_ids(page)).at(-1)).toBe(matching[0].definition.id);
  await page.waitForLoadState('networkidle');
  expect(calls.unexpected).toEqual([]);
  expect(calls.errors).toEqual([]);
});

test('bulk actions use the current visible selection', async ({ page }) => {
  const calls = await mock_backend(page);
  await open_processes(page);
  const select_all = page.getByRole('checkbox', {
    name: 'Select all', exact: true,
  });
  const suspend = page.getByRole('button', { name: 'Suspend', exact: true });
  await select_all.check();
  await expect(page.getByText('50 selected', { exact: true })).toBeVisible();
  await paginator(page).getByRole('button', { name: 'Next' }).click();
  await expect(select_all).not.toBeChecked();
  await expect(suspend).toBeDisabled();
  await select_all.check();
  await suspend.click();
  await page.getByRole('dialog')
    .getByRole('button', { name: 'OK', exact: true }).click();
  await expect.poll(() => calls.suspensions.length).toBe(50);
  expect(calls.suspensions.sort()).toEqual(ids(by_name.slice(50, 100)).sort());
  await expect(suspend).toBeDisabled();

  await select_all.check();
  await page.getByLabel('Sort by', { exact: true }).selectOption('version');
  await expect(suspend).toBeDisabled();
  await select_all.check();
  await page.getByRole('searchbox', { name: 'Search by name' }).fill('Flow 14');
  await expect(suspend).toBeDisabled();
  await expect(select_all).not.toBeChecked();
  expect(calls.unexpected).toEqual([]);
  expect(calls.errors).toEqual([]);
});

test('count failures do not reject unhandled promises', async ({ page }) => {
  const failing_id = by_name[0].definition.id;
  const calls = await mock_backend(page, {
    fail_counts: new Set([failing_id]),
  });
  await open_processes(page);
  expect(calls.counts).toContain(failing_id);
  expect(calls.errors).toEqual([]);
  expect(calls.unexpected).toEqual([]);
  // Failed counts must not claim that every instance is active.
  await expect(table_rows(page).first().locator('td').nth(3)).toHaveText('—/—');
  await paginator(page).getByRole('button', { name: 'Next' }).click();
  await expect.poll(() => visible_ids(page))
    .toEqual(ids(by_name.slice(50, 100)));
  await page.waitForLoadState('networkidle');
  expect(calls.errors).toEqual([]);
});
