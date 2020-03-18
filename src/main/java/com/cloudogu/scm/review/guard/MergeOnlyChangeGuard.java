/**
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
package com.cloudogu.scm.review.guard;

import com.cloudogu.scm.editor.ChangeGuard;
import com.cloudogu.scm.editor.ChangeObstacle;
import com.cloudogu.scm.review.config.service.ConfigService;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

@Extension(requires = "scm-editor-plugin")
public class MergeOnlyChangeGuard implements ChangeGuard {

  private final RepositoryManager repositoryManager;
  private final ConfigService configService;

  @Inject
  public MergeOnlyChangeGuard(RepositoryManager repositoryManager, ConfigService configService) {
    this.repositoryManager = repositoryManager;
    this.configService = configService;
  }

  @Override
  public Collection<ChangeObstacle> getObstacles(NamespaceAndName namespaceAndName, String branch, Changes changes) {
    Repository repository = repositoryManager.get(namespaceAndName);
    if (configService.isBranchProtected(repository, branch)) {
      return Collections.singleton(new ChangeObstacle() {
        @Override
        public String getMessage() {
          return "The branch " + branch + " can only be written using pull requests";
        }

        @Override
        public String getKey() {
          return "scm-review-plugin.obstacle";
        }
      });
    } else {
      return Collections.emptyList();
    }
  }
}
