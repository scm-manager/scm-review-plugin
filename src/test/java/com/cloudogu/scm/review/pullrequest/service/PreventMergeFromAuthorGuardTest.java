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

import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.config.service.ConfigService;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.user.User;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreventMergeFromAuthorGuardTest {

  private static final String CURRENT_USER = "dent";

  private static PullRequest PULL_REQUEST = TestData.createPullRequest();
  private static Repository REPOSITORY = RepositoryTestData.createHeartOfGold();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  Subject subject;
  @Mock
  ConfigService configService;

  @InjectMocks
  PreventMergeFromAuthorGuard guard;

  @BeforeEach
  void initSubject() {
    ThreadContext.bind(subject);
    lenient().when(subject.getPrincipals().oneByType(User.class)).thenReturn(new User(CURRENT_USER));
  }

  @Test
  void shouldNotCheckAnythingWhenDisabled() {
    when(configService.isPreventMergeFromAuthor(REPOSITORY)).thenReturn(false);

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles).isEmpty();
  }

  @Test
  void shouldPreventMergeFromAuthorWhenEnabled() {
    when(configService.isPreventMergeFromAuthor(REPOSITORY)).thenReturn(true);
    PULL_REQUEST.setAuthor(CURRENT_USER);

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles)
      .extracting("key")
      .containsExactly("scm-review-plugin.pullRequest.guard.mergeFromAuthorNotAllowed.obstacle");
  }

  @Test
  void shouldNotPreventMergeFromOtherUsersWhenEnabled() {
    when(configService.isPreventMergeFromAuthor(REPOSITORY)).thenReturn(true);
    PULL_REQUEST.setAuthor("trillian");

    Collection<MergeObstacle> obstacles = guard.getObstacles(REPOSITORY, PULL_REQUEST);

    assertThat(obstacles).isEmpty();
  }
}
