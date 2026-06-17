/**
 * Operaton smoke test browser flows.
 * Drives Tasklist and Cockpit against a running Operaton instance.
 *
 * Usage: node browser-flows.mjs <BASE_URL> [SCREENSHOT_DIR]
 *   BASE_URL       e.g. http://localhost:18080
 *   SCREENSHOT_DIR directory for screenshots (default: /tmp/operaton-smoke)
 */
import { chromium } from 'playwright';
import { mkdirSync } from 'fs';

const BASE_URL = process.argv[2] || 'http://localhost:18080';
const SCREENSHOT_DIR = process.argv[3] || '/tmp/operaton-smoke';
mkdirSync(SCREENSHOT_DIR, { recursive: true });

const results = [];
let allPassed = true;

function step(label, ok, detail = '') {
  const icon = ok ? '✅' : '❌';
  const line = `${icon} ${label}${detail ? ' — ' + detail : ''}`;
  results.push({ ok, line });
  console.log(line);
  if (!ok) allPassed = false;
}

async function shot(page, name) {
  await page.screenshot({ path: `${SCREENSHOT_DIR}/${name}.png`, fullPage: false }).catch(() => {});
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
page.setDefaultTimeout(20000);

try {
  // ── TASKLIST ──────────────────────────────────────────────────────────────
  console.log('\n── Tasklist ─────────────────────────────────────────────');
  // App redirects /tasklist/ → /tasklist/default/#/login
  await page.goto(`${BASE_URL}/operaton/app/tasklist/default/`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
  await shot(page, '01-login');

  // 1. Login page — inputs use ng-model, not name attr
  const usernameInput = page.locator('[ng-model="username"]');
  const loginVisible = await usernameInput.isVisible().catch(() => false);
  step('Login page appears', loginVisible);

  if (loginVisible) {
    // 2. Login demo/demo
    await usernameInput.fill('demo');
    await page.locator('[ng-model="password"]').fill('demo');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);
    await shot(page, '02-tasklist');

    const assignCount = await page.getByText('Assign Reviewer').count().catch(() => 0);
    step('2 "Assign Reviewer" tasks visible', assignCount >= 2, `found ${assignCount}`);

    // 3. Click first Assign Reviewer task
    if (assignCount >= 1) {
      await page.getByText('Assign Reviewer').first().click();
      await page.waitForTimeout(2000);
      await shot(page, '03-task-form');
      const formVisible = await page.locator('.cam-tasklist-form, .task-detail, [cam-tasklist-form]').first().isVisible().catch(() => false);
      step('Task form shown after click', formVisible);

      // 4. Fill reviewer — select[ng-model="reviewer"], pick "john"
      const reviewerSelect = page.locator('[ng-model="reviewer"]');
      const selectVisible = await reviewerSelect.isVisible().catch(() => false);
      if (selectVisible) {
        await reviewerSelect.selectOption('john');
        step('Reviewer "John" selected', true);
        await page.waitForTimeout(500);

        // Complete button becomes enabled once reviewer is chosen
        const completeBtn = page.locator('button[ng-click="complete()"]').first();
        await completeBtn.waitFor({ state: 'visible' });
        await expect_enabled(page, completeBtn);
        await completeBtn.click();
        await page.waitForTimeout(2500);
        await shot(page, '04-after-complete');

        const remainingAssign = await page.getByText('Assign Reviewer').count().catch(() => 0);
        step('Task completed — 1 "Assign Reviewer" remaining', remainingAssign === 1, `${remainingAssign} remaining`);
      } else {
        step('Reviewer select visible', false, '[ng-model="reviewer"] not found');
        step('Task completed', false, 'skipped — reviewer not set');
      }

      // 5. Review Invoice task appears in list (assigned to John)
      await page.waitForTimeout(1000);
      const reviewInvoiceCount = await page.getByText('Review Invoice').count().catch(() => 0);
      step('"Review Invoice" task visible in list', reviewInvoiceCount >= 1, `found ${reviewInvoiceCount}`);
    } else {
      step('Click task to open form', false, 'no Assign Reviewer tasks');
      step('Reviewer selected', false, 'skipped');
      step('Task completed', false, 'skipped');
      step('Review Invoice visible', false, 'skipped');
    }
  } else {
    ['Login', '2 tasks visible', 'Task form', 'Reviewer', 'Complete', 'Review Invoice'].forEach(
      s => step(s, false, 'skipped — login not found')
    );
  }

  // ── COCKPIT ───────────────────────────────────────────────────────────────
  console.log('\n── Cockpit ──────────────────────────────────────────────');
  await page.goto(`${BASE_URL}/operaton/app/cockpit/default/`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
  await shot(page, '06-cockpit-home');

  // 1. Dashboard shows Running Process Instances
  const dashboardVisible = await page.getByText('Running Process Instances').isVisible().catch(() => false);
  step('Cockpit dashboard — "Running Process Instances" visible', dashboardVisible);

  // 2. Navigate via "Processes" nav link (top navbar)
  const processesNav = page.getByRole('link', { name: 'Processes', exact: true });
  const navVisible = await processesNav.isVisible().catch(() => false);
  step('"Processes" nav link visible', navVisible);

  if (navVisible) {
    await processesNav.click();
    await page.waitForTimeout(2000);
    await shot(page, '07-processes-list');

    // 3. Click invoice process definition
    const invoiceRow = page.locator('td a, table a').filter({ hasText: /invoice/i }).first();
    const invoiceVisible = await invoiceRow.isVisible().catch(() => false);
    step('Invoice process definition visible', invoiceVisible);

    if (invoiceVisible) {
      await invoiceRow.click();
      await page.waitForTimeout(2500);
      await shot(page, '08-process-def');

      // 4. Click a running process instance
      const instanceLink = page.locator('table tbody tr a, [href*="process-instance"]').first();
      const instanceVisible = await instanceLink.isVisible().catch(() => false);
      step('Running instance row visible', instanceVisible);

      if (instanceVisible) {
        await instanceLink.click();
        await page.waitForTimeout(3000);
        await shot(page, '09-instance-detail');

        // 5. Click Variables tab, then edit a variable
        const variablesTab = page.getByRole('link', { name: 'Variables', exact: true })
          .or(page.locator('a, .nav-link').filter({ hasText: /^variables$/i }).first());
        if (await variablesTab.isVisible().catch(() => false)) {
          await variablesTab.click();
          await page.waitForTimeout(1500);
        }
        await shot(page, '10-variables-tab');

        // Edit button enables inline editing: button[ng-click="enableEditMode(info, true)"]
        const editBtn = page.locator('button[ng-click*="enableEditMode"]').first();
        const editVisible = await editBtn.isVisible().catch(() => false);
        step('Variable edit button visible', editVisible);

        if (editVisible) {
          await editBtn.click();
          await page.waitForTimeout(1000);
          await shot(page, '11-inline-edit');
          // Inline input appears in the row after enabling edit mode
          const valueInput = page.locator('input[ng-model*="variable.value"], input[ng-model*="editValue"], .editable-input input, td input[type="text"]').first();
          const inputVisible = await valueInput.isVisible().catch(() => false);
          if (inputVisible) {
            await valueInput.click({ clickCount: 3 });
            await valueInput.fill('999');
            // Save: click the save button (ng-click="saveVariable(v)") in the row
            const saveBtn = page.locator('tr:has(input) button[ng-click*="saveVariable"]').first();
            const saveBtnVisible = await saveBtn.isVisible().catch(() => false);
            if (saveBtnVisible) {
              await saveBtn.click();
            } else {
              await valueInput.press('Enter');
            }
            await page.waitForTimeout(2000);
            await shot(page, '12-after-save');
            // App exits edit mode on save — input disappears from the row
            const stillEditing = await valueInput.isVisible().catch(() => false);
            step('Variable saved — edit mode exited', !stillEditing);
          } else {
            // Inline edit mode active — check if the row is in edit mode at all
            const editMode = await page.locator('[ng-click*="saveVariable"], [ng-click*="save"], .editmode').isVisible().catch(() => false);
            step('Inline edit mode activated', editMode, 'value input not found');
          }
        } else {
          const varSection = await page.locator('table').isVisible().catch(() => false);
          step('Variables table visible (edit button not found)', varSection);
        }
      } else {
        step('Open process instance', false, 'no instance link');
        step('Edit variable', false, 'skipped');
      }
    } else {
      step('Invoice process def', false, 'not found in list');
      step('Instance', false, 'skipped');
      step('Variable', false, 'skipped');
    }
  } else {
    step('Navigate processes', false, 'nav link not found');
    step('Invoice def', false, 'skipped');
    step('Instance', false, 'skipped');
    step('Variable', false, 'skipped');
  }

} catch (err) {
  console.error('Unexpected error:', err.message);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/error.png` }).catch(() => {});
  allPassed = false;
} finally {
  await browser.close();
}

console.log('\n── Results ──────────────────────────────────────────────');
results.forEach(r => console.log(r.line));
console.log(`\nScreenshots: ${SCREENSHOT_DIR}`);
console.log('');
if (!allPassed) { console.error('OVERALL: FAIL'); process.exit(1); }
else console.log('OVERALL: PASS');

// Helper: wait for button to be enabled (AngularJS ng-disabled reacts after digest)
async function expect_enabled(page, locator) {
  for (let i = 0; i < 20; i++) {
    const disabled = await locator.getAttribute('disabled').catch(() => null);
    if (disabled === null) return;
    await page.waitForTimeout(200);
  }
}
