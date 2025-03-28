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

package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.config.service.BasePullRequestConfig;
import com.cloudogu.scm.review.config.service.ConfigService;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import com.google.common.base.Strings;
import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.shiro.SecurityUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory;
import sonia.scm.api.v2.resources.BaseMapper;
import sonia.scm.api.v2.resources.BranchLinkProvider;
import sonia.scm.repository.Branch;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;
import sonia.scm.web.EdisonHalAppender;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUser;
import static com.cloudogu.scm.review.pullrequest.dto.PullRequestCheckResultDto.PullRequestCheckStatus.PR_VALID;
import static de.otto.edison.hal.Embedded.embeddedBuilder;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;

@Mapper(
  builder = @Builder(disableBuilder = true)
)
@SuppressWarnings("java:S6813") // we cannot use constructor injection here
public abstract class PullRequestMapper extends BaseMapper<PullRequest, PullRequestDto> {
  @Inject
  private UserDisplayManager userDisplayManager;
  @Inject
  private ConfigService configService;
  @Inject
  private PullRequestService pullRequestService;
  @Inject
  private CommentService commentService;
  @Inject
  private RepositoryServiceFactory serviceFactory;
  @Inject
  private BranchLinkProvider branchLinkProvider;
  @Inject
  private BranchResolver branchResolver;

