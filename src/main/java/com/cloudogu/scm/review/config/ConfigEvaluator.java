/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
