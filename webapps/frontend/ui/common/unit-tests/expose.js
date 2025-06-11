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

var $ = (window.jQuery = window.$ = require('jquery'));
var commons = require('operaton-commons-ui/lib');
var sdk = require('operaton-bpm-sdk-js/lib/angularjs/index');
var dataDepend = require('angular-data-depend');
var angular = require('operaton-commons-ui/vendor/angular');

window.angular = angular;
window.jquery = $;
window['operaton-commons-ui'] = commons;
window['operaton-bpm-sdk-js'] = sdk;
window['angular-data-depend'] = dataDepend;
window['moment'] = require('operaton-commons-ui/vendor/moment');
window['events'] = require('events');
window['cam-common'] = require('../scripts/module');
