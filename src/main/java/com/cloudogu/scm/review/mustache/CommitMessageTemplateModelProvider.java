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

package com.cloudogu.scm.review.mustache;

import com.cloudogu.mustache.MustacheModelProvider;
import sonia.scm.plugin.Extension;

import java.util.List;

@Extension
public class CommitMessageTemplateModelProvider implements MustacheModelProvider {

  @Override
  public List<String> getModel() {
    return List.of(
      "namespace",
      "repositoryName",
      "pullRequest.id",
      "pullRequest.title",
      "pullRequest.description",
      "pullRequest.source",
      "pullRequest.target",
      "author.displayName",
      "author.mail",
      "currentUser.displayName",
      "currentUser.mail",
      "date",
      "localDate",
      "changesets[]",
      "changeset.author.name",
      "changeset.author.mail",
      "changeset.description",
      "changeset.id",
      "contributors[]",
      "contributor.type",
      "contributor.person.name",
      "contributor.person.mail"
    );
  }

  @Override
  public String getName() {
    return "CommitMessageTemplate";
  }

}
