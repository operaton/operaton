/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const js = require('@eslint/js');
const prettierPlugin = require('eslint-plugin-prettier');
const prettierConfig = require('eslint-config-prettier');
const babelParser = require('@babel/eslint-parser');

module.exports = [
  // Global ignores - replaces .eslintignore
  {
    ignores: [
      'node_modules/**/*.js',
      '**/*test*/**/*.js',
      'target/',
      'node/',
      // Ignore JSON files from ESLint (they should be validated by other tools)
      '**/*.json',
    ],
  },

  // Base recommended config from ESLint
  js.configs.recommended,

  // Main configuration for JavaScript files
  {
    files: ['**/*.js'],
    languageOptions: {
      ecmaVersion: 2020,
      sourceType: 'module',
      parser: babelParser,
      parserOptions: {
        requireConfigFile: false,
        babelOptions: {
          presets: ['@babel/preset-env'],
        },
      },
      globals: {
        // Node environment
        process: 'readonly',
        __dirname: 'readonly',
        __filename: 'readonly',
        module: 'readonly',
        require: 'readonly',
        exports: 'readonly',
        console: 'readonly',
        Buffer: 'readonly',
        global: 'readonly',

        // Browser environment
        window: 'readonly',
        document: 'readonly',
        navigator: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        setTimeout: 'readonly',
        setInterval: 'readonly',
        clearTimeout: 'readonly',
        clearInterval: 'readonly',
        fetch: 'readonly',
        top: 'readonly',
        FormData: 'readonly',
        Blob: 'readonly',
        FileReader: 'readonly',
        btoa: 'readonly',
        atob: 'readonly',
        getComputedStyle: 'readonly',

        // Test globals
        expect: 'readonly',
        it: 'readonly',
        describe: 'readonly',
        beforeEach: 'writable',
        afterEach: 'writable',
        before: 'writable',
        after: 'writable',

        // RequireJS
        requirejs: 'readonly',
        define: 'readonly',
      },
    },
    plugins: {
      prettier: prettierPlugin,
    },
    rules: {
      ...prettierConfig.rules,
      'prettier/prettier': 'error',
      // Removed space-before-blocks, space-before-function-paren, and semi
      // as they conflict with prettier's formatting
      'no-console': 1,
      'no-self-assign': 0,
      'no-useless-escape': 'off',
      // Allow unused vars that start with underscore (common pattern for intentionally unused params)
      'no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_',
        },
      ],
    },
  },
];
