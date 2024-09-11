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

package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentEvent;
import com.cloudogu.scm.review.comment.service.CommentType;
import com.cloudogu.scm.review.comment.service.Reply;
import com.cloudogu.scm.review.comment.service.ReplyEvent;
import com.github.legman.Subscribe;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.shiro.SecurityUtils;
import sonia.scm.HandlerEventType;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.user.User;
import sonia.scm.user.UserManager;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class PullRequestChangeService {

  private static final String SOURCE_BRANCH = "SOURCE_BRANCH";
  private static final String TARGET_BRANCH = "TARGET_BRANCH";
  private static final String TITLE = "TITLE";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String SUBSCRIBER = "SUBSCRIBER";
  private static final String LABELS = "LABELS";
  private static final String REVIEW_MARKS = "REVIEW_MARKS";
  private static final String DELETE_SOURCE_BRANCH_AFTER_MERGE = "DELETE_SOURCE_BRANCH_AFTER_MERGE";
  private static final String REVIEWER = "REVIEWER";
  private static final String COMMENT = "COMMENT";
  private static final String REPLY = "REPLY";
  private static final String TASK = "TASK";
  private static final String COMMENT_TYPE = "COMMENT_TYPE";
  private static final String PR_STATUS = "PR_STATUS";
  private static final String SOURCE_BRANCH_REVISION = "SOURCE_BRANCH_REVISION";

  private static final String CURRENT_USERNAME = "currentUsername";
  private static final String CURRENT_APPROVED = "currentApproved";
  private static final String PREVIOUS_USERNAME = "previousUsername";
  private static final String PREVIOUS_APPROVED = "previousApproved";

  private static final String COMMENT_VALUE = "comment";

  private static final String FILE = "file";
  private static final String USER = "user";

  private static final String REJECTION_CAUSE = "rejectionCause";
  private static final String REJECTION_MESSAGE = "rejectionMessage";

  private static final String storeName = "pullRequestChanges";

  private final RepositoryResolver repositoryResolver;
  private final DataStoreFactory dataStoreFactory;
  private final UserManager userManager;
  private final Clock clock;
  private final PullRequestService pullRequestService;

  @Inject
  public PullRequestChangeService(RepositoryResolver repositoryResolver, DataStoreFactory dataStoreFactory, UserManager userManager, PullRequestService pullRequestService) {
    this(repositoryResolver, dataStoreFactory, userManager, Clock.systemDefaultZone(), pullRequestService);
  }

  public PullRequestChangeService(RepositoryResolver repositoryResolver, DataStoreFactory dataStoreFactory, UserManager userManager, Clock clock, PullRequestService pullRequestService) {
    this.repositoryResolver = repositoryResolver;
    this.dataStoreFactory = dataStoreFactory;
    this.userManager = userManager;
    this.clock = clock;
    this.pullRequestService = pullRequestService;
  }


  public List<PullRequestChange> getAllChangesOfPullRequest(NamespaceAndName namespaceAndName, String pullRequestId) {
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    DataStore<PullRequestChangeContainer> store = dataStoreFactory.withType(PullRequestChangeContainer.class)
      .withName(storeName)
      .forRepository(repository)
      .build();

    return store.getOptional(pullRequestId).orElseGet(PullRequestChangeContainer::new).allChanges;
  }

  public void addPullRequestChange(NamespaceAndName namespaceAndName, PullRequestChange change) {
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    DataStore<PullRequestChangeContainer> store = dataStoreFactory.withType(PullRequestChangeContainer.class)
      .withName(storeName)
      .forRepository(repository)
      .build();

    PullRequestChangeContainer changes = store.getOptional(change.getPrId()).orElseGet(PullRequestChangeContainer::new);
    changes.allChanges.add(change);
    store.put(change.getPrId(), changes);
  }

  public void addAllPullRequestChanges(NamespaceAndName namespaceAndName, String pullRequestId, Collection<PullRequestChange> changes) {
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    DataStore<PullRequestChangeContainer> store = dataStoreFactory.withType(PullRequestChangeContainer.class)
      .withName(storeName)
      .forRepository(repository)
      .build();

    PullRequestChangeContainer currentChanges = store.getOptional(pullRequestId).orElseGet(PullRequestChangeContainer::new);
    currentChanges.allChanges.addAll(changes);
    store.put(pullRequestId, currentChanges);
  }

  @Subscribe(async = false)
  public void onPullRequestModified(PullRequestEvent event) {
    if (event.getEventType() != HandlerEventType.MODIFY) {
      return;
    }

    NamespaceAndName namespaceAndName = event.getRepository().getNamespaceAndName();
    User user = getUser();
    PullRequest oldPr = event.getOldItem();
    PullRequest updatedPr = event.getPullRequest();
    String pullRequestId = event.getPullRequest().getId();
    List<PullRequestChange> changes = new ArrayList<>();

    computeValueEdit(
      SOURCE_BRANCH, oldPr.getSource(), updatedPr.getSource(), user, pullRequestId, changes, value -> null
    );

    computeValueEdit(
      TARGET_BRANCH, oldPr.getTarget(), updatedPr.getTarget(), user, pullRequestId, changes, value -> null
    );

    computeValueEdit(
      DELETE_SOURCE_BRANCH_AFTER_MERGE, oldPr.isShouldDeleteSourceBranch(), updatedPr.isShouldDeleteSourceBranch(), user, pullRequestId, changes, value -> null
    );

    computeValueEdit(
      DESCRIPTION, oldPr.getDescription(), updatedPr.getDescription(), user, pullRequestId, changes, value -> null
    );

    computeValueEdit(
      PR_STATUS, oldPr.getStatus(), updatedPr.getStatus(), user, pullRequestId, changes, value -> null
    );

    computeValueEdit(
      TITLE, oldPr.getTitle(), updatedPr.getTitle(), user, pullRequestId, changes, value -> null
    );

    computeChangesInSet(
      LABELS, oldPr.getLabels(), updatedPr.getLabels(), user, pullRequestId, changes, value -> null
    );

    computeChangesInReviewers(oldPr.getReviewer(), updatedPr.getReviewer(), namespaceAndName, user, pullRequestId);

    addAllPullRequestChanges(namespaceAndName, pullRequestId, changes);
  }

  private User getUser() {
    Object principal = SecurityUtils.getSubject().getPrincipal();

    if (principal == null) {
      return new User();
    }

    User user = userManager.get(principal.toString());
    return user == null ? new User() : user;
  }

  private void computeValueEdit(String property, Object oldValue, Object newValue, User user, String pullRequestId, List<PullRequestChange> changes, BuildAdditionalInfo builder) {
    if (!oldValue.equals(newValue)) {
      changes.add(new PullRequestChange(
        pullRequestId,
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        oldValue.toString(),
        newValue.toString(),
        property,
        builder.build(newValue)
      ));
    }
  }

  private void computeChangesInSet(String property, Set<?> oldValue, Set<?> newValue, User user, String pullRequestId, List<PullRequestChange> changes, BuildAdditionalInfo builder) {
    Set<?> added = computeSetDiff(newValue, oldValue);
    added.forEach(value -> changes.add(
      new PullRequestChange(
        pullRequestId,
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        null,
        value.toString(),
        property,
        builder.build(value)
      )));

    Set<?> removed = computeSetDiff(oldValue, newValue);
    removed.forEach(value -> changes.add(
      new PullRequestChange(
        pullRequestId,
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        value.toString(),
        null,
        property,
        builder.build(value)
      )));
  }

  private Set<?> computeSetDiff(Set<?> a, Set<?> b) {
    return a.stream().filter(value -> !b.contains(value)).collect(Collectors.toSet());
  }

  private void computeChangesInReviewers(Map<String, Boolean> oldValue, Map<String, Boolean> newValue, NamespaceAndName namespaceAndName, User user, String pullRequestId) {
    Map<String, Boolean> addedReviewer = computeAddedMapEntries(newValue, oldValue);
    addedReviewer.forEach((reviewer, approved) -> addPullRequestChange(
      namespaceAndName,
      new PullRequestChange(
        pullRequestId,
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        null,
        new Reviewer(reviewer, approved).toString(),
        REVIEWER,
        Map.of(CURRENT_USERNAME, reviewer, CURRENT_APPROVED, approved.toString())
      )
    ));

    Map<String, Boolean> removedReviewer = computeAddedMapEntries(oldValue, newValue);
    removedReviewer.forEach((reviewer, approved) -> addPullRequestChange(
      namespaceAndName,
      new PullRequestChange(
        pullRequestId,
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        new Reviewer(reviewer, approved).toString(),
        null,
        REVIEWER,
        Map.of(PREVIOUS_USERNAME, reviewer, PREVIOUS_APPROVED, approved.toString())
      )
    ));

    newValue.forEach((reviewer, newApproval) -> {
      Boolean oldApproval = oldValue.get(reviewer);
      if (oldValue.containsKey(reviewer) && oldApproval != newApproval) {
        addPullRequestChange(
          namespaceAndName,
          new PullRequestChange(
            pullRequestId,
            user.getId(),
            user.getDisplayName(),
            user.getMail(),
            Instant.now(clock),
            new Reviewer(reviewer, oldApproval).toString(),
            new Reviewer(reviewer, newApproval).toString(),
            REVIEWER,
            Map.of(
              PREVIOUS_USERNAME, reviewer, PREVIOUS_APPROVED, oldApproval.toString(),
              CURRENT_USERNAME, reviewer, CURRENT_APPROVED, newApproval.toString()
            )
          )
        );
      }
    });
  }

  private Map<String, Boolean> computeAddedMapEntries(Map<String, Boolean> a, Map<String, Boolean> b) {
    return a.entrySet().stream()
      .filter(entry -> !b.containsKey(entry.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Subscribe(async = false)
  public void onCommentEvent(CommentEvent event) {
    if ((event.getItem() != null && event.getItem().isSystemComment()) ||
      (event.getOldItem() != null && event.getOldItem().isSystemComment())) {
      return;
    }

    if (event.getEventType() == HandlerEventType.CREATE) {
      User user = getUser();

      addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
        event.getPullRequest().getId(),
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        null,
        event.getItem().getComment(),
        event.getItem().getType() == CommentType.COMMENT ? COMMENT : TASK,
        null
      ));
    }

    if (event.getEventType() == HandlerEventType.DELETE) {
      User user = getUser();

      addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
        event.getPullRequest().getId(),
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        event.getOldItem().getComment(),
        null,
        event.getOldItem().getType() == CommentType.COMMENT ? COMMENT : TASK,
        null
      ));
    }

    if (event.getEventType() == HandlerEventType.MODIFY) {
      List<PullRequestChange> changes = new ArrayList<>(2);
      Comment oldComment = event.getOldItem();
      Comment updatedComment = event.getItem();
      User user = getUser();
      String pullRequestId = event.getPullRequest().getId();

      computeValueEdit(
        event.getOldItem().getType() == CommentType.COMMENT ? COMMENT : TASK,
        oldComment.getComment(),
        updatedComment.getComment(),
        user,
        pullRequestId,
        changes,
        value -> null
      );

      computeValueEdit(
        COMMENT_TYPE,
        oldComment.getType(),
        updatedComment.getType(),
        user,
        pullRequestId,
        changes,
        value -> Map.of(
          COMMENT_VALUE, event.getItem().getComment())
      );

      addAllPullRequestChanges(event.repository.getNamespaceAndName(), pullRequestId, changes);
    }
  }

  @Subscribe(async = false)
  public void onReplyEvent(ReplyEvent event) {
    if (event.getEventType() == HandlerEventType.CREATE) {
      User user = getUser();

      addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
        event.getPullRequest().getId(),
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        null,
        event.getItem().getComment(),
        REPLY,
        null
      ));
    }

    if (event.getEventType() == HandlerEventType.DELETE) {
      User user = getUser();

      addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
        event.getPullRequest().getId(),
        user.getId(),
        user.getDisplayName(),
        user.getMail(),
        Instant.now(clock),
        event.getOldItem().getComment(),
        null,
        REPLY,
        null
      ));
    }

    if (event.getEventType() == HandlerEventType.MODIFY) {
      Reply oldComment = event.getOldItem();
      Reply updatedComment = event.getItem();
      User user = getUser();

      if (!oldComment.getComment().equals(updatedComment.getComment())) {
        addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
          event.getPullRequest().getId(),
          user.getId(),
          user.getDisplayName(),
          user.getMail(),
          Instant.now(clock),
          oldComment.getComment(),
          updatedComment.getComment(),
          REPLY,
          null
        ));
      }
    }
  }

  @Subscribe(async = false)
  public void onSubscribed(PullRequestSubscribedEvent event) {
    String removedSubscriber = event.getType() == PullRequestSubscribedEvent.EventType.UNSUBSCRIBED ? event.getSubscriber().getId() : null;
    String addedSubscriber = event.getType() == PullRequestSubscribedEvent.EventType.SUBSCRIBED ? event.getSubscriber().getId() : null;
    User user = getUser();

    addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
      event.getPullRequest().getId(),
      user.getId(),
      user.getDisplayName(),
      user.getMail(),
      Instant.now(clock),
      removedSubscriber,
      addedSubscriber,
      SUBSCRIBER,
      null
    ));
  }

  @Subscribe(async = false)
  public void onReviewMark(PullRequestReviewMarkEvent event) {
    ReviewMark removedReviewMark = event.getType() == PullRequestReviewMarkEvent.EventType.REMOVED ?
      event.getReviewMark() : null;

    ReviewMark addedReviewMark = event.getType() == PullRequestReviewMarkEvent.EventType.ADDED ?
      event.getReviewMark() : null;

    User user = getUser();

    addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
      event.getPullRequest().getId(),
      user.getId(),
      user.getDisplayName(),
      user.getMail(),
      Instant.now(clock),
      removedReviewMark != null ? removedReviewMark.toString() : null,
      addedReviewMark != null ? addedReviewMark.toString() : null,
      REVIEW_MARKS,
      Map.of(FILE, event.getReviewMark().getFile(), USER, event.getReviewMark().getUser())
    ));
  }

  @Subscribe(async = false)
  public void onApproval(PullRequestApprovalEvent event) {
    if (event.getApprover() == null) {
      return;
    }

    Reviewer previousApproval = event.isNewApprover() ?
      null :
      new Reviewer(
        event.getApprover().getId(), event.getCause() != PullRequestApprovalEvent.ApprovalCause.APPROVED
      );

    Reviewer currentApproval = new Reviewer(
      event.getApprover().getId(), event.getCause() == PullRequestApprovalEvent.ApprovalCause.APPROVED
    );

    Map<String, String> additionalInfo = new HashMap<>(4);
    additionalInfo.put(CURRENT_USERNAME, currentApproval.username);
    additionalInfo.put(CURRENT_APPROVED, currentApproval.approved.toString());

    if (previousApproval != null) {
      additionalInfo.put(PREVIOUS_USERNAME, previousApproval.username);
      additionalInfo.put(PREVIOUS_APPROVED, previousApproval.approved.toString());
    }

    User user = getUser();

    addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
      event.getPullRequest().getId(),
      user.getId(),
      user.getDisplayName(),
      user.getMail(),
      Instant.now(clock),
      previousApproval != null ? previousApproval.toString() : null,
      currentApproval.toString(),
      REVIEWER,
      additionalInfo
    ));
  }

  @Subscribe(async = false)
  public void onPullRequestRejected(PullRequestRejectedEvent event) {
    User user = getUser();
    Map<String, String> additionalInfo = new HashMap<>(2);
    additionalInfo.put(REJECTION_CAUSE, event.getCause().name());

    if (!Strings.isNullOrEmpty(event.getMessage())) {
      additionalInfo.put(REJECTION_MESSAGE, event.getMessage());
    }

    addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
      event.getPullRequest().getId(),
      user.getId(),
      user.getDisplayName(),
      user.getMail(),
      Instant.now(clock),
      event.getPreviousStatus() != null ? event.getPreviousStatus().name() : null,
      PullRequestStatus.REJECTED.name(),
      PR_STATUS,
      additionalInfo
    ));
  }

  @Subscribe(async = false)
  public void onPullRequestReopened(PullRequestReopenedEvent event) {
    User user = getUser();

    addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
      event.getPullRequest().getId(),
      user.getId(),
      user.getDisplayName(),
      user.getMail(),
      Instant.now(clock),
      PullRequestStatus.REJECTED.name(),
      PullRequestStatus.OPEN.name(),
      PR_STATUS,
      null
    ));
  }

  @Subscribe
  public void onPostPush(PostReceiveRepositoryHookEvent event) {
    if (!pullRequestService.supportsPullRequests(event.getRepository())) {
      return;
    }

    User user = getUser();
    List<String> branches = event.getContext().getBranchProvider().getCreatedOrModified();
    List<PullRequest> repoPrs = pullRequestService.getAll(event.getRepository().getNamespace(), event.getRepository().getName());

    for (String branch : branches) {
      List<PullRequest> affectedPrs = repoPrs.stream().filter(pr -> branch.equals(pr.getSource())).toList();

      if (affectedPrs.isEmpty()) {
        continue;
      }

      Modifications modifications = event.getContext().getModificationsProvider().getModifications(branch);
      affectedPrs.forEach(pr ->
        addPullRequestChange(event.getRepository().getNamespaceAndName(), new PullRequestChange(
          pr.getId(),
          user.getId(),
          user.getDisplayName(),
          user.getMail(),
          Instant.now(clock),
          modifications.getBaseRevision().orElse(null),
          modifications.getRevision(),
          SOURCE_BRANCH_REVISION,
          null
        ))
      );
    }
  }

  @FunctionalInterface
  private interface BuildAdditionalInfo {
    Map<String, String> build(Object value);
  }

  @Getter
  @XmlRootElement(name = "pull-request-change-container")
  @XmlAccessorType(XmlAccessType.FIELD)
  static class PullRequestChangeContainer {
    private final List<PullRequestChange> allChanges = new ArrayList<>();
  }

  @ToString
  @AllArgsConstructor
  private static class Reviewer {
    private String username;
    private Boolean approved;
  }
}
