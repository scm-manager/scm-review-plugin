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

package com.cloudogu.scm.review.comment.service;

import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sonia.scm.repository.InternalRepositoryException;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.DiffFile;
import sonia.scm.repository.api.DiffLine;
import sonia.scm.repository.api.DiffResult;
import sonia.scm.repository.api.Hunk;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;

@Slf4j
class LocationVerifier {

  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  LocationVerifier(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  void verifyLocation(Comment pullRequestComment, PullRequest pullRequest, Repository repository) {
    if (pullRequestComment.getLocation() != null) {
      Location location = pullRequestComment.getLocation();
      try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
        verifyLocation(pullRequestComment, pullRequest, repository, repositoryService, location);
      } catch (IOException e) {
        log.warn(
          "exception while checking diff for hunk for comment on pull request {} in repository {} for location in " +
            "file {}, old line number {}, new line number {}",
          pullRequest.getId(),
          repository,
          location.getFile(),
          location.getOldLineNumber(),
          location.getNewLineNumber(),
          e
        );
        throw new InternalRepositoryException(repository, "could not check diff to compute hunk of location");
      }
    }
  }

  private void verifyLocation(
    Comment pullRequestComment,
    PullRequest pullRequest,
    Repository repository,
    RepositoryService repositoryService,
    Location location
  ) throws IOException {
    DiffFile diffFile = findDiffFile(repositoryService, pullRequest, repository, location);
    if (locationHasToBeChecked(location)) {
      String hunk = findHunk(pullRequest, repository, location, diffFile);
      if (location.getHunk() != null && !hunk.equals(location.getHunk())) {
        throw new LocationForCommentNotFoundException(
          repository.getNamespaceAndName(),
          pullRequest.getId(),
          location
        );
      }
      pullRequestComment.getLocation().setHunk(hunk);
    }
  }

  private DiffFile findDiffFile(
    RepositoryService repositoryService, PullRequest pullRequest, Repository repository, Location location
  ) throws IOException {
    DiffResult diffResult =
      repositoryService.getDiffResultCommand()
        .setRevision(pullRequest.getSource())
        .setAncestorChangeset(pullRequest.getTarget())
        .getDiffResult();
    for (DiffFile diffFile : diffResult) {
      if (isCorrectDiffFile(location, diffFile)) {
        return diffFile;
      }
    }
    throw new LocationForCommentNotFoundException(
      repository.getNamespaceAndName(),
      pullRequest.getId(),
      location.getFile()
    );
  }

  private String findHunk(PullRequest pullRequest, Repository repository, Location location, DiffFile diffFile) {
    for (Hunk hunk : diffFile) {
      if (isContainedInHunk(hunk, location)) {
        checkLines(location, hunk, repository, pullRequest);
        return hunk.getRawHeader();
      }
    }
    throw new LocationForCommentNotFoundException(
      repository.getNamespaceAndName(),
      pullRequest.getId(),
      location,
      MoreObjects.firstNonNull(location.getOldLineNumber(), location.getNewLineNumber())
    );
  }

  private static boolean isCorrectDiffFile(Location location, DiffFile diffFile) {
    return location.getFile().equals(diffFile.getNewPath()) ||
      "/dev/null".equals(diffFile.getNewPath()) && location.getFile().equals(diffFile.getOldPath());
  }

  private void checkLines(Location location, Hunk hunk, Repository repository, PullRequest pullRequest) {
    for (DiffLine line : hunk) {
      checkLine(location, line, repository, pullRequest);
    }
  }

  private void checkLine(Location location, DiffLine line, Repository repository, PullRequest pullRequest) {
    if (location.getNewLineNumber() != null && line.getNewLineNumber().orElse(-1) == location.getNewLineNumber()) {
      line.getOldLineNumber().ifPresent(oldLineNumber -> {
        if (location.getOldLineNumber() == null) {
          location.setOldLineNumber(oldLineNumber);
        } else if (!location.getOldLineNumber().equals(oldLineNumber)) {
          throw new LocationForCommentNotFoundException(
            repository.getNamespaceAndName(),
            pullRequest.getId(),
            location,
            location.getOldLineNumber()
          );
        }
      });
    } else if (location.getOldLineNumber() != null &&
      line.getOldLineNumber().orElse(-1) == location.getOldLineNumber()) {
      line.getNewLineNumber().ifPresent(newLineNumber -> {
        if (location.getNewLineNumber() == null) {
          location.setNewLineNumber(newLineNumber);
        } else if (!location.getNewLineNumber().equals(newLineNumber)) {
          throw new LocationForCommentNotFoundException(
            repository.getNamespaceAndName(),
            pullRequest.getId(),
            location,
            location.getNewLineNumber()
          );
        }
      });
    }
  }

  private boolean isContainedInHunk(Hunk hunk, Location location) {
    return location.getNewLineNumber() != null ?
           hunk.getNewStart() <= location.getNewLineNumber() &&
             hunk.getNewStart() + hunk.getNewLineCount() > location.getNewLineNumber() :
           hunk.getOldStart() <= location.getOldLineNumber() &&
             hunk.getOldStart() + hunk.getOldLineCount() > location.getOldLineNumber();
  }

  private boolean locationHasToBeChecked(Location location) {
    return location != null && (location.getOldLineNumber() != null || location.getNewLineNumber() != null);
  }
}
