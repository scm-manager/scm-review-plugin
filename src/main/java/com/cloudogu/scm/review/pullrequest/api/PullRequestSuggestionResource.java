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

package com.cloudogu.scm.review.pullrequest.api;

import com.cloudogu.scm.review.pullrequest.dto.PushEntryDto;
import com.cloudogu.scm.review.pullrequest.service.PullRequestSuggestionService;
import de.otto.edison.hal.Links;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.SecurityUtils;
import sonia.scm.NotFoundException;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryManager;

import java.util.Collection;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Links.linkingTo;

@Path("v2/push-entries/{namespace}/{name}")
public class PullRequestSuggestionResource {

  private final PullRequestSuggestionService suggestionService;

  private final RepositoryManager repositoryManager;

  private final ScmPathInfoStore scmPathInfoStore;

  @Inject
  public PullRequestSuggestionResource(PullRequestSuggestionService suggestionService,
                                       RepositoryManager repositoryManager,
                                       ScmPathInfoStore scmPathInfoStore) {
    this.suggestionService = suggestionService;
    this.repositoryManager = repositoryManager;
    this.scmPathInfoStore = scmPathInfoStore;
  }

  @DELETE
  @Path("{branch}")
  public void removePushEntry(@PathParam("namespace") String namespace,
                              @PathParam("name") String name,
                              @PathParam("branch") String branch) {
    Repository repository = getRepository(new NamespaceAndName(namespace, name));
    String username = SecurityUtils.getSubject().getPrincipal().toString();
    suggestionService.removePushEntry(repository.getId(), branch, username);
  }

  private Repository getRepository(NamespaceAndName namespaceAndName) {
    Repository repository = repositoryManager.get(namespaceAndName);
    if (repository == null) {
      throw new NotFoundException(Repository.class, namespaceAndName.toString());
    }

    return repository;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("")
  public Collection<PushEntryDto> getAllPushEntries(@PathParam("namespace") String namespace,
                                                    @PathParam("name") String name) {
    Repository repository = getRepository(new NamespaceAndName(namespace, name));
    String username = SecurityUtils.getSubject().getPrincipal().toString();

    Collection<PullRequestSuggestionService.PushEntry> pushEntries = suggestionService.getPushEntries(
      repository.getId(), username
    );

    return pushEntries.stream().map(pushEntry -> mapToDto(namespace, name, pushEntry)).toList();
  }

  private PushEntryDto mapToDto(String namespace, String name, PullRequestSuggestionService.PushEntry pushEntry) {
    PushEntryDto dto = new PushEntryDto();
    dto.setBranch(pushEntry.getBranch());
    dto.setPushedAt(pushEntry.getPushedAt());

    Links.Builder builder = linkingTo().single(
      link(
        "delete", deletePushEntryLink(namespace, name, pushEntry.getBranch())
      )
    );
    dto.add(builder.build());

    return dto;
  }

  private String deletePushEntryLink(String namespace, String name, String branch) {
    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get(), PullRequestSuggestionResource.class);
    return linkBuilder.method("removePushEntry").parameters(namespace, name, branch).href();
  }
}
