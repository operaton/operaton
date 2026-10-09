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

'use strict';

const production = require('./webpack.common')({}, {});

module.exports = function (config) {
  config.set({
    frameworks: ['mocha', 'webpack'],
    files: ['ui/common/unit-tests/expose.js', 'ui/**/unit-tests/*.spec.js'],
    preprocessors: {
      'ui/common/unit-tests/expose.js': ['webpack'],
      'ui/**/unit-tests/*.spec.js': ['webpack'],
    },
    webpack: {
      context: __dirname,
      mode: 'development',
      devtool: 'inline-source-map',
      resolve: production.resolve,
      module: production.module,
      stats: 'errors-warnings',
    },
    reporters: ['progress'],
    browsers: ['ChromeHeadless'],
    concurrency: 1,
    singleRun: true,
  });
};
