#!/usr/bin/env node
// Writes docs/accessibility/COVERAGE.md from the route manifest and the state
// matrix.
//
// Deliberately unlike a11y-report.mjs: no engine, no dev server, no browser,
// no clock. It reads two modules and writes one file in a few milliseconds,
// which is why it is safe to run in CI and in a pre-commit check.
//
//   node e2e/a11y-coverage.mjs           write the file
//   node e2e/a11y-coverage.mjs --check   exit 1 if the file is out of date
//
// Unlike the report, --check DOES exit non-zero. The report is informational
// by contract; a coverage table that disagrees with routes.js is simply two
// files stating different facts.

import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, relative, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

import { build_coverage, render_coverage } from "./a11y-coverage.js";

const frontend = resolve(dirname(fileURLToPath(import.meta.url)), ".."),
  OUT_FILE = resolve(frontend, "docs/accessibility/COVERAGE.md");

const read_existing = async () => {
  try {
    return await readFile(OUT_FILE, "utf8");
  } catch (error) {
    if (error.code === "ENOENT") return null;
    throw error;
  }
};

const main = async () => {
  const check = process.argv.includes("--check"),
    markdown = render_coverage(build_coverage()),
    existing = await read_existing(),
    where = relative(frontend, OUT_FILE);

  if (check) {
    if (existing === markdown) {
      console.log(`✓ ${where} is up to date`);
      return 0;
    }
    console.error(
      existing === null
        ? `✖ ${where} does not exist — run \`npm run a11y:coverage\``
        : `✖ ${where} is out of date — run \`npm run a11y:coverage\``,
    );
    return 1;
  }

  await mkdir(dirname(OUT_FILE), { recursive: true });
  await writeFile(OUT_FILE, markdown, "utf8");
  console.log(
    existing === markdown ? `✓ ${where} unchanged` : `✓ wrote ${where}`,
  );
  return 0;
};

if (import.meta.url === pathToFileURL(process.argv[1]).href)
  main().then(
    (code) => process.exit(code),
    (error) => {
      console.error(error);
      process.exit(1);
    },
  );
