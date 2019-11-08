package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.PullRequestResourceLinks;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.google.common.base.Strings;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.stream.Collectors.toList;

@Mapper
public abstract class PullRequestMapper extends BaseMapper<PullRequest, PullRequestDto> {

  @Inject
  private UserDisplayManager userDisplayManager;

  @Inject
  private RepositoryServiceFactory serviceFactory;
  private PullRequestResourceLinks pullRequestResourceLinks = new PullRequestResourceLinks(() -> URI.create("/"));
  @Inject
  private BranchRevisionResolver branchRevisionResolver;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  @Mapping(target = "reviewer", source = "reviewer", qualifiedByName = "mapReviewer")
  @Mapping(target = "author", source = "author", qualifiedByName = "mapAuthor")
  public abstract PullRequestDto map(PullRequest pullRequest, @Context Repository repository);

  @Mapping(target = "reviewer", source = "reviewer", qualifiedByName = "mapReviewerFromDto")
  public abstract PullRequest map(PullRequestDto dto);

  @Named("mapReviewerFromDto")
  Set<String> mapReviewerFromDto(Set<DisplayedUserDto> reviewer) {
    if (reviewer.isEmpty()) {
      return Collections.emptySet();
    }
    return reviewer
      .stream()
      .map(DisplayedUserDto::getId)
      .map(userDisplayManager::get)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(DisplayUser::getId)
      .collect(Collectors.toSet());
  }

  @Named("mapReviewer")
  Set<DisplayedUserDto> mapReviewer(Set<String> reviewer) {
    return reviewer
      .stream()
      .map(userDisplayManager::get)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(this::createDisplayedUserDto)
      .collect(Collectors.toSet());
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
  protected void appendLinks(@MappingTarget PullRequestDto target, PullRequest pullRequest, @Context Repository repository) {

    Links.Builder linksBuilder = linkingTo().self(pullRequestResourceLinks.pullRequest().self(repository.getNamespace(), repository.getName(), target.getId()));
    linksBuilder.single(link("comments", pullRequestResourceLinks.pullRequestComments().all(repository.getNamespace(), repository.getName(), target.getId())));
    if (CurrentUserResolver.getCurrentUser() != null && !Strings.isNullOrEmpty(CurrentUserResolver.getCurrentUser().getMail())) {
      linksBuilder.single(link("subscription", pullRequestResourceLinks.pullRequest().subscription(repository.getNamespace(), repository.getName(), target.getId())));
    }
    if (PermissionCheck.mayModifyPullRequest(repository, pullRequest)) {
      linksBuilder.single(link("update", pullRequestResourceLinks.pullRequest().update(repository.getNamespace(), repository.getName(), target.getId())));
    }
    if (PermissionCheck.mayMerge(repository) && target.getStatus() == PullRequestStatus.OPEN) {
      linksBuilder.single(link("reject", pullRequestResourceLinks.pullRequest().reject(repository.getNamespace(), repository.getName(), target.getId())));

      if(RepositoryPermissions.push(repository).isPermitted()) {
        linksBuilder.single(link("mergeDryRun", pullRequestResourceLinks.mergeLinks().dryRun(repository.getNamespace(), repository.getName())));
        appendMergeStrategyLinks(linksBuilder, repository);
      }

    }
    target.add(linksBuilder.build());
  }

  private void appendMergeStrategyLinks(Links.Builder linksBuilder, @Context Repository repository) {
    try (RepositoryService service = serviceFactory.create(repository)) {
      if (service.isSupported(Command.MERGE)) {
        List<Link> strategyLinks = Stream.of(service.getMergeCommand().getSupportedMergeStrategies())
          .flatMap(Set::stream)
          .map(strategy -> createStrategyLink(repository.getNamespaceAndName(), strategy))
          .collect(toList());
        linksBuilder.array(strategyLinks);
      }
    }
  }

  private DisplayedUserDto createDisplayedUserDto(DisplayUser user) {
    return new DisplayedUserDto(user.getId(), user.getDisplayName(), user.getMail());
  }

  private Link createStrategyLink(NamespaceAndName namespaceAndName, MergeStrategy strategy) {
    return Link.linkBuilder("merge", pullRequestResourceLinks.mergeLinks().merge(
        namespaceAndName.getNamespace(),
        namespaceAndName.getName(),
        strategy
      )
    ).withName(strategy.name()).build();
  }
}
