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

package com.cloudogu.scm.review.config;


public class ConfigEvaluator {

  public static  <
    T,
    GLOBAL extends  WithDisableConfig,
    NAMESPACE extends WithDisableConfig & OverwritableConfig,
    REPOSITORY extends OverwritableConfig
    > T evaluate(GLOBAL globalConfig, NAMESPACE namespaceConfig, REPOSITORY repositoryConfig) {
    if (globalConfig.isDisableRepositoryConfiguration()) {
      return (T) globalConfig;
    }

    if (namespaceConfig.isOverwriteParentConfig()) {
      if (!namespaceConfig.isDisableRepositoryConfiguration() && repositoryConfig.isOverwriteParentConfig()) {
        return (T) repositoryConfig;
      }
      return (T) namespaceConfig;
    } else if (repositoryConfig.isOverwriteParentConfig() && !namespaceConfig.isDisableRepositoryConfiguration()) {
      return (T) repositoryConfig;
    }

    return (T) globalConfig;
  }
}
