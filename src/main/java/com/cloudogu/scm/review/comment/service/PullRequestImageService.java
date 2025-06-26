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

import com.cloudogu.scm.review.PermissionCheck;
import com.cloudogu.scm.review.RepositoryResolver;
import com.github.legman.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.config.ConfigValue;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.store.Blob;
import sonia.scm.store.BlobStore;
import sonia.scm.store.BlobStoreFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.cloudogu.scm.review.comment.AcceptedImageTypes.MAX_ACCEPTED_IMAGE_TYPE_STRING_LENGTH;

@Singleton
public class PullRequestImageService {

  private static final String IMAGE_STORE = "image-store";
  private final BlobStoreFactory blobStoreFactory;
  private final RepositoryResolver repositoryResolver;

  private final CommentStoreBuilder commentStoreBuilder;

  private final int maxImageSizeInBytes;

  @Inject
  public PullRequestImageService(BlobStoreFactory blobStoreFactory,
                                 RepositoryResolver repositoryResolver,
                                 CommentStoreBuilder commentStoreBuilder,
                                 @ConfigValue(
                                   key = "maxImageSizeInBytes",
                                   defaultValue = "8000000",
                                   description = "The maximum size for images uploaded in a pull request comment")
                                 int maxImageSizeInBytes) {
    this.blobStoreFactory = blobStoreFactory;
    this.repositoryResolver = repositoryResolver;
    this.commentStoreBuilder = commentStoreBuilder;
    this.maxImageSizeInBytes = maxImageSizeInBytes;
  }

  public void createReplyImage(NamespaceAndName namespaceAndName, String pullRequestId, String commentId, String replyId, String fileHash, String filetype, InputStream imageStream) {
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    PermissionCheck.checkComment(repository);
    CommentStore commentStore = commentStoreBuilder.create(repository);
    Comment comment = commentStore.getPullRequestCommentById(pullRequestId, commentId)
      .orElseThrow(() -> new NotFoundException(Comment.class, commentId));
    Reply reply = comment.getReplies().stream().filter(r -> r.getId().equals(replyId)).findFirst()
      .orElseThrow(() -> new NotFoundException(Reply.class, replyId));

    String imageId = createPullRequestImage(repository, pullRequestId, fileHash, filetype, imageStream);
    reply.getAssignedImages().add(imageId);
    commentStore.update(pullRequestId, comment);
  }

  public void createCommentImage(NamespaceAndName namespaceAndName, String pullRequestId, String commentId, String fileHash, String filetype, InputStream imageStream) {
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    PermissionCheck.checkComment(repository);
    CommentStore commentStore = commentStoreBuilder.create(repository);
    Comment comment = commentStore.getPullRequestCommentById(pullRequestId, commentId)
      .orElseThrow(() -> new NotFoundException(Comment.class, commentId));

    String imageId = createPullRequestImage(repository, pullRequestId, fileHash, filetype, imageStream);
    comment.getAssignedImages().add(imageId);
    commentStore.update(pullRequestId, comment);
  }

  @SneakyThrows(NoSuchAlgorithmException.class)
  private String createPullRequestImage(Repository repository, String pullRequestId, String fileHash, String filetype, InputStream imageStream) {
    if (filetype == null) {
      throw new ImageUploadFailedException(new RuntimeException("Filetype is missing for file hash " + fileHash));
    }
    String imageId = buildImageId(pullRequestId, fileHash);
    BlobStore imageStore = createImageStore(repository);

    if (imageStore.getOptional(imageId).isPresent()) {
      return imageId;
    }

    Blob imageBlob = imageStore.create(imageId);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");

    try (OutputStream imageOutputStream = imageBlob.getOutputStream();
         DigestInputStream digestInputStream = new DigestInputStream(imageStream, digest)) {
      imageOutputStream.write(filetype.getBytes());
      imageOutputStream.write(0);
      IOUtils.copyLarge(digestInputStream, imageOutputStream, 0, maxImageSizeInBytes);
      if (imageStream.read() != -1) {
        throw new ImageTooBigException();
      }

      if (!bytesToHex(digest.digest()).equals(fileHash)) {
        throw new HashAndContentMismatchException();
      }
    } catch (IOException e) {
      imageStore.remove(imageBlob);
      throw new ImageUploadFailedException(e);
    } catch (Exception e) {
      imageStore.remove(imageBlob);
      throw e;
    }

    return imageId;
  }

