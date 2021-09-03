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
  void shouldNotEnrichHitIfUserNotFound() {
    when(context.oneByType(Hit.class))
      .thenReturn(Optional.of(new Hit("1", "1", 1f, ImmutableMap.of("author", new Hit.ValueField("trillian")))));

    when(userDisplayManager.get("trillian")).thenReturn(Optional.empty());
    enricher.enrich(context, appender);

    verify(appender, never()).appendEmbedded(eq("user"), any(HalRepresentation.class));
  }

  @Test
  void shouldEnrichHitWithDisplayUser() {
    when(context.oneByType(Hit.class))
      .thenReturn(Optional.of(new Hit("1", "1", 1f, ImmutableMap.of("author", new Hit.ValueField("trillian")))));

    when(userDisplayManager.get("trillian")).thenReturn(Optional.of(DisplayUser.from(new User("trillian", "Tricia McMillan", "trillian@hitchhiker.org"))));
    enricher.enrich(context, appender);

    verify(appender, times(1)).appendEmbedded(eq("user"), (DisplayedUserDto) argThat(user -> {
      assertThat(((DisplayedUserDto)user).getDisplayName()).isEqualTo("Tricia McMillan");
      return true;
    }));
  }
}
