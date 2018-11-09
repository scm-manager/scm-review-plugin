package com.cloudogu.scm.review;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import sonia.scm.store.DataStore;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestStoreTest {

  @Mock
  private DataStore<PullRequest> dataStore;

  @InjectMocks
  private PullRequestStore store;

  @BeforeEach
  public void setUpStore() {
    Map<String, PullRequest> backingMap = Maps.newHashMap();
    // delegate store methods to backing map
    when(dataStore.getAll()).thenReturn(backingMap);
    doAnswer(invocationOnMock -> {
      String id = invocationOnMock.getArgument(0);
      PullRequest pr = invocationOnMock.getArgument(1);
      backingMap.put(id, pr);
      return null;
    }).when(dataStore).put(anyString(), any(PullRequest.class));
  }

  @Test
  public void testAdd() {
    assertThat(store.add(createPullRequest())).isEqualTo("1");
    assertThat(store.add(createPullRequest())).isEqualTo("2");
    assertThat(store.add(createPullRequest())).isEqualTo("3");
  }

  private PullRequest createPullRequest() {
    return PullRequest.builder()
      .title("Awesome PR")
      .description("Hitchhiker's guide to the galaxy")
      .source("develop")
      .target("master")
      .author("dent")
      .creationDate(Instant.now())
      .build();
  }

}
