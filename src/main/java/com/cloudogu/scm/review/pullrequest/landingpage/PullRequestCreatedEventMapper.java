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

package com.cloudogu.scm.review.pullrequest.landingpage;

import com.cloudogu.scm.landingpage.myevents.MyEvent;
import com.cloudogu.scm.review.CurrentUserResolver;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.cloudogu.scm.review.pullrequest.service.PullRequestEvent;
import com.cloudogu.scm.review.pullrequest.service.PullRequestStatus;
import com.github.legman.Subscribe;
import lombok.Getter;
import sonia.scm.EagerSingleton;
import sonia.scm.HandlerEventType;
import sonia.scm.event.Event;
import sonia.scm.event.ScmEventBus;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import jakarta.inject.Inject;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.Optional;

import static com.cloudogu.scm.review.PermissionCheck.READ_PULL_REQUEST;

@Extension
@EagerSingleton
@Requires("scm-landingpage-plugin")
public class PullRequestCreatedEventMapper {

  private final ScmEventBus eventBus;
  private final UserDisplayManager userDisplayManager;

  @Inject
  public PullRequestCreatedEventMapper(ScmEventBus eventBus, UserDisplayManager userDisplayManager) {
    this.eventBus = eventBus;
    this.userDisplayManager = userDisplayManager;
  }

  @Subscribe
  public void mapToLandingpageEvent(PullRequestEvent pullRequestEvent) {
    if (pullRequestEvent.getEventType() == HandlerEventType.CREATE) {
      eventBus.post(new PullRequestCreatedEvent(pullRequestEvent.getRepository(), pullRequestEvent.getItem(), userDisplayManager.get(pullRequestEvent.getItem().getAuthor())));
    } else if (pullRequestEvent.getEventType() == HandlerEventType.MODIFY && pullRequestEvent.getOldItem().isDraft() && pullRequestEvent.getItem().isOpen()) {
      eventBus.post(new PullRequestDraftToOpenEvent(pullRequestEvent.getRepository(), pullRequestEvent.getItem(), userDisplayManager.get(pullRequestEvent.getItem().getAuthor())));
    }
  }

  @Subscribe
  public void mapReopenEvent(com.cloudogu.scm.review.pullrequest.service.PullRequestReopenedEvent pullRequestReopenedEvent) {
    eventBus.post(new PullRequestReopenedEvent(
      pullRequestReopenedEvent.getRepository(),
      pullRequestReopenedEvent.getPullRequest(),
      CurrentUserResolver.getCurrentUserDisplayName()
    ));
  }

  @Event
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement
  @Getter
  public static class PullRequestCreatedEvent extends MyEvent {

    private String namespace;
    private String name;
    private String id;
    private String title;
    private String source;
    private String target;
    private PullRequestStatus status;
    private String author;

    public PullRequestCreatedEvent() {
    }

    public PullRequestCreatedEvent(Repository repository, PullRequest pullRequest, Optional<DisplayUser> displayUser) {
      super("PullRequestCreatedEvent", RepositoryPermissions.custom(READ_PULL_REQUEST, repository.getId()).asShiroString(), pullRequest.getCreationDate());
      this.namespace = repository.getNamespace();
      this.name = repository.getName();
      this.id = pullRequest.getId();
      this.title = pullRequest.getTitle();
      this.source = pullRequest.getSource();
      this.target = pullRequest.getTarget();
      this.status = pullRequest.getStatus();
      this.author = displayUser.map(DisplayUser::getDisplayName).orElse(null);
    }
  }

  @Event
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement
  @Getter
  public static class PullRequestReopenedEvent extends MyEvent {

    private String namespace;
    private String name;
    private String id;
    private String title;
    private String source;
    private String target;
    private PullRequestStatus status;
    private String author;

    public PullRequestReopenedEvent() {
    }

    public PullRequestReopenedEvent(Repository repository, PullRequest pullRequest, String author) {
      super("PullRequestReopenedEvent", RepositoryPermissions.custom(READ_PULL_REQUEST, repository.getId()).asShiroString(), Instant.now());
      this.namespace = repository.getNamespace();
      this.name = repository.getName();
      this.id = pullRequest.getId();
      this.title = pullRequest.getTitle();
      this.source = pullRequest.getSource();
      this.target = pullRequest.getTarget();
      this.status = pullRequest.getStatus();
      this.author = author;
    }
  }

  @Event
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement
  @Getter
  public static class PullRequestDraftToOpenEvent extends MyEvent {

    private String namespace;
    private String name;
    private String id;
    private String title;
    private String author;

    public PullRequestDraftToOpenEvent() {
    }

    public PullRequestDraftToOpenEvent(Repository repository, PullRequest pullRequest, Optional<DisplayUser> displayUser) {
      super("PullRequestDraftToOpenEvent", RepositoryPermissions.custom(READ_PULL_REQUEST, repository.getId()).asShiroString(), pullRequest.getLastModified());
      this.namespace = repository.getNamespace();
      this.name = repository.getName();
      this.id = pullRequest.getId();
      this.title = pullRequest.getTitle();
      this.author = displayUser.map(DisplayUser::getDisplayName).orElse(null);
    }
  }
}
