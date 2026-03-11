const path = require('path');

// Load the real `ids` implementation directly from node_modules to avoid
// webpack alias recursion (webpack aliases 'ids' -> this file).
let idsPkg;
try {
  idsPkg = require(path.resolve(__dirname, 'node_modules/ids/dist/index.js'));
} catch (e) {
  // fallback: try plain require in case node resolves differently
  try { idsPkg = require('ids'); } catch (e2) { idsPkg = null; }
}

function isFn(v) { return typeof v === 'function'; }

// Resolve constructor from known shapes
let Ctor = null;
if (isFn(idsPkg)) {
  Ctor = idsPkg;
} else if (idsPkg && isFn(idsPkg.Ids)) {
  Ctor = idsPkg.Ids;
} else if (idsPkg && isFn(idsPkg.default)) {
  Ctor = idsPkg.default;
} else if (idsPkg && idsPkg.Ids && isFn(idsPkg.Ids.default)) {
  Ctor = idsPkg.Ids.default;
} else if (idsPkg && idsPkg.default && isFn(idsPkg.default.Ids)) {
  Ctor = idsPkg.default.Ids;
}

// Last resort: pick any function property
if (!Ctor && idsPkg && typeof idsPkg === 'object') {
  for (const k of Object.keys(idsPkg)) {
    if (isFn(idsPkg[k])) { Ctor = idsPkg[k]; break; }
  }
}

// Build namespace export: ensure .Ids and .default are constructors
const ns = {};
if (isFn(Ctor)) {
  ns.Ids = Ctor;
  ns.default = Ctor;
} else {
  // if no ctor found, attempt to export the package namespace as-is
  Object.assign(ns, idsPkg || {});
  if (!ns.Ids && isFn(ns.default)) ns.Ids = ns.default;
  if (!ns.default && isFn(ns.Ids)) ns.default = ns.Ids;
}
ns.__esModule = true;

module.exports = ns;
