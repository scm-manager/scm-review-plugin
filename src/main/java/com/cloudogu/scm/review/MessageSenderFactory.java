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

import sonia.scm.config.ScmConfiguration;
import sonia.scm.repository.RepositoryHookEvent;

import jakarta.inject.Inject;

import static sonia.scm.repository.api.HookFeature.MESSAGE_PROVIDER;

public class MessageSenderFactory {

  private final ScmConfiguration configuration;

  @Inject
  public MessageSenderFactory(ScmConfiguration configuration) {
    this.configuration = configuration;
  }

  public MessageSender create(RepositoryHookEvent event) {
    if (event.getContext().isFeatureSupported(MESSAGE_PROVIDER)) {
      return new ProviderMessageSender(configuration, event);
    } else {
      return new NoOpMessageSender();
    }
  }
}
