package com.cloudogu.scm.review.pullrequest.service;

import com.cloudogu.scm.review.BranchResolver;
import com.cloudogu.scm.review.RepositoryResolver;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.event.ScmEventBus;
import sonia.scm.repository.Repository;
import sonia.scm.user.User;

import java.time.Instant;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPullRequestServiceTest {

  private static final Repository REPOSITORY = new Repository("1", "git", "space", "X");
  private static final User LOGGED_IN_USER = new User("user", "display", "user@example.com");

  @Mock
  RepositoryResolver repositoryResolver;
  @Mock
  BranchResolver branchResolver;
  @Mock
  PullRequestStoreFactory storeFactory;
  @Mock
  PullRequestStore store;
  @Mock
  ScmEventBus eventBus;
  @Mock
  Subject subject;

  @Captor
  ArgumentCaptor<Object> eventCaptor;

  @InjectMocks
  DefaultPullRequestService service;

  @BeforeEach
  void initStore() {
    when(storeFactory.create(REPOSITORY)).thenReturn(store);
  }

  @BeforeEach
  void initEventBus() {
    doNothing().when(eventBus).post(eventCaptor.capture());
  }

  @Nested
  class ForNewPullRequests {

    @BeforeEach
    void setUser() {
      ThreadContext.bind(subject);
      PrincipalCollection principalCollection = mock(PrincipalCollection.class);
      when(subject.getPrincipals()).thenReturn(principalCollection);
      when(principalCollection.oneByType(any())).thenReturn(LOGGED_IN_USER);
    }

    @BeforeEach
    void initAddWithNewId() {
      when(store.add(any())).thenReturn("new_id");
    }

    @Test
    void shouldStoreNewPullRequest() {
      PullRequest pullRequest = createPullRequest(null, null, null);

      String newId = service.add(REPOSITORY, pullRequest);

      assertThat(newId).isEqualTo("new_id");
      verify(store).add(pullRequest);
    }

    @Test
    void shouldSetCreationDate() {
      PullRequest pullRequest = createPullRequest(null, null, null);

      service.add(REPOSITORY, pullRequest);

      assertThat(pullRequest.getCreationDate()).isNotNull();
      assertThat(pullRequest.getLastModified()).isNull();
    }

    @Test
    void shouldStoreAuthorAsSubscriber() {
      PullRequest pullRequest = createPullRequest(null, null, null);

      service.add(REPOSITORY, pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(1)
        .allMatch(r -> r.getName().equals(LOGGED_IN_USER.getId()));
    }

    @Test
    void shouldStoreReviewerAsSubscriber() {
      PullRequest pullRequest = createPullRequest(null, null, null);
      pullRequest.setReviewer(singleton(new Recipient("reviewer", "reviewer@example.com")));

      service.add(REPOSITORY, pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(2)
        .anyMatch(r -> r.getName().equals("reviewer"));
    }

    @Test
    void shouldSendEvent() {
      PullRequest pullRequest = createPullRequest(null, null, null);

      service.add(REPOSITORY, pullRequest);

      assertThat(eventCaptor.getValue())
        .isInstanceOf(PullRequestEvent.class)
        .extracting("item", "oldItem", "eventType")
        .containsExactly(pullRequest, null, HandlerEventType.CREATE);
    }
  }

  @Nested
  class ForChangedPullRequests {

    PullRequest oldPullRequest;

    @BeforeEach
    void mockOldPullRequest() {
      oldPullRequest = createPullRequest("changed", Instant.ofEpochSecond(1_000_000), null);
      oldPullRequest.setSubscriber(singleton(new Recipient("subscriber", "reviewer@example.com")));
      oldPullRequest.setReviewer(singleton(new Recipient("reviewer", "reviewer@example.com")));
      when(store.get("changed")).thenReturn(oldPullRequest);
    }

    @Test
    void shouldStoreChangedPullRequest() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      verify(store).update(pullRequest);
    }

    @Test
    void shouldSetLastModifiedDate() {
      PullRequest pullRequest = createPullRequest("changed", Instant.ofEpochSecond(0), Instant.ofEpochMilli(0));

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(pullRequest.getCreationDate().getEpochSecond()).isEqualTo(1_000_000);
      assertThat(pullRequest.getLastModified()).isNotNull();
      assertThat(pullRequest.getLastModified().getEpochSecond()).isNotEqualTo(0);
    }

    @Test
    void shouldStoreSubscriberFromOldObject() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(1)
        .allMatch(r -> r.getName().equals("subscriber"));

    }

    @Test
    void shouldAddNewReviewerAsSubscriber() {
      PullRequest pullRequest = createPullRequest("changed", null, null);
      pullRequest.setReviewer(singleton(new Recipient("new_reviewer", "reviewer@example.com")));

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(pullRequest.getSubscriber())
        .hasSize(2)
        .anyMatch(r -> r.getName().equals("new_reviewer"));
    }

    @Test
    void shouldSendEvent() {
      PullRequest pullRequest = createPullRequest("changed", null, null);

      service.update(REPOSITORY, "changed", pullRequest);

      assertThat(eventCaptor.getValue())
        .isInstanceOf(PullRequestEvent.class)
        .extracting("item", "oldItem", "eventType")
        .containsExactly(pullRequest, oldPullRequest, HandlerEventType.MODIFY);
    }
  }

  private PullRequest createPullRequest(String id, Instant creationDate, Instant lastModified) {
    return new PullRequest(id, "source", "target", "pr", "description", null, creationDate, lastModified, null, emptySet(), emptySet());
  }
}
