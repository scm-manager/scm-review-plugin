package com.cloudogu.scm.review.events;

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelRegistryTest {

  @Mock
  private ClientFactory clientFactory;

  @InjectMocks
  private ChannelRegistry registry;

  @Test
  void shouldReturnChannel() {
    Channel channel = channel("HoG", "42");
    assertThat(channel).isNotNull();
  }

  @Test
  void shouldReturnTheSame() {
    Channel one = channel("HoG", "42");
    Channel two = channel("HoG", "42");
    assertThat(one).isSameAs(two);
  }

  @Test
  void shouldCallRemoveClosedClientsOnAllExistingChannels() {
    registry.setChannelFactory(channelId -> spy(new Channel(clientFactory, Clock.systemUTC())));
    Channel one = channel("HoG", "42");
    channel("HoG", "42");
    verify(one).removeClosedOrTimeoutClients();
  }

  private Channel channel(String repositoryId, String pullRequestId) {
    Repository repository = RepositoryTestData.createHeartOfGold();
    repository.setId(repositoryId);
    PullRequest pullRequest = TestData.createPullRequest(pullRequestId);
    return registry.channel(repository, pullRequest);
  }

}