  private PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(() -> URI.create("/"));

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "reviewer", source = "reviewer")
  @Mapping(target = "author", source = "author")
  @Mapping(target = "reviser", source = "reviser")
  @Mapping(target = "markedAsReviewed", ignore = true)
  public abstract PullRequestDto map(PullRequest pullRequest, @Context Repository repository);

  @Mapping(target = "subscriber", ignore = true)
  @Mapping(target = "reviewer", source = "reviewer")
  @Mapping(target = "reviewMarks", ignore = true)
  public abstract PullRequest map(PullRequestDto dto);

  Map<String, Boolean> mapReviewerFromDto(Set<ReviewerDto> reviewer) {
    Map<String, Boolean> reviewerMap = new HashMap<>();

    for (ReviewerDto singleReviewer : reviewer) {
      Optional<DisplayUser> displayUser = userDisplayManager.get(singleReviewer.getId());
      if (!displayUser.isPresent()) {
        continue;
      }
      reviewerMap.put(displayUser.get().getId(), singleReviewer.isApproved());
    }

    return reviewerMap;
  }

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

  String mapUser(DisplayedUserDto author) {
    if (author == null) {
      return null;
    } else {
      return author.getId();
    }
  }

  public PullRequestMapper using(UriInfo uriInfo) {
    pullRequestResourceLinks = new PullRequestResourceLinks(uriInfo::getBaseUri);
    return this;
  }

  @AfterMapping
  void mapTasks(@MappingTarget PullRequestDto target, PullRequest pullRequest, @Context Repository repository) {
    List<Comment> comments = commentService.getAll(repository.getNamespace(), repository.getName(), pullRequest.getId());
    target.setTasks(
      new TasksDto(
        countCommentsByFilter(comments, c -> c.getType() == CommentType.TASK_TODO),
        countCommentsByFilter(comments, c -> c.getType() == CommentType.TASK_DONE)
      )
    );
  }

  @AfterMapping
  void mapReviewMarks(@MappingTarget PullRequestDto target, PullRequest pullRequest) {
    List<String> filesMarkedAsReviewed = pullRequest.getReviewMarks()
      .stream()
      .filter(mark -> mark.getUser().equals(getCurrentUser().getId()))
      .map(ReviewMark::getFile)
      .toList();
    target.setMarkedAsReviewed(filesMarkedAsReviewed);
  }

  private long countCommentsByFilter(List<Comment> comments, Predicate<Comment> filter) {
    return comments.stream().filter(filter).count();
  }

  @ObjectFactory
  PullRequestDto createDto(PullRequest pullRequest, @Context Repository repository) {
    String namespace = repository.getNamespace();
    String name = repository.getName();
    String pullRequestId = pullRequest.getId();
    Links.Builder linksBuilder = linkingTo().self(pullRequestResourceLinks.pullRequest()
      .self(namespace, name, pullRequestId));
    linksBuilder.single(link("comments", pullRequestResourceLinks.pullRequestComments()
      .all(namespace, name, pullRequestId)));
    linksBuilder.single(link("changes", pullRequestResourceLinks.pullRequestChanges()
      .readAll(namespace, name, pullRequestId)));
    linksBuilder.single(link("events", pullRequestResourceLinks.pullRequest()
      .events(namespace, name, pullRequestId)));
    if (PermissionCheck.mayComment(repository) && CurrentUserResolver.getCurrentUser() != null) {
      if (pullRequest.isOpen()) {
        if (pullRequestService.hasUserApproved(repository, pullRequest.getId())) {
          linksBuilder.single(link("disapprove", pullRequestResourceLinks.pullRequest().disapprove(namespace, name, pullRequestId)));
        } else {
          linksBuilder.single(link("approve", pullRequestResourceLinks.pullRequest().approve(namespace, name, pullRequestId)));
        }
      }

      if (!Strings.isNullOrEmpty(CurrentUserResolver.getCurrentUser().getMail())) {
        linksBuilder.single(link("subscription", pullRequestResourceLinks.pullRequest()
          .subscription(namespace, name, pullRequestId)));
      }
    }
    if (PermissionCheck.mayModifyPullRequest(repository, pullRequest) && pullRequest.isInProgress()) {
      linksBuilder.single(link("update", pullRequestResourceLinks.pullRequest()
        .update(namespace, name, pullRequestId)));
      linksBuilder.single(link("check", pullRequestResourceLinks.pullRequest()
        .check(namespace, name, pullRequestId)));
    }
    if (PermissionCheck.mayMerge(repository) && pullRequest.isInProgress() && RepositoryPermissions.push(repository).isPermitted() && (pullRequest.isOpen())) {
      linksBuilder.single(link("defaultCommitMessage", pullRequestResourceLinks.mergeLinks()
        .createDefaultCommitMessage(namespace, name, pullRequest.getId())));
      linksBuilder.single(link("mergeStrategyInfo", pullRequestResourceLinks.mergeLinks().getMergeStrategyInfo(namespace, name, pullRequestId)));
      appendMergeStrategyLinks(linksBuilder, repository, pullRequest);
    }
    if ((PermissionCheck.mayRejectPullRequest(pullRequest) || PermissionCheck.mayMerge(repository)) && pullRequest.isInProgress()) {
      linksBuilder.single(link("reject", pullRequestResourceLinks.pullRequest()
        .reject(namespace, name, pullRequestId)));
      linksBuilder.single(link("rejectWithMessage", pullRequestResourceLinks.pullRequest()
        .rejectWithMessage(namespace, name, pullRequestId)));
    }

    Optional<Branch> sourceBranch = branchResolver.find(repository, pullRequest.getSource());
    Optional<Branch> targetBranch = branchResolver.find(repository, pullRequest.getTarget());
    boolean isPrCreationValid = false;
    if (sourceBranch.isPresent() && targetBranch.isPresent()) {
      isPrCreationValid = PR_VALID.equals(pullRequestService.checkIfPullRequestIsValid(repository, pullRequest.getSource(), pullRequest.getTarget()));
    }

    if (PermissionCheck.mayCreate(repository) && pullRequest.isRejected() && sourceBranch.isPresent() && targetBranch.isPresent() && isPrCreationValid) {
      linksBuilder.single(link("reopen", pullRequestResourceLinks.pullRequest()
        .reopen(namespace, name, pullRequestId)));
    }

    linksBuilder.single(link("reviewMark", pullRequestResourceLinks.pullRequest().reviewMark(namespace, name, pullRequestId)));

    if ((PermissionCheck.mayMerge(repository) || pullRequest.getAuthor().equals(SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString())) && pullRequest.getStatus() == PullRequestStatus.DRAFT) {
      linksBuilder.single(link("convertToPR", pullRequestResourceLinks.pullRequest().convertToPR(namespace, name, pullRequestId)));
    }
    linksBuilder.single(link("sourceBranch", branchLinkProvider.get(repository.getNamespaceAndName(), pullRequest.getSource())));
    linksBuilder.single(link("targetBranch", branchLinkProvider.get(repository.getNamespaceAndName(), pullRequest.getTarget())));
    if (pullRequest.isInProgress()) {
      linksBuilder.single(link("workflowResult", pullRequestResourceLinks.workflowEngineLinks().results(namespace, name, pullRequest.getId())));
    }
    linksBuilder.single(link("mergeConflicts", pullRequestResourceLinks.mergeLinks().conflicts(namespace, name, pullRequest.getId())));
    linksBuilder.single(link("mergeCheck", pullRequestResourceLinks.mergeLinks().check(namespace, name, pullRequest.getId())));
    Embedded.Builder embeddedBuilder = embeddedBuilder();
    embedDefaultConfig(repository, embeddedBuilder);
    embeddedBuilder.with("availableLabels", new LabelsDto(configService.evaluateConfig(repository).getLabels()));
    applyEnrichers(new EdisonHalAppender(linksBuilder, embeddedBuilder), pullRequest, repository);
    return new PullRequestDto(linksBuilder.build(), embeddedBuilder.build());
  }

  private void embedDefaultConfig(Repository repository, Embedded.Builder embeddedBuilder) {
    BasePullRequestConfig basePullRequestConfig = configService.evaluateConfig(repository);
    embeddedBuilder.with("defaultConfig", new DefaultConfigDto(basePullRequestConfig.getDefaultMergeStrategy().name(), basePullRequestConfig.isDeleteBranchOnMerge()));
  }

  private void appendMergeStrategyLinks(Links.Builder linksBuilder, Repository repository, PullRequest pullRequest) {
    try (RepositoryService service = serviceFactory.create(repository)) {
      if (service.isSupported(Command.MERGE)) {
        List<Link> mergeStrategyLinks = service.getMergeCommand().getSupportedMergeStrategies()
          .stream()
          .map(strategy -> createMergeStrategyLink(repository.getNamespaceAndName(), pullRequest, strategy))
          .toList();
        linksBuilder.array(mergeStrategyLinks);
        if (PermissionCheck.mayPerformEmergencyMerge(repository)) {
          List<Link> emergencyMergeStrategyLinks = service.getMergeCommand().getSupportedMergeStrategies()
            .stream()
            .map(strategy -> createEmergencyMergeStrategyLink(repository.getNamespaceAndName(), pullRequest, strategy))
            .toList();
          linksBuilder.array(emergencyMergeStrategyLinks);
        }
      }
    }
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName(), user.getMail());
  }

  private ReviewerDto createReviewerDto(DisplayUser user, Boolean approved) {
    return new ReviewerDto(user.getId(), user.getDisplayName(), user.getMail(), approved);
  }

  private Link createMergeStrategyLink(NamespaceAndName namespaceAndName, PullRequest pullRequest, MergeStrategy strategy) {
    return Link.linkBuilder("merge", pullRequestResourceLinks.mergeLinks().merge(
        namespaceAndName.getNamespace(),
        namespaceAndName.getName(),
        pullRequest.getId(),
        strategy
      )
    ).withName(strategy.name()).build();
  }

  private Link createEmergencyMergeStrategyLink(NamespaceAndName namespaceAndName, PullRequest pullRequest, MergeStrategy strategy) {
    return Link.linkBuilder("emergencyMerge", pullRequestResourceLinks.mergeLinks().emergencyMerge(
        namespaceAndName.getNamespace(),
        namespaceAndName.getName(),
        pullRequest.getId(),
        strategy
      )
    ).withName(strategy.name()).build();
  }

  @AllArgsConstructor
  @Getter
  @SuppressWarnings("java:S2160") // we do not need equals or hashcode
  static class DefaultConfigDto extends HalRepresentation {
    private String mergeStrategy;
    private boolean deleteBranchOnMerge;
  }
}
