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

'use strict';

var angular = require('operaton-commons-ui/vendor/angular'),
  camLayoutCtrl = require('./controllers/cam-layout-ctrl'),
  camHeaderViewsCtrl = require('./controllers/cam-header-views-ctrl');

require('operaton-commons-ui/lib/util/index');

var navigationModule = angular.module('cam.tasklist.navigation', [
  require('operaton-commons-ui/lib/util/index').name,
  'ui.bootstrap',
]);

navigationModule.controller('camHeaderViewsCtrl', camHeaderViewsCtrl);
navigationModule.controller('camLayoutCtrl', camLayoutCtrl);

module.exports = navigationModule;
