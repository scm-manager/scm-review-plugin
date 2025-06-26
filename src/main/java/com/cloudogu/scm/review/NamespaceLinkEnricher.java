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

import com.cloudogu.scm.review.config.api.NamespaceConfigResource;
import com.cloudogu.scm.review.config.service.ConfigService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Namespace;

import static com.cloudogu.scm.review.PermissionCheck.mayConfigure;

@Extension
@Enrich(Namespace.class)
public class NamespaceLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final ConfigService configService;


  @Inject
  public NamespaceLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, ConfigService configService) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.configService = configService;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    String namespace = context.oneRequireByType(String.class);
    if (mayConfigure(namespace) && !configService.getGlobalPullRequestConfig().isDisableRepositoryConfiguration()) {
      LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), NamespaceConfigResource.class);
      appender.appendLink("pullRequestConfig", linkBuilder.method("getNamespaceConfig").parameters(namespace).href());
    }
  }
}
