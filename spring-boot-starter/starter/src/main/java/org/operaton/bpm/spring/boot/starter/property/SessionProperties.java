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
package org.operaton.bpm.spring.boot.starter.property;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties.joinOn;

public class SessionProperties {

  @NestedConfigurationProperty
  protected Cookie cookie = new Cookie();

  public Cookie getCookie() {
    return cookie;
  }

  public void setCookie(Cookie cookie) {
    this.cookie = cookie;
  }

  @Override
  public String toString() {
    return joinOn(this.getClass())
            .add("cookie=" + cookie)
            .toString();
  }

  public static class Cookie {

    protected String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return joinOn(this.getClass())
              .add("name='" + name + '\'')
              .toString();
    }
  }
}