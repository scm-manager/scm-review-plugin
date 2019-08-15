package com.cloudogu.scm.review.comment.service;

import com.google.common.collect.EvictingQueue;
import com.google.inject.Inject;
import org.apache.shiro.SecurityUtils;
import sonia.scm.ContextEntry;
import sonia.scm.NotFoundException;
import sonia.scm.repository.api.DiffFile;
import sonia.scm.repository.api.DiffLine;
import sonia.scm.repository.api.DiffResult;
import sonia.scm.repository.api.DiffResultCommandBuilder;
import sonia.scm.repository.api.Hunk;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.OptionalInt;

public class CommentInitializer {
  public static final int CONTEXT_SIZE = 7;
  private final Clock clock;
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public CommentInitializer(RepositoryServiceFactory repositoryServiceFactory) {
    this(repositoryServiceFactory, Clock.systemDefaultZone());
  }

  CommentInitializer(RepositoryServiceFactory repositoryServiceFactory, Clock clock) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.clock = clock;
  }

  public void initialize(Comment comment, String repositoryId) throws IOException {
    comment.setDate(clock.instant());
    comment.setAuthor(getCurrentUserId());

    if (comment.getLocation() != null && comment.getLocation().getHunk() != null) {
      try (RepositoryService repositoryService = repositoryServiceFactory.create(repositoryId)) {
        DiffResultCommandBuilder diffResultCommand = repositoryService.getDiffResultCommand();
        DiffResult diffResult = diffResultCommand.getDiffResult();

        EvictingQueue<DiffLine> contextLines = computeContext(comment, diffResult);
        comment.setContext(new InlineContext(new ArrayList<>(contextLines)));
      }
    }
  }

  private EvictingQueue<DiffLine> computeContext(Comment comment, DiffResult diffResult) {
    DiffFile matchingDiffFile = findMatchingDiffFile(comment, diffResult);
    Hunk matchingHunk = findMatchingHunk(comment, matchingDiffFile);
    return extractContextLines(comment, matchingHunk);
  }

  private DiffFile findMatchingDiffFile(Comment comment, DiffResult diffResult) {
    for (DiffFile diffFile : diffResult) {
      if (diffFile.getNewPath().equals(comment.getLocation().getFile())) {
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

  private EvictingQueue<DiffLine> extractContextLines(Comment comment, Hunk hunk) {
    EvictingQueue<DiffLine> contextLines = EvictingQueue.create(CONTEXT_SIZE);
    Integer trailingLineCount = null;
    boolean lineFound = false;
    for (DiffLine line : hunk) {
      if (comment.getLocation().getNewLineNumber() != null) {
        lineFound = addLineToContext(contextLines, line, comment.getLocation().getNewLineNumber(), line.getNewLineNumber());
      } else {
        lineFound = addLineToContext(contextLines, line, comment.getLocation().getOldLineNumber(), line.getOldLineNumber());
      }
      if (lineFound) {
        trailingLineCount = CONTEXT_SIZE / 2 + 1;
      }
      if (trailingLineCount != null) {
        trailingLineCount = trailingLineCount - 1;
        if (trailingLineCount <= 0 && contextLines.size() >= CONTEXT_SIZE) {
          return contextLines;
        }
      }
    }
    return contextLines;
  }

  private boolean addLineToContext(EvictingQueue<DiffLine> contextLines, DiffLine line, Integer commentLineNumber, OptionalInt hunkLineNumber) {
    contextLines.add(line);
    return hunkLineNumber.isPresent() && hunkLineNumber.getAsInt() == commentLineNumber;
  }

  private String getCurrentUserId() {
    return SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
  }
}
