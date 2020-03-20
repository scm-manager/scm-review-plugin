/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
