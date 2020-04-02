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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.landingpage.mytasks.MyTask;
import com.cloudogu.scm.landingpage.mytasks.MyTaskProvider;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.dto.PullRequestMapper;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import org.apache.shiro.SecurityUtils;
import sonia.scm.EagerSingleton;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.cloudogu.scm.review.pullrequest.service.PullRequestStatus.OPEN;

@Extension(requires = "scm-landingpage-plugin")
@EagerSingleton
public class MyOpenTasks implements MyTaskProvider {
  private final RepositoryServiceFactory serviceFactory;
  private final RepositoryManager repositoryManager;
  private final PullRequestService pullRequestService;
  private final CommentService commentService;
  private final PullRequestMapper mapper;

  @Inject
  public MyOpenTasks(RepositoryServiceFactory serviceFactory, RepositoryManager repositoryManager, PullRequestService pullRequestService, CommentService commentService, PullRequestMapper mapper) {
    this.serviceFactory = serviceFactory;
    this.repositoryManager = repositoryManager;
    this.pullRequestService = pullRequestService;
    this.commentService = commentService;
    this.mapper = mapper;
  }

  @Override
  public Iterable<MyTask> getTasks() {
    String subject = SecurityUtils.getSubject().getPrincipal().toString();
    Collection<MyTask> result = new ArrayList<>();
    repositoryManager.getAll().stream().filter(this::supportsPullRequests).forEach(
      repository -> allPullRequestsFor(repository).stream()
        .filter(pr -> pr.getStatus() == OPEN)
        .filter(pr -> pr.getAuthor().equals(subject))
        .filter(pr -> commentService.getAll(repository.getNamespace(), repository.getName(), pr.getId()).stream()
          .anyMatch(comment -> comment.getType() == CommentType.TASK_TODO))
        .forEach(pr -> result.add(new MyPullRequestTodos(repository, pr, mapper)))
    );
    return result;
  }

  private List<PullRequest> allPullRequestsFor(Repository repository) {
    return pullRequestService.getAll(repository.getNamespace(), repository.getName());
  }

  private boolean supportsPullRequests(Repository repository) {
    try (RepositoryService repositoryService = serviceFactory.create(repository)) {
      return repositoryService.isSupported(Command.MERGE);
    }
  }
}
