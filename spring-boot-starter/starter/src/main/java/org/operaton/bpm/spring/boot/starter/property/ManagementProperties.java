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
package org.operaton.bpm.spring.boot.starter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties.joinOn;

@ConfigurationProperties("management")
public class ManagementProperties {

  private Health health = new Health();

  /**
   * @return the health
   */
  public Health getHealth() {
    return health;
  }

  /**
   * @param health the health to set
   */
  public void setHealth(Health health) {
    this.health = health;
  }

  @Override
  public String toString() {
    return "ManagementProperties [health=%s]".formatted(health);
  }

  public static class Health {

    private Operaton operaton = new Operaton();

    /**
     * @return the operaton
     */
    public Operaton getOperaton() {
      return operaton;
    }

    /**
     * @param operaton the operaton to set
     */
    public void setOperaton(Operaton operaton) {
      this.operaton = operaton;
    }

    @Override
    public String toString() {
      return joinOn(this.getClass())
        .add("operaton=" + operaton)
        .toString();
    }

    public class Operaton {
      private boolean enabled = true;

      /**
       * @return the enabled
       */
      public boolean isEnabled() {
        return enabled;
      }

      /**
       * @param enabled the enabled to set
       */
      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      @Override
      public String toString() {
        return joinOn(this.getClass())
          .add("enabled=" + enabled)
          .toString();
      }

    }
  }

}
