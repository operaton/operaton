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

var dialogTemplate = require('./cancel-process-instance-dialog.html?raw');
var actionTemplate = require('./cancel-process-instance-action.html?raw');

module.exports = function(ngModule) {
  ngModule.controller('CancelProcessInstanceActionController', [
    '$scope',
    'search',
    'Uri',
    '$uibModal',
    function($scope, search, Uri, $modal) {
      $scope.openDialog = function() {
        $modal
          .open({
            resolve: {
              processData: function() {
                return $scope.processData;
              },
              processInstance: function() {
                return $scope.processInstance;
              }
            },
            controller: 'CancelProcessInstanceController',
            template: dialogTemplate
          })
          .result.catch(function() {});
      };
    }
  ]);

  var Configuration = function PluginConfiguration(ViewsProvider) {
    ViewsProvider.registerDefaultView(
      'cockpit.processInstance.runtime.action',
      {
        id: 'cancel-process-instance-action',
        label: 'PLUGIN_CANCEL_PROCESS_DELETE_ACTION',
        template: actionTemplate,
        controller: 'CancelProcessInstanceActionController',
        priority: 20
      }
    );
  };

  Configuration.$inject = ['ViewsProvider'];

  ngModule.config(Configuration);
};