  private BlobStore createImageStore(Repository repository) {
    return blobStoreFactory.withName(IMAGE_STORE).forRepository(repository).build();
  }

  private String buildImageId(String pullRequestId, String fileHash) {
    return String.format("%s-%s", pullRequestId, fileHash);
  }

  private String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte hashByte : hash) {
      hexString.append(String.format("%02x", hashByte));
    }

    return hexString.toString();
  }

  public ImageStream getPullRequestImage(NamespaceAndName namespaceAndName, String pullRequestId, String fileHash) {
    Repository repository = repositoryResolver.resolve(namespaceAndName);
    BlobStore imageStore = createImageStore(repository);
    return imageStore
      .getOptional(buildImageId(pullRequestId, fileHash))
      .map(ImageStream::new)
      .orElseThrow(() -> new NotFoundException("Comment Image", buildImageId(pullRequestId, fileHash)));
  }

  @Subscribe
  public void onCommentDeleted(CommentEvent event) {
    if (event.getEventType() != HandlerEventType.DELETE) {
      return;
    }

    Set<String> imagesPossiblyToDelete = new HashSet<>(event.getOldItem().getAssignedImages());
    event.getOldItem().getReplies().forEach(reply -> imagesPossiblyToDelete.addAll(reply.getAssignedImages()));
    deleteImages(event, imagesPossiblyToDelete, (imagesToDelete, comments) ->
      comments.stream()
        .filter(comment -> !comment.getId().equals(event.getOldItem().getId()))
        .forEach(comment -> {
          imagesToDelete.removeAll(comment.getAssignedImages());
          comment.getReplies().forEach(reply -> imagesToDelete.removeAll(reply.getAssignedImages()));
        })
    );
  }

  @Subscribe
  public void onReplyDeleted(ReplyEvent event) {
    if (event.getEventType() != HandlerEventType.DELETE) {
      return;
    }

    Set<String> imagesPossiblyToDelete = new HashSet<>(event.getOldItem().getAssignedImages());
    deleteImages(event, imagesPossiblyToDelete, (imagesToDelete, comments) ->
      comments.forEach(comment -> {
        imagesToDelete.removeAll(comment.getAssignedImages());
        comment.getReplies().stream()
          .filter(reply -> isNotDeletedReply(comment, event.getRootComment(), reply, event.getOldItem()))
          .forEach(reply -> imagesToDelete.removeAll(reply.getAssignedImages()));
      })
    );
  }

  private void deleteImages(BasicCommentEvent<?> event, Set<String> imagesToDelete, FilterImagesToDelete filter) {
    BlobStore imageStore = createImageStore(event.getRepository());
    CommentStore commentStore = commentStoreBuilder.create(event.getRepository());
    filter.handle(imagesToDelete, commentStore.getAll(event.getPullRequest().getId()));
    imagesToDelete.forEach(imageStore::remove);
  }

  private boolean isNotDeletedReply(Comment comment, Comment rootCommentOfDeletedReply, Reply reply, Reply deletedReply) {
    return !comment.getId().equals(rootCommentOfDeletedReply.getId()) || !reply.getId().equals(deletedReply.getId());
  }

  @FunctionalInterface
  private interface FilterImagesToDelete {
    void handle(Set<String> imagesToDelete, Collection<Comment> comments);
  }

  @Getter
  @SuppressWarnings("java:S112") // The RuntimeExceptions are not likely to be thrown
  public static class ImageStream {
    private final InputStream imageStream;
    private final String filetype;

    public ImageStream(Blob blob) {
      try {
        this.imageStream = blob.getInputStream();
        this.filetype = readFileType(blob);
      } catch (IOException e) {
        throw new RuntimeException("Could not read filetype from input stream for blob " + blob.getId(), e);
      }
    }

    private String readFileType(Blob blob) throws IOException {
      StringBuilder builder = new StringBuilder();
      int b;
      while ((b = imageStream.read()) > 0) {
        if (builder.length() > MAX_ACCEPTED_IMAGE_TYPE_STRING_LENGTH) {
          throw new RuntimeException("Illegal filetype int blob " + blob.getId());
        }
        builder.append((char) b);
      }
      return builder.toString();
    }
  }
}
