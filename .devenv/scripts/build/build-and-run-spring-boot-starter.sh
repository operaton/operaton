#!/usr/bin/env bash

# Copyright 2026 the Operaton contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

rm -rf ~/.m2/repository/org/operaton

./mvnw -f bom install
./mvnw -DskipTests -Dmaven.test.skip -am -pl \
spring-boot-starter/starter,\
spring-boot-starter/starter-client/spring,\
spring-boot-starter/starter-client/spring-boot,\
spring-boot-starter/starter-rest,\
spring-boot-starter/starter-security,\
spring-boot-starter/starter-test,\
spring-boot-starter/starter-test-junit5,\
spring-boot-starter/starter-webapp,\
spring-boot-starter/starter-webapp-core,\
examples/invoice,\
qa/arquillian-extensions,\
qa/integration-tests-webapps \
  install

./mvnw -Pdistro,integration-test-spring-boot-starter -f spring-boot-starter verify
