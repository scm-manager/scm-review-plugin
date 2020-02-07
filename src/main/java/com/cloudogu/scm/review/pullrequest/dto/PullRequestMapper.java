package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentService;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestService;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.cloudogu.scm.review.pullrequest.service.ReviewMark;
import com.google.common.base.Strings;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.NotFoundException;
import sonia.scm.api.v2.resources.BaseMapper;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.MergeStrategy;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.cloudogu.scm.review.CurrentUserResolver.getCurrentUser;
import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.stream.Collectors.toList;

@Mapper
public abstract class PullRequestMapper extends BaseMapper<PullRequest, PullRequestDto> {

  @Inject
  private UserDisplayManager userDisplayManager;

  @Inject
  private PullRequestService pullRequestService;
  @Inject
  private CommentService commentService;
  @Inject
  private RepositoryServiceFactory serviceFactory;
  private PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(() -> URI.create("/"));

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "reviewer", source = "reviewer", qualifiedByName = "mapReviewer")
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  @Mapping(target = "markedAsReviewed", ignore = true)
  public abstract PullRequestDto map(PullRequest pullRequest, @Context Repository repository);

  @Mapping(target = "subscriber", ignore = true)
  @Mapping(target = "reviewer", source = "reviewer", qualifiedByName = "mapReviewerFromDto")
  @Mapping(target = "reviewMarks", ignore = true)
  public abstract PullRequest map(PullRequestDto dto);

  @Named("mapReviewerFromDto")
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

  @Named("mapReviewer")
  Set<ReviewerDto> mapReviewer(Map<String, Boolean> reviewer) {
    return reviewer
      .entrySet()
      .stream()
      .map(entry -> this.createReviewerDto(getUserIfAvailable(entry), entry.getValue()))
      .collect(Collectors.toSet());
  }

  private DisplayUser getUserIfAvailable(Map.Entry<String, Boolean> entry) {
    Optional<DisplayUser> getDisplayUser = userDisplayManager.get(entry.getKey());
    DisplayUser user = getDisplayUser.orElse(null);

    if (user == null) {
      throw new NotFoundException(DisplayUser.class, String.format("User %s not found", entry.getKey()));
    }
    return user;
  }

  @Named("mapAuthor")
  DisplayedUserDto mapAuthor(String authorId) {
    return userDisplayManager.get(authorId).map(this::createDisplayedUserDto).orElse(new DisplayedUserDto(authorId, authorId, null));
  }

  String mapAuthor(DisplayedUserDto author) {
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
      .collect(toList());
    target.setMarkedAsReviewed(filesMarkedAsReviewed);
  }

  private long countCommentsByFilter(List<Comment> comments, Predicate<Comment> filter) {
    return comments.stream().filter(filter).count();
  }

  @AfterMapping
  protected void appendLinks(@MappingTarget PullRequestDto target, PullRequest pullRequest, @Context Repository repository) {
    Links.Builder linksBuilder = linkingTo().self(pullRequestResourceLinks.pullRequest()
      .self(repository.getNamespace(), repository.getName(), target.getId()));
    linksBuilder.single(link("comments", pullRequestResourceLinks.pullRequestComments()
      .all(repository.getNamespace(), repository.getName(), target.getId())));
    linksBuilder.single(link("events", pullRequestResourceLinks.pullRequest()
      .events(repository.getNamespace(), repository.getName(), target.getId())));
    if (PermissionCheck.mayComment(repository) && CurrentUserResolver.getCurrentUser() != null && !Strings.isNullOrEmpty(CurrentUserResolver.getCurrentUser().getMail())) {
      if (pullRequest.getStatus() == PullRequestStatus.OPEN) {
        if (pullRequestService.hasUserApproved(repository, pullRequest.getId())) {
          linksBuilder.single(link("disapprove", pullRequestResourceLinks.pullRequest().disapprove(repository.getNamespace(), repository.getName(), target.getId())));
        } else {
          linksBuilder.single(link("approve", pullRequestResourceLinks.pullRequest().approve(repository.getNamespace(), repository.getName(), target.getId())));
        }
      }

      linksBuilder.single(link("subscription", pullRequestResourceLinks.pullRequest()
        .subscription(repository.getNamespace(), repository.getName(), target.getId())));
    }
    if (PermissionCheck.mayModifyPullRequest(repository, pullRequest)) {
      linksBuilder.single(link("update", pullRequestResourceLinks.pullRequest()
        .update(repository.getNamespace(), repository.getName(), target.getId())));
    }
    if (PermissionCheck.mayMerge(repository) && target.getStatus() == PullRequestStatus.OPEN) {
      linksBuilder.single(link("reject", pullRequestResourceLinks.pullRequest()
        .reject(repository.getNamespace(), repository.getName(), target.getId())));

      if (RepositoryPermissions.push(repository).isPermitted() && target.getStatus() == PullRequestStatus.OPEN) {
        linksBuilder.single(link("mergeCheck", pullRequestResourceLinks.mergeLinks()
          .check(repository.getNamespace(), repository.getName(), pullRequest.getId())));
        linksBuilder.single(link("mergeConflicts", pullRequestResourceLinks.mergeLinks()
          .conflicts(repository.getNamespace(), repository.getName(), pullRequest.getId())));
        linksBuilder.single(link("defaultCommitMessage", pullRequestResourceLinks.mergeLinks()
          .createDefaultCommitMessage(repository.getNamespace(), repository.getName(), pullRequest.getId())));
        appendMergeStrategyLinks(linksBuilder, repository, pullRequest);
      }
    }
    linksBuilder.single(link("markAsReviewed", pullRequestResourceLinks.pullRequest().markAsReviewed(repository.getNamespace(), repository.getName(), target.getId())));
    linksBuilder.single(link("markAsNotReviewed", pullRequestResourceLinks.pullRequest().markAsNotReviewed(repository.getNamespace(), repository.getName(), target.getId())));
    target.add(linksBuilder.build());
  }

  private void appendMergeStrategyLinks(Links.Builder linksBuilder, Repository repository, PullRequest pullRequest) {
    try (RepositoryService service = serviceFactory.create(repository)) {
      if (service.isSupported(Command.MERGE)) {
        List<Link> strategyLinks = service.getMergeCommand().getSupportedMergeStrategies()
          .stream()
          .map(strategy -> createStrategyLink(repository.getNamespaceAndName(), pullRequest, strategy))
          .collect(toList());
        linksBuilder.array(strategyLinks);
      }
    }
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName(), user.getMail());
  }

  private ReviewerDto createReviewerDto(DisplayUser user, Boolean approved) {
    return new ReviewerDto(user.getId(), user.getDisplayName(), user.getMail(), approved);
  }

  private Link createStrategyLink(NamespaceAndName namespaceAndName, PullRequest pullRequest, MergeStrategy strategy) {
    return Link.linkBuilder("merge", pullRequestResourceLinks.mergeLinks().merge(
      namespaceAndName.getNamespace(),
      namespaceAndName.getName(),
      pullRequest.getId(),
      strategy
      )
    ).withName(strategy.name()).build();
  }
}
