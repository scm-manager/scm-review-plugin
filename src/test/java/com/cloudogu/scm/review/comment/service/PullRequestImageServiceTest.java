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


import com.cloudogu.scm.review.RepositoryResolver;
import com.cloudogu.scm.review.TestData;
import com.cloudogu.scm.review.pullrequest.service.PullRequest;
import org.apache.shiro.authz.UnauthorizedException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.HandlerEventType;
import sonia.scm.NotFoundException;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.store.Blob;
import sonia.scm.store.BlobStore;
import sonia.scm.store.InMemoryBlobStoreFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware("TrainerRed")
class PullRequestImageServiceTest {

  private final Path imageAPath = Paths.get("src", "test", "resources", "images", "a.png");
  private final String imageAHash = "522121e72cb937698b72fb6ab5311541abef03fe476abf2aeda6968860412571";

  private final Path imageBPath = Paths.get("src", "test", "resources", "images", "b.png");
  private final String imageBHash = "169a5323f000a499cb2a5d8068ecd799f31b93a96fdc23a7dfd2149b7be9871b";

  private final Path imageTooBigPath = Paths.get("src", "test", "resources", "images", "tooBig.png");
  private final String imageTooBigHash = "2e00ac33fe4c96a4f2f5090c1009214c34ee3fd083e278ae0fba75d9ff13e1c1";

  private final Repository repository = RepositoryTestData.create42Puzzle();

  private final PullRequest featurePullRequest = TestData.createPullRequest("feature");

  private Comment featureComment;
  private final Reply firstFeatureReply = Reply.createReply("1", "first", "TrainerRed");
  private final Reply secondFeatureReply = Reply.createReply("2", "second", "TrainerRed");

  private Comment hotfixComment;
  private final Reply thirdHotfixReply = Reply.createReply("3", "third", "TrainerRed");

  private InMemoryBlobStoreFactory blobStoreFactory;

  @Mock
  private RepositoryResolver repositoryResolver;

  @Mock
  private CommentStoreFactory commentStoreFactory;

  @Mock
  private CommentStore featureCommentStore;

  private PullRequestImageService imageService;

  @BeforeEach
  void setUp() {
    featureComment = TestData.createComment();
    featureComment.addReply(firstFeatureReply);
    featureComment.addReply(secondFeatureReply);

    hotfixComment = TestData.createComment();
    hotfixComment.setId("2");
    hotfixComment.addReply(thirdHotfixReply);

    blobStoreFactory = new InMemoryBlobStoreFactory();
    imageService = new PullRequestImageService(blobStoreFactory, repositoryResolver, commentStoreFactory, 8_000_000);

    lenient().when(repositoryResolver.resolve(any(NamespaceAndName.class))).thenReturn(repository);
    lenient().when(commentStoreFactory.create(repository)).thenReturn(featureCommentStore);
    lenient().when(featureCommentStore.getAll(featurePullRequest.getId())).thenReturn(List.of(featureComment, hotfixComment));
    lenient().when(featureCommentStore.getPullRequestCommentById(featurePullRequest.getId(), featureComment.getId())).thenReturn(Optional.of(featureComment));
    lenient().when(featureCommentStore.getPullRequestCommentById(featurePullRequest.getId(), hotfixComment.getId())).thenReturn(Optional.of(hotfixComment));
  }

  private InputStream openImage(Path path) throws IOException {
    return Files.newInputStream(path, StandardOpenOption.READ);
  }

  private BlobStore getBlobStore(Repository repository) {
    return blobStoreFactory.withName("image-store").forRepository(repository).build();
  }

  private String createBlobId(String pullRequestId, String fileHash) {
    return String.format("%s-%s", pullRequestId, fileHash);
  }

  private void assertImage(Blob image, String expectedImageId, Path imagePath) throws IOException {
    assertThat(image.getId()).isEqualTo(expectedImageId);
    InputStream inputStream = image.getInputStream();
    assertThat(new String(inputStream.readNBytes("image/png".length()))).isEqualTo("image/png");
    assertThat(inputStream.read()).isEqualTo(0);
    assertThat(inputStream.readAllBytes()).isEqualTo(openImage(imagePath).readAllBytes());
  }

