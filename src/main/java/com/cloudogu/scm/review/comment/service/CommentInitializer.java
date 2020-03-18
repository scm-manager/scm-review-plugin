/**
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

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.inject.Inject;
import org.apache.shiro.SecurityUtils;
import sonia.scm.ContextEntry;
import sonia.scm.NotFoundException;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.DiffFile;
import sonia.scm.repository.api.DiffLine;
import sonia.scm.repository.api.DiffResult;
import sonia.scm.repository.api.DiffResultCommandBuilder;
import sonia.scm.repository.api.Hunk;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class CommentInitializer {

  private final Clock clock;
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public CommentInitializer(RepositoryServiceFactory repositoryServiceFactory) {
    this(repositoryServiceFactory, Clock.systemDefaultZone());
  }

  private CommentInitializer(RepositoryServiceFactory repositoryServiceFactory, Clock clock) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.clock = clock;
  }

  void initialize(BasicComment comment, PullRequest pullRequest, String repositoryId) {
    comment.setDate(clock.instant());
    comment.setAuthor(getCurrentUserId());

    if (comment instanceof Comment) {
      initializeContextFromDiff((Comment) comment, pullRequest, repositoryId);
    }
  }

  private void initializeContextFromDiff(Comment comment, PullRequest pullRequest, String repositoryId) {
    if (comment.getLocation() != null && comment.getLocation().getHunk() != null) {
      try (RepositoryService repositoryService = repositoryServiceFactory.create(repositoryId)) {
        if (repositoryService.isSupported(Command.DIFF_RESULT)) {
          initializeContextFromDiff(comment, pullRequest, repositoryService);
        }
      }
    }
  }

  private void initializeContextFromDiff(Comment comment, PullRequest pullRequest, RepositoryService repositoryService) {
    DiffResultCommandBuilder diffResultCommand = repositoryService.getDiffResultCommand();
    diffResultCommand.setRevision(pullRequest.getSource()).setAncestorChangeset(pullRequest.getTarget());
    DiffResult diffResult = getDiffResult(repositoryService.getRepository(), diffResultCommand);

    List<ContextLine> contextLines =
      computeContext(comment, diffResult)
      .stream()
      .map(ContextLine::copy)
      .collect(Collectors.toList());
    comment.setContext(new InlineContext(contextLines));
  }

  private DiffResult getDiffResult(Repository repository, DiffResultCommandBuilder diffResultCommand) {
    try {
      return diffResultCommand.getDiffResult();
    } catch (IOException e) {
      throw new InternalRepositoryException(repository, "could not load diff result", e);
    }
  }

  private List<DiffLine> computeContext(Comment comment, DiffResult diffResult) {
    DiffFile matchingDiffFile = findMatchingDiffFile(comment, diffResult);
    Hunk matchingHunk = findMatchingHunk(comment, matchingDiffFile);
    return extractContextLines(comment, matchingHunk);
  }

  private DiffFile findMatchingDiffFile(Comment comment, DiffResult diffResult) {
    for (DiffFile diffFile : diffResult) {
      if (diffFile.getNewPath().equals(comment.getLocation().getFile())
        || diffFile.getOldPath().equals(comment.getLocation().getFile())) {
        return diffFile;
      }
    }
    throw NotFoundException.notFound(ContextEntry.ContextBuilder.entity("fileName", comment.getLocation().getFile()));
  }

  private Hunk findMatchingHunk(Comment comment, DiffFile diffFile) {
    if (comment.getLocation().getNewLineNumber() != null) {
      return findMatchingHunkForNewLineNumber(comment, diffFile);
    } else {
      return findMatchingHunkForOldLineNumber(comment, diffFile);
    }
  }

  private Hunk findMatchingHunkForNewLineNumber(Comment comment, DiffFile diffFile) {
    for (Hunk hunk : diffFile) {
      Integer commentNewLineNumber = comment.getLocation().getNewLineNumber();
      if (commentNewLineNumber >= hunk.getNewStart() && commentNewLineNumber < (hunk.getNewStart() + hunk.getNewLineCount())) {
        return hunk;
      }
    }
    throw NotFoundException.notFound(ContextEntry.ContextBuilder.entity("lineNumber", comment.getLocation().getNewLineNumber().toString()));
  }

  private Hunk findMatchingHunkForOldLineNumber(Comment comment, DiffFile diffFile) {
    for (Hunk hunk : diffFile) {
      Integer commentOldLineNumber = comment.getLocation().getOldLineNumber();
      if (commentOldLineNumber >= hunk.getOldStart() && commentOldLineNumber < (hunk.getOldStart() + hunk.getOldLineCount())) {
        return hunk;
      }
    }
    throw NotFoundException.notFound(ContextEntry.ContextBuilder.entity("lineNumber", comment.getLocation().getOldLineNumber().toString()));
  }

  private List<DiffLine> extractContextLines(Comment comment, Hunk hunk) {

    ContextCollector<DiffLine> contextLines = new ContextCollector<>();

    for (DiffLine line : hunk) {
      if (comment.getLocation().getNewLineNumber() != null) {
        addLineToContext(contextLines, line, comment.getLocation().getNewLineNumber(), line.getNewLineNumber());
      } else {
        addLineToContext(contextLines, line, comment.getLocation().getOldLineNumber(), line.getOldLineNumber());
      }
    }
    return contextLines.getContext();
  }

  private void addLineToContext(ContextCollector<DiffLine> contextLines, DiffLine line, Integer commentLineNumber, OptionalInt hunkLineNumber) {
    if (hunkLineNumber.isPresent() && hunkLineNumber.getAsInt() == commentLineNumber) {
      contextLines.addCentral(line);
    } else {
      contextLines.add(line);
    }
  }

  private String getCurrentUserId() {
    return SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
  }
}
