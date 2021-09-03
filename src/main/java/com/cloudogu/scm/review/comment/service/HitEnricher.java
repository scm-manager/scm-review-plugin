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
