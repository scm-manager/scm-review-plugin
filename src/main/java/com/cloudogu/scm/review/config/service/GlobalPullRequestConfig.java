package com.cloudogu.scm.review.config.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "global-config")
public class GlobalPullRequestConfig extends PullRequestConfig {

  @XmlElement(name = "disable-repository-configuration")
  private boolean disableRepositoryConfiguration = false;

  public boolean isDisableRepositoryConfiguration() {
    return disableRepositoryConfiguration;
  }

  public void setDisableRepositoryConfiguration(boolean disableRepositoryConfiguration) {
    this.disableRepositoryConfiguration = disableRepositoryConfiguration;
  }
}
