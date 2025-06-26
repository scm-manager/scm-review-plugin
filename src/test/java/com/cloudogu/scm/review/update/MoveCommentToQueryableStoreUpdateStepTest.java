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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sonia.scm.migration.RepositoryUpdateContext;
import sonia.scm.store.InMemoryByteDataStore;
import sonia.scm.store.InMemoryByteDataStoreFactory;
import sonia.scm.store.QueryableMutableStore;
import sonia.scm.store.QueryableStoreExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(QueryableStoreExtension.class)
@QueryableStoreExtension.QueryableTypes(Comment.class)
class MoveCommentToQueryableStoreUpdateStepTest {

  private static final String COMMENT_XML = """
    <?xml version="1.0" ?>
    <pull-request-comments>
      <comment>
        <id>9pUoMiyApWN</id>
        <comment>[Changelog entry file](https://github.com/scm-manager/changelog#changelog-entry-files) created in `gradle/changelog`

    Available types:

    - `fixed`
    - `added`
    - `changed`

    The description should not start with "Added" or "Fixed" but should say, **what** has been added or fixed.</comment>
        <author>rpfeuffer</author>
        <date>1750149506252</date>
        <executedTransitions>
          <id>DoUoOB5iOdy</id>
          <transition>com.cloudogu.scm.review.comment.service.CommentTransition:SET_DONE</transition>
          <date>1750170984764</date>
          <user>rpfeuffer</user>
        </executedTransitions>
        <systemComment>false</systemComment>
        <type>TASK_DONE</type>
        <outdated>false</outdated>
        <emergencyMerged>false</emergencyMerged>
        <systemCommentParameters></systemCommentParameters>
      </comment>
      <comment>
        <id>1uUoS4y6mUG</id>
        <comment>Do we have a unit test covering this?</comment>
        <author>tdiegeler</author>
        <date>1750228630876</date>
        <executedTransitions>
          <id>CPUoT6YEUbb</id>
          <transition>com.cloudogu.scm.review.comment.service.CommentTransition:SET_DONE</transition>
          <date>1750243784426</date>
          <user>rpfeuffer</user>
        </executedTransitions>
        <location>
          <file>scm-webapp/src/main/java/sonia/scm/store/QueryableStoreDeletionHandler.java</file>
          <hunk>@@ -46,6 +46,10 @@</hunk>
          <newLineNumber>49</newLineNumber>
        </location>
        <systemComment>false</systemComment>
        <type>TASK_DONE</type>
        <outdated>false</outdated>
        <context>
          <lines>
            <oldLineNumber>47</oldLineNumber>
            <newLineNumber>47</newLineNumber>
            <content>    }</content>
          </lines>
          <lines>
            <oldLineNumber>48</oldLineNumber>
            <newLineNumber>48</newLineNumber>
            <content>    Collection&lt;Class&lt;?&gt;&gt; typesWithParent = metaDataProvider.getTypesWithParent(classes);</content>
          </lines>
          <lines>
            <oldLineNumber>49</oldLineNumber>
            <content>    typesWithParent.forEach(type -&gt; storeFactory.getForMaintenance(type, ids).clear());</content>
          </lines>
          <lines>
            <newLineNumber>49</newLineNumber>
            <content>    typesWithParent.forEach(type -&gt; {</content>
          </lines>
          <lines>
            <newLineNumber>50</newLineNumber>
            <content>      try (QueryableMaintenanceStore&lt;?&gt; store = storeFactory.getForMaintenance(type, ids)) {</content>
          </lines>
          <lines>
            <newLineNumber>51</newLineNumber>
            <content>        store.clear();</content>
          </lines>
          <lines>
            <newLineNumber>52</newLineNumber>
            <content>      }</content>
          </lines>
        </context>
        <emergencyMerged>false</emergencyMerged>
        <systemCommentParameters></systemCommentParameters>
      </comment>
    </pull-request-comments>
    """;

  private final InMemoryByteDataStoreFactory dataStoreFactory = new InMemoryByteDataStoreFactory();

  private MoveCommentToQueryableStoreUpdateStep updateStep;

  @BeforeEach
  void initUpdateStep(CommentStoreFactory commentStoreFactory) {
    updateStep = new MoveCommentToQueryableStoreUpdateStep(dataStoreFactory, commentStoreFactory);
  }

  @Test
  void x(CommentStoreFactory commentStoreFactory) throws Exception {
    ((InMemoryByteDataStore<Comment>) dataStoreFactory.withType(Comment.class)
      .withName("pullRequestComment")
      .forRepository("test-repo")
      .build())
      .putRawXml("1", COMMENT_XML);

    updateStep.doUpdate(new RepositoryUpdateContext("test-repo"));

    try (QueryableMutableStore<Comment> store = commentStoreFactory.getMutable("test-repo", "1")) {
      assertThat(store.getAll()).hasSize(2);
    }
  }
}
