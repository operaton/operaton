let Ids = require('ids');

if (Ids?.default) {
  Ids = Ids.default;
}

// EXPORT 1: Direct export for CommonJS (form-js uses this)
// This allows: var Ids = require('ids'); new Ids();
module.exports = Ids;

// EXPORT 2: Named property for ESM Named Imports (dmn-migrate uses this)
// This allows: import { Ids } from 'ids';
module.exports.Ids = Ids;

// EXPORT 3: Default property for ESM Default Imports (bpmn-js uses this)
// This allows: import Ids from 'ids';
module.exports.default = Ids;
