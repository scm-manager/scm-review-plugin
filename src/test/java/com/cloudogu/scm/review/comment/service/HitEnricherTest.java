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
package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import com.google.common.collect.ImmutableMap;
import de.otto.edison.hal.HalRepresentation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.search.Hit;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.User;
import sonia.scm.user.UserDisplayManager;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("UnstableApiUsage")
class HitEnricherTest {

  @Mock
  private UserDisplayManager userDisplayManager;

  @Mock
  private HalEnricherContext context;
  @Mock
  private HalAppender appender;

  @InjectMocks
  private HitEnricher enricher;

  @Test
  void shouldNotEnrichHitIfNotComment() {
    mockHit(ImmutableMap.of("author", new Hit.ValueField("trillian")));

    enricher.enrich(context, appender);

    verify(appender, never()).appendEmbedded(eq("user"), any(HalRepresentation.class));
  }

  @Test
  void shouldNotEnrichHitIfUserNotFound() {
    mockHit(ImmutableMap.of("author", new Hit.ValueField("trillian"), "pullRequestId", new Hit.ValueField("1")));

    when(userDisplayManager.get("trillian")).thenReturn(Optional.empty());
    enricher.enrich(context, appender);

    verify(appender, never()).appendEmbedded(eq("user"), any(HalRepresentation.class));
  }

  @Test
  void shouldEnrichHitWithDisplayUser() {
    mockHit(ImmutableMap.of("author", new Hit.ValueField("trillian"), "pullRequestId", new Hit.ValueField("1")));

    when(userDisplayManager.get("trillian"))
      .thenReturn(Optional.of(DisplayUser.from(new User("trillian", "Tricia McMillan", "trillian@hitchhiker.org"))));
    enricher.enrich(context, appender);

    verify(appender, times(1)).appendEmbedded(eq("user"), (DisplayedUserDto) argThat(user -> {
      assertThat(((DisplayedUserDto)user).getDisplayName()).isEqualTo("Tricia McMillan");
      return true;
    }));
  }

  private void mockHit(Map<String, Hit.Field> hitMap) {
    when(context.oneByType(Hit.class)).thenReturn(Optional.of(new Hit("1", "1", 1f, hitMap)));
  }
}
