package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.dto.DisplayedUserDto;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.plugin.Extension;
import sonia.scm.search.Hit;
import sonia.scm.user.DisplayUser;
import sonia.scm.user.UserDisplayManager;

import javax.inject.Inject;
import java.util.Optional;

@Extension
@Enrich(Hit.class)
@SuppressWarnings("UnstableApiUsage")
public class HitEnricher implements HalEnricher {

  private final UserDisplayManager userDisplayManager;

  @Inject
  public HitEnricher(UserDisplayManager userDisplayManager) {
    this.userDisplayManager = userDisplayManager;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Optional<Hit> hit = context.oneByType(Hit.class);
    if (hit.isPresent() && hit.get().getFields().containsKey("author")) {
      appendEmbeddedUserForAuthor(appender, hit.get());
    }
  }

  private void appendEmbeddedUserForAuthor(HalAppender appender, Hit hit) {
    Hit.Field author = hit.getFields().get("author");
    Optional<DisplayUser> displayUser = userDisplayManager.get(((Hit.ValueField) author).getValue().toString());
    if (displayUser.isPresent()) {
      DisplayUser user = displayUser.get();
      appender.appendEmbedded("user", new DisplayedUserDto(user.getId(), user.getDisplayName(), user.getMail()));
    }
  }
}
