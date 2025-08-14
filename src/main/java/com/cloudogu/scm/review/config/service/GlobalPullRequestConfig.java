/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.config.service;

import com.cloudogu.scm.review.config.WithDisableConfig;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "global-config")
public class GlobalPullRequestConfig extends BasePullRequestConfig implements WithDisableConfig {

  @XmlElement(name = "disable-repository-configuration")
  private boolean disableRepositoryConfiguration = false;

  public boolean isDisableRepositoryConfiguration() {
    return disableRepositoryConfiguration;
  }

  public void setDisableRepositoryConfiguration(boolean disableRepositoryConfiguration) {
    this.disableRepositoryConfiguration = disableRepositoryConfiguration;
  }
}
