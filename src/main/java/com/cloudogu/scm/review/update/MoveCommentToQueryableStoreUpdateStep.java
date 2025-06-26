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

package com.cloudogu.scm.review.update;

import com.cloudogu.scm.review.comment.service.Comment;
import com.cloudogu.scm.review.comment.service.CommentStoreFactory;
import jakarta.inject.Inject;
import org.mapstruct.Mapper;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.migration.RepositoryUpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.version.Version;

import static org.mapstruct.CollectionMappingStrategy.ADDER_PREFERRED;

@Extension
public class MoveCommentToQueryableStoreUpdateStep implements RepositoryUpdateStep {
  private final DataStoreFactory dataStoreFactory;
  private final CommentStoreFactory commentStoreFactory;

  @Inject
  public MoveCommentToQueryableStoreUpdateStep(DataStoreFactory dataStoreFactory, CommentStoreFactory commentStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
    this.commentStoreFactory = commentStoreFactory;
  }

  @Override
  public void doUpdate(RepositoryUpdateContext repositoryUpdateContext) throws Exception {
    String repositoryId = repositoryUpdateContext.getRepositoryId();

    DataStore<LegacyXmlPullRequestComments> xmlStore =
      dataStoreFactory
        .withType(LegacyXmlPullRequestComments.class)
        .withName("pullRequestComment")
        .forRepository(repositoryId)
        .build();

    xmlStore.getAll()
      .forEach(
        (id, comments) -> {
          try (QueryableMutableStore<Comment> qStore = commentStoreFactory.getMutable(repositoryId, id)) {
            qStore.transactional(
              () -> {
                comments.getComments()
                  .stream()
                  .map(comment -> new LegacyCommentMapperImpl().map(comment))
                  .forEach(comment -> qStore.put(comment.getId(), comment));
                return true;
              });
          }
        }
      );
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.0.0");
  }

  @Override
  public String getAffectedDataType() {
    return "commentStore";
  }
}

@Mapper(collectionMappingStrategy = ADDER_PREFERRED)
interface LegacyCommentMapper {
  Comment map(LegacyXmlComment comments);
}
