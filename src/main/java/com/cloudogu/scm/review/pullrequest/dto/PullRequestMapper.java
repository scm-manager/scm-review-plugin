package com.cloudogu.scm.review.pullrequest.dto;

import com.cloudogu.scm.review.comment.api.CommentRootResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestResource;
import com.cloudogu.scm.review.pullrequest.api.PullRequestRootResource;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class PullRequestMapper extends BaseMapper<PullRequest, PullRequestDto> {

  private URI baseUri;

  @Mapping(target = "attributes", ignore = true) // We do not map HAL attributes
  public abstract PullRequestDto map(PullRequest pullRequest, @Context Repository repository);

  public abstract PullRequest map(PullRequestDto pullRequestDto);

  @AfterMapping
  void appendLinks(@MappingTarget PullRequestDto target, @Context Repository repository) {
    Links.Builder linksBuilder = linkingTo().self(getSelfLink(target, repository));
    if (RepositoryPermissions.push(repository).isPermitted()) {
      linksBuilder.single(link("createComment", getCreateCommentLink(target, repository)));
      linksBuilder.single(link("comments", getCommentsLink(target, repository)));
    }
    target.add(linksBuilder.build());
  }

  private String getCommentsLink(PullRequestDto target, Repository repository) {
    return new LinkBuilder(() -> baseUri, PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class)
      .method("getPullRequestResource").parameters()
      .method("comments").parameters()
      .method("getAll").parameters(repository.getNamespace(), repository.getName(), target.getId())
      .href();
  }

  private String getSelfLink(@MappingTarget PullRequestDto target, @Context Repository repository) {
    return new LinkBuilder(() -> baseUri, PullRequestRootResource.class, PullRequestResource.class)
      .method("getPullRequestResource").parameters()
      .method("get").parameters(repository.getNamespace(), repository.getName(), target.getId())
      .href();
  }

  private String getCreateCommentLink(@MappingTarget PullRequestDto target, @Context Repository repository) {
    return new LinkBuilder(() -> baseUri, PullRequestRootResource.class, PullRequestResource.class, CommentRootResource.class)
      .method("getPullRequestResource").parameters()
      .method("comments").parameters()
      .method("create").parameters(repository.getNamespace(), repository.getName(), target.getId())
      .href();
  }

  public PullRequestMapper using(UriInfo uriInfo) {
    this.baseUri = uriInfo.getBaseUri();
    return this;
  }
}