  @Nested
  class GetPullRequestImageTests {

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseRepositoryWasNotFound() {
      when(repositoryResolver.resolve(any(NamespaceAndName.class))).thenThrow(NotFoundException.class);

      assertThatThrownBy(() -> {
        imageService.getPullRequestImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          imageAHash
        );
      }).isInstanceOf(NotFoundException.class);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseImageWasNotFound() {
      assertThatThrownBy(() -> {
        imageService.getPullRequestImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          imageAHash
        );
      }).isInstanceOf(NotFoundException.class);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldReturnImageBlob() throws IOException {
      getBlobStore(repository).create(createBlobId(featurePullRequest.getId(), imageAHash));

      PullRequestImageService.ImageStream image = imageService.getPullRequestImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        imageAHash
      );

      assertThat(image.getImageStream())
        .hasSameContentAs(getBlobStore(repository).get(createBlobId(featurePullRequest.getId(), imageAHash)).getInputStream());
    }
  }

  @Nested
  class CreateReplyImageTests {
    @Test
    void shouldThrowErrorBecauseOfMissingPermissions() {
      assertThatThrownBy(() -> {
        imageService.createReplyImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          firstFeatureReply.getId(),
          imageAHash,
          "image/png",
          openImage(imageAPath)
        );
      }).isInstanceOf(UnauthorizedException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseImageIsTooBig() {
      assertThatThrownBy(() -> {
        imageService.createReplyImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          firstFeatureReply.getId(),
          imageTooBigHash,
          "image/png",
          openImage(imageTooBigPath)
        );
      }).isInstanceOf(ImageTooBigException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseRepositoryWasNotFound() {
      when(repositoryResolver.resolve(any(NamespaceAndName.class))).thenThrow(NotFoundException.class);

      assertThatThrownBy(() -> {
        imageService.createReplyImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          firstFeatureReply.getId(),
          imageAHash,
          "image/png",
          openImage(imageAPath)
        );
      }).isInstanceOf(NotFoundException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseCommentWasNotFound() {
      when(
        featureCommentStore.getPullRequestCommentById(featurePullRequest.getId(), featureComment.getId())
      ).thenReturn(Optional.empty());

      assertThatThrownBy(() -> {
        imageService.createReplyImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          firstFeatureReply.getId(),
          imageAHash,
          "image/png",
          openImage(imageAPath)
        );
      }).isInstanceOf(NotFoundException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseReplyWasNotFound() {
      assertThatThrownBy(() -> {
        imageService.createReplyImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          "unknownReplyId",
          imageAHash,
          "image/png",
          openImage(imageAPath)
        );
      }).isInstanceOf(NotFoundException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseFileHashAndImageDoesNotMatch() {
      assertThatThrownBy(() -> {
        imageService.createReplyImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          firstFeatureReply.getId(),
          imageAHash,
          "image/png",
          openImage(imageBPath)
        );
      }).isInstanceOf(HashAndContentMismatchException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateNewImageForReply() throws IOException {
      imageService.createReplyImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        firstFeatureReply.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      String imageId = createBlobId(featurePullRequest.getId(), imageAHash);
      assertThat(firstFeatureReply.getAssignedImages()).isEqualTo(Set.of(imageId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(1);

      assertImage(images.get(0), imageId, imageAPath);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateTwoDifferentImagesForReply() throws IOException {
      imageService.createReplyImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        firstFeatureReply.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      imageService.createReplyImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        firstFeatureReply.getId(),
        imageBHash,
        "image/png",
        openImage(imageBPath)
      );

      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      String imageBId = createBlobId(featurePullRequest.getId(), imageBHash);
      assertThat(firstFeatureReply.getAssignedImages()).isEqualTo(Set.of(imageAId, imageBId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(2);

      assertImage(images.get(0), imageAId, imageAPath);
      assertImage(images.get(1), imageBId, imageBPath);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateSameImageOnlyOnceForReply() throws IOException {
      imageService.createReplyImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        firstFeatureReply.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      imageService.createReplyImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        firstFeatureReply.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      assertThat(firstFeatureReply.getAssignedImages()).isEqualTo(Set.of(imageAId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(1);

      assertImage(images.get(0), imageAId, imageAPath);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateSameImageOnlyOnceForDifferentReplies() throws IOException {
      imageService.createReplyImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        firstFeatureReply.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      imageService.createReplyImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        secondFeatureReply.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      assertThat(firstFeatureReply.getAssignedImages()).isEqualTo(Set.of(imageAId));
      assertThat(secondFeatureReply.getAssignedImages()).isEqualTo(Set.of(imageAId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(1);

      assertImage(images.get(0), imageAId, imageAPath);
    }
  }

  @Nested
  class CreateCommentImageTests {

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseImageIsTooBig() {
      assertThatThrownBy(() -> {
        imageService.createCommentImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          imageTooBigHash,
          "image/png",
          openImage(imageTooBigPath)
        );
      }).isInstanceOf(ImageTooBigException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    void shouldThrowErrorBecauseOfMissingPermissions() {
      assertThatThrownBy(() -> {
        imageService.createCommentImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          imageAHash,
          "image/png",
          openImage(imageAPath)
        );
      }).isInstanceOf(UnauthorizedException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseRepositoryWasNotFound() {
      when(repositoryResolver.resolve(any(NamespaceAndName.class))).thenThrow(NotFoundException.class);

      assertThatThrownBy(() -> {
        imageService.createCommentImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          imageAHash,
          "image/png",
          openImage(imageAPath)
        );
      }).isInstanceOf(NotFoundException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseCommentWasNotFound() {
      when(
        featureCommentStore.getPullRequestCommentById(featurePullRequest.getId(), featureComment.getId())
      ).thenReturn(Optional.empty());

      assertThatThrownBy(() -> {
        imageService.createCommentImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          imageAHash,
          "image/png",
          openImage(imageAPath)
        );
      }).isInstanceOf(NotFoundException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseFileHashAndImageDoesNotMatch() {
      assertThatThrownBy(() -> {
        imageService.createCommentImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          imageAHash,
          "image/png",
          openImage(imageBPath)
        );
      }).isInstanceOf(HashAndContentMismatchException.class);

      assertThat(getBlobStore(repository).getAll()).isEmpty();
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldThrowErrorBecauseFiletypeIsNotGiven() throws IOException {
      assertThatThrownBy(() -> {
        imageService.createCommentImage(
          new NamespaceAndName(repository.getNamespace(), repository.getName()),
          featurePullRequest.getId(),
          featureComment.getId(),
          imageAHash,
          null,
          openImage(imageAPath)
        );
      }).isInstanceOf(ImageUploadFailedException.class);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateNewImageForComment() throws IOException {
      imageService.createCommentImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      String imageId = createBlobId(featurePullRequest.getId(), imageAHash);
      assertThat(featureComment.getAssignedImages()).isEqualTo(Set.of(imageId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(1);

      assertImage(images.get(0), imageId, imageAPath);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateTwoDifferentImagesForComment() throws IOException {
      imageService.createCommentImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      imageService.createCommentImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        imageBHash,
        "image/png",
        openImage(imageBPath)
      );

      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      String imageBId = createBlobId(featurePullRequest.getId(), imageBHash);
      assertThat(featureComment.getAssignedImages()).isEqualTo(Set.of(imageAId, imageBId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(2);

      assertImage(images.get(0), imageAId, imageAPath);
      assertImage(images.get(1), imageBId, imageBPath);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateSameImageOnlyOnceForComment() throws IOException {
      imageService.createCommentImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      imageService.createCommentImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      assertThat(featureComment.getAssignedImages()).isEqualTo(Set.of(imageAId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(1);

      assertImage(images.get(0), imageAId, imageAPath);
    }

    @Test
    @SubjectAware(permissions = "repository:commentPullRequest:*")
    void shouldCreateSameImageOnlyOnceForDifferentComments() throws IOException {
      imageService.createCommentImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        featureComment.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      imageService.createCommentImage(
        new NamespaceAndName(repository.getNamespace(), repository.getName()),
        featurePullRequest.getId(),
        hotfixComment.getId(),
        imageAHash,
        "image/png",
        openImage(imageAPath)
      );

      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      assertThat(featureComment.getAssignedImages()).isEqualTo(Set.of(imageAId));
      assertThat(hotfixComment.getAssignedImages()).isEqualTo(Set.of(imageAId));

      List<Blob> images = getBlobStore(repository).getAll();
      assertThat(images).hasSize(1);

      assertImage(images.get(0), imageAId, imageAPath);
    }
  }

  @Nested
  class OnReplyDeletedTests {

    @Test
    void shouldNotDeleteAnyImagesBecauseReplyWasNotDeleted() {
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(createBlobId(featurePullRequest.getId(), imageAHash));
      firstFeatureReply.getAssignedImages().add(image.getId());

      imageService.onReplyDeleted(new ReplyEvent(repository, featurePullRequest, null, firstFeatureReply, featureComment, HandlerEventType.MODIFY));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }

    @Test
    void shouldDeleteImageABecauseReplyWasOnlyReference() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      blobStore.create(imageAId);

      firstFeatureReply.getAssignedImages().add(imageAId);
      imageService.onReplyDeleted(new ReplyEvent(repository, featurePullRequest, null, firstFeatureReply, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).isEmpty();
    }

    @Test
    void shouldDeleteImageAAndBBecauseReplyWasOnlyReference() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      String imageBId = createBlobId(featurePullRequest.getId(), imageBHash);
      BlobStore blobStore = getBlobStore(repository);
      blobStore.create(imageAId);
      blobStore.create(imageBId);

      firstFeatureReply.getAssignedImages().addAll(List.of(imageAId, imageBId));
      imageService.onReplyDeleted(new ReplyEvent(repository, featurePullRequest, null, firstFeatureReply, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).isEmpty();
    }

    @Test
    void shouldNotDeleteImageABecauseRootCommentIsReferencing() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(imageAId);

      featureComment.getAssignedImages().add(imageAId);
      firstFeatureReply.getAssignedImages().add(imageAId);
      imageService.onReplyDeleted(new ReplyEvent(repository, featurePullRequest, null, firstFeatureReply, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }

    @Test
    void shouldNotDeleteImageABecauseOtherReplyOfRootCommentIsReferencing() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(imageAId);

      firstFeatureReply.getAssignedImages().add(imageAId);
      secondFeatureReply.getAssignedImages().add(imageAId);
      imageService.onReplyDeleted(new ReplyEvent(repository, featurePullRequest, null, firstFeatureReply, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }

    @Test
    void shouldNotDeleteImageABecauseOtherRootCommentIsReferencing() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(imageAId);

      firstFeatureReply.getAssignedImages().add(imageAId);
      hotfixComment.getAssignedImages().add(imageAId);
      imageService.onReplyDeleted(new ReplyEvent(repository, featurePullRequest, null, firstFeatureReply, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }

    @Test
    void shouldNotDeleteImageABecauseOtherReplyOfOtherRootCommentIsReferencing() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(imageAId);

      firstFeatureReply.getAssignedImages().add(imageAId);
      thirdHotfixReply.getAssignedImages().add(imageAId);
      imageService.onReplyDeleted(new ReplyEvent(repository, featurePullRequest, null, firstFeatureReply, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }
  }

  @Nested
  class OnCommentDeletedTests {

    @Test
    void shouldNotDeleteAnyImagesBecauseCommentWasNotDeleted() {
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(createBlobId(featurePullRequest.getId(), imageAHash));
      firstFeatureReply.getAssignedImages().add(image.getId());

      imageService.onCommentDeleted(new CommentEvent(repository, featurePullRequest, null, featureComment, HandlerEventType.MODIFY));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }

    @Test
    void shouldDeleteImageABecauseCommentWasOnlyReference() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      blobStore.create(imageAId);

      featureComment.getAssignedImages().add(imageAId);
      imageService.onCommentDeleted(new CommentEvent(repository, featurePullRequest, null, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).isEmpty();
    }

    @Test
    void shouldDeleteImageAAndBBecauseCommentWasOnlyReference() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      String imageBId = createBlobId(featurePullRequest.getId(), imageBHash);
      BlobStore blobStore = getBlobStore(repository);
      blobStore.create(imageAId);
      blobStore.create(imageBId);

      featureComment.getAssignedImages().addAll(List.of(imageAId, imageBId));
      imageService.onCommentDeleted(new CommentEvent(repository, featurePullRequest, null, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).isEmpty();
    }

    @Test
    void shouldDeleteImageABecauseReplyOfCommentWasOnlyReference() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      blobStore.create(imageAId);

      firstFeatureReply.getAssignedImages().add(imageAId);
      imageService.onCommentDeleted(new CommentEvent(repository, featurePullRequest, null, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).isEmpty();
    }

    @Test
    void shouldNotDeleteImageABecauseOtherCommentIsReferencing() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(imageAId);

      featureComment.getAssignedImages().add(imageAId);
      hotfixComment.getAssignedImages().add(imageAId);
      imageService.onCommentDeleted(new CommentEvent(repository, featurePullRequest, null, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }

    @Test
    void shouldNotDeleteImageABecauseReplyOfOtherCommentIsReferencing() {
      String imageAId = createBlobId(featurePullRequest.getId(), imageAHash);
      BlobStore blobStore = getBlobStore(repository);
      Blob image = blobStore.create(imageAId);

      featureComment.getAssignedImages().add(imageAId);
      thirdHotfixReply.getAssignedImages().add(imageAId);
      imageService.onCommentDeleted(new CommentEvent(repository, featurePullRequest, null, featureComment, HandlerEventType.DELETE));

      assertThat(blobStore.getAll()).hasSize(1);
      assertThat(blobStore.getAll().get(0)).isEqualTo(image);
    }

  }
}
