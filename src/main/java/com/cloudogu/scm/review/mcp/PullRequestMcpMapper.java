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

package com.cloudogu.scm.review.mcp;

import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.cloudogu.scm.review.pullrequest.dto.ReviewerDto;
import com.cloudogu.scm.review.pullrequest.dto.TasksDto;
import com.cloudogu.scm.review.pullrequest.service.MergeObstacle;
import com.cloudogu.scm.review.pullrequest.service.MergeService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import jakarta.inject.Inject;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import sonia.scm.repository.Repository;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper
@SuppressWarnings("java:S6813") // we cannot use constructor injection here
public abstract class PullRequestMcpMapper {
  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private CommentService commentService;
  @Inject
  private MergeService mergeService;

  public abstract PullRequestOverviewMcp mapOverview(PullRequest pullRequest, @Context Repository repository);

  public abstract PullRequestDetailMcp mapDetails(PullRequest pullRequest, @Context Repository repository);

  Set<ReviewerDto> mapReviewer(Map<String, Boolean> reviewer) {
    return reviewer
      .entrySet()
      .stream()
      .map(entry -> this.createReviewerDto(getUserIfAvailable(entry), entry.getValue()))
      .collect(Collectors.toSet());
  }

  private DisplayUser getUserIfAvailable(Map.Entry<String, Boolean> entry) {
    return userDisplayManager
      .get(entry.getKey())
      .orElse(DisplayUser.from(new User(entry.getKey(), entry.getKey(), null)));
  }

  DisplayedUserDto mapUser(String authorId) {
    return userDisplayManager.get(authorId).map(this::createDisplayedUserDto).orElse(new DisplayedUserDto(authorId, authorId, null));
  }

  @AfterMapping
  void mapTasks(@MappingTarget PullRequestDetailMcp target, PullRequest pullRequest, @Context Repository repository) {
    commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId());
    target.setTasks(
      new TasksDto(
        commentService.getCount(repository.getNamespace(), repository.getName(), pullRequest.getId(), CommentType.TASK_TODO),
        commentService.getCount(repository.getNamespace(), repository.getName(), pullRequest.getId(), CommentType.TASK_DONE)
      )
    );
  }

  @AfterMapping
  void mapRepository(@MappingTarget PullRequestOverviewMcp target, @Context Repository repository) {
    target.setRepository(new PullRequestOverviewMcp.Repository(repository.getNamespace(), repository.getName()));
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName(), user.getMail());
  }

  private ReviewerDto createReviewerDto(DisplayUser user, Boolean approved) {
    return new ReviewerDto(user.getId(), user.getDisplayName(), user.getMail(), approved);
  }

  public PullRequestDetailMcp mapWithObstacles(PullRequest pullRequest, Repository repository) {
    mergeService.getObstacles(repository, pullRequest);
    PullRequestDetailMcp pullRequestDetailMcp = mapDetails(pullRequest, repository);
    pullRequestDetailMcp.setMergeObstacles(
      mergeService.getObstacles(repository, pullRequest)
        .stream()
        .map(MergeObstacle::getMessage)
        .toList()
    );
    return pullRequestDetailMcp;
  }
}
