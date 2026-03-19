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