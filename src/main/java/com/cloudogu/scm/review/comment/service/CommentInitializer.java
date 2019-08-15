package com.cloudogu.scm.review.comment.service;

import com.google.common.collect.EvictingQueue;
import com.google.inject.Inject;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.shiro.SecurityUtils;
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
import java.util.List;

public class CommentInitializer {
  public static final int CONTEXT_SIZE = 7;
  private final Clock clock;
  private final DiffResultCommandBuilder diffResultCommandBuilder;
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public CommentInitializer(RepositoryServiceFactory repositoryServiceFactory, DiffResultCommandBuilder diffResultCommandBuilder) {
    this(repositoryServiceFactory, diffResultCommandBuilder, Clock.systemDefaultZone());
  }

  CommentInitializer(RepositoryServiceFactory repositoryServiceFactory, DiffResultCommandBuilder diffResultCommandBuilder, Clock clock) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.diffResultCommandBuilder = diffResultCommandBuilder;
    this.clock = clock;
  }

  public void initialize(Comment comment, String repositoryId) throws IOException {
    comment.setDate(clock.instant());
    comment.setAuthor(getCurrentUserId());

    if (comment.getLocation() != null && comment.getLocation().getHunk() != null) {
      try (RepositoryService repositoryService = repositoryServiceFactory.create(repositoryId)) {
        DiffResultCommandBuilder diffResultCommand = repositoryService.getDiffResultCommand();
        DiffResult diffResult = diffResultCommand.getDiffResult();

        for (DiffFile diffFile : diffResult) {
          if (diffFile.getNewPath().equals(comment.getLocation().getFile())) {
            for (Hunk hunk : diffFile) {
              Integer commentNewLineNumber = comment.getLocation().getNewLineNumber();
              if (commentNewLineNumber >= hunk.getNewStart() && commentNewLineNumber < (hunk.getNewStart() + hunk.getNewLineCount())) {
                EvictingQueue<DiffLine> contextLines = EvictingQueue.create(CONTEXT_SIZE);
                for (DiffLine line : hunk) {
                  int contextEndLineNumber = comment.getLocation().getNewLineNumber() + CONTEXT_SIZE / 2;

                  if (line.getNewLineNumber().getAsInt() <= contextEndLineNumber || contextLines.size() < CONTEXT_SIZE) {
                    contextLines.add(line);
                  }
                }
                comment.setContext(new InlineContext(new ArrayList<>(contextLines)));
              }
            }
          }

        }
      }
    }
  }

  private String getCurrentUserId() {
    return SecurityUtils.getSubject().getPrincipals().getPrimaryPrincipal().toString();
  }
}
