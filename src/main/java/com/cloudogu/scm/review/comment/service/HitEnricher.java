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

import jakarta.inject.Inject;
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
    if (hit.isPresent() && shouldAppendDisplayUser(hit.get())) {
      appendEmbeddedUserForAuthor(appender, hit.get());
    }
  }

  private boolean shouldAppendDisplayUser(Hit hit) {
    // We only need display users for comments so far
    return hit.getFields().containsKey("author") && hit.getFields().containsKey("pullRequestId");
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
