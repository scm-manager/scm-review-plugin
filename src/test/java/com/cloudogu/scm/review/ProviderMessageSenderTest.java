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

package com.cloudogu.scm.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.RepositoryHookEvent;
import sonia.scm.repository.api.HookMessageProvider;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderMessageSenderTest {

  @Mock
  private ScmConfiguration scmConfiguration;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryHookEvent event;
  @Mock
  private HookMessageProvider messageProvider;

  @InjectMocks
  private ProviderMessageSender sender;

  @BeforeEach
  void setUpMessageProvider() {
    when(event.getContext().getMessageProvider())
      .thenReturn(messageProvider);
  }

  @BeforeEach
  void mockBaseUrl() {
    when(scmConfiguration.getBaseUrl()).thenReturn("https://hog/scm");
  }

  @Test
  void shouldEncodeBranchInUrl() {
    sender.sendCreatePullRequestMessage("nice+branch");

    verify(messageProvider).sendMessage("https://hog/scm/repo/null/null/pull-requests/add/changesets/?source=nice%2Bbranch");
  }
}
