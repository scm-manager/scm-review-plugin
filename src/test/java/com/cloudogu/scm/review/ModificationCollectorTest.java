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
package com.cloudogu.scm.review;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Added;
import sonia.scm.repository.Removed;
import sonia.scm.repository.Modified;
import sonia.scm.repository.Modification;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.ModificationsCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModificationCollectorTest {

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private ModificationsCommandBuilder modificationsCommandBuilder;

  @InjectMocks
  private ModificationCollector collector;


  private Repository repository = RepositoryTestData.createHeartOfGold();

  private Map<String, Modifications> modificationMap;

  private String currentRevision;

  @BeforeEach
  void setUpRepositoryService() throws IOException {
    modificationMap = new HashMap<>();

    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
    when(repositoryService.getModificationsCommand()).thenReturn(modificationsCommandBuilder);

    lenient().doAnswer(ic -> {
      currentRevision = ic.getArgument(0);
      return modificationsCommandBuilder;
    }).when(modificationsCommandBuilder).revision(anyString());

    lenient().doAnswer(ic -> modificationMap.get(currentRevision)).when(modificationsCommandBuilder).getModifications();
  }

  @Test
  void shouldCollectModification() throws IOException {
    Changeset one = changeset("21")
      .added("a.txt")
      .removed("b.txt")
      .build();

    Changeset two = changeset("42")
      .modified("c.txt", "d.txt")
      .build();

    Set<String> modifications = collector.collect(repository, ImmutableList.of(one, two));
    assertThat(modifications).containsExactlyInAnyOrder("a.txt", "b.txt", "c.txt", "d.txt");
  }

  @Test
  void shouldCloseRepositorySerice() throws IOException {
    Changeset one = changeset("21")
      .added("a.txt")
      .removed("b.txt")
      .build();

    collector.collect(repository, ImmutableList.of(one));

    verify(repositoryService).close();
  }

  @Test
  void shouldCloseRepositoryServiceEvenOnError() throws IOException {
    when(repositoryService.getModificationsCommand()).thenThrow(new IllegalStateException("fail"));

    Changeset one = changeset("21")
      .added("a.txt")
      .removed("b.txt")
      .build();

    try {
      collector.collect(repository, ImmutableList.of(one));
    } catch (IllegalStateException ex) {
      // we only want to verify, if the repository service was closed
    }

    verify(repositoryService).close();
  }

  private Builder changeset(String id) {
    return new Builder(id);
  }

  private class Builder {

    private final Changeset changeset;
    private final List<Modification> modifications = new ArrayList<>();

    private Builder(String id) {
      this.changeset = new Changeset();
      this.changeset.setId(id);
    }

    Builder added(String... paths) {
      Arrays.stream(paths).map(Added::new).forEach(modifications::add);
      return this;
    }

    Builder modified(String... paths) {
      Arrays.stream(paths).map(Modified::new).forEach(modifications::add);
      return this;
    }

    Builder removed(String... paths) {
      Arrays.stream(paths).map(Removed::new).forEach(modifications::add);
      return this;
    }

    Changeset build() {
      modificationMap.put(changeset.getId(), new Modifications(changeset.getId(), modifications));
      return changeset;
    }
  }
}
