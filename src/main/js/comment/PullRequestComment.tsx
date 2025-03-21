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

import React, { FC, ReactText, useState } from "react";
import classNames from "classnames";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { ConfirmAlert, confirmAlert, DateFromNow, ErrorNotification } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { Comment, CommentImage, Mention, PossibleTransition, PullRequest } from "../types/PullRequest";
import { useDeleteComment, useTransformComment, useUpdateComment, useUpdateCommentWithImages } from "../pullRequest";
import CommentSpacingWrapper from "./CommentSpacingWrapper";
import CommentActionToolbar from "./CommentActionToolbar";
import CommentTags from "./CommentTags";
import CommentContent from "./CommentContent";
import CommentMetadata from "./CommentMetadata";
import ContextModal from "./ContextModal";
import LastEdited from "./LastEdited";
import EditButtons from "./EditButtons";
import Replies from "./Replies";
import ReplyEditor from "./ReplyEditor";
import MentionTextarea from "./MentionTextarea";
import handleImagePaste from "./handleImagePaste";

const LinkWithInheritColor = styled.a`
  color: inherit;
`;

const AuthorName = styled.span`
  margin-left: 5px;
`;

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  parent?: Comment;
  comment: Comment;
  createLink?: string;
  createWithImageLink?: string;
};

const PullRequestComment: FC<Props> = ({
  repository,
  pullRequest,
  parent,
  comment,
  createLink,
  createWithImageLink
}) => {
  const [t] = useTranslation("plugins");
  const [collapsed, setCollapsed] = useState(comment.type === "TASK_DONE");
  const [edit, setEdit] = useState(false);
  const [changed, setChanged] = useState(false);
  const [contextModalOpen, setContextModalOpen] = useState(false);
  const [showConfirmDeleteModal, setShowConfirmDeleteModal] = useState(false);
  const [commentType, setCommentType] = useState(comment.type);
  const [images, setImages] = useState<CommentImage[]>([]);
  const [commentText, setCommentText] = useState(comment.comment || "");
  const [mentions, setMentions] = useState<Mention[]>(comment.mentions || []);
  const [replyEditor, setReplyEditor] = useState<Comment>();

  const { remove, isLoading: deleteLoading, error: deleteError } = useDeleteComment(repository, pullRequest);

  const { update } = useUpdateComment(repository, pullRequest);
  const { update: updateWithImage } = useUpdateCommentWithImages(repository, pullRequest);
  const { transform } = useTransformComment(repository, pullRequest);

  const startUpdate = () => {
    setEdit(true);
    setChanged(false);
  };

  const cancelUpdate = () => {
    setEdit(false);
    setCommentText(comment.comment || "");
    setCommentType(comment.type);
    setMentions(comment.mentions);
  };

  const updateComment = () => {
    if (!changed) {
      setEdit(false);
      return;
    }

    if (images.length === 0) {
      update({ ...comment, type: commentType, comment: commentText, mentions });
    } else {
      updateWithImage({ ...comment, type: commentType, comment: commentText, mentions }, images);
    }
    setEdit(false);
  };

  const executeTransition = (transition: string) => {
    if (!(comment && comment._embedded && comment._embedded.possibleTransitions)) {
      throw new Error("comment has no possible transitions");
    }

    const transformation = comment._embedded.possibleTransitions.find(
      (pt: PossibleTransition) => pt.name === transition
    );
    if (!transformation) {
      throw new Error("comment does not have transition " + transition);
    }

    transform(transformation);
  };

  const confirmTransition = (transition: string, translationKey: string) => {
    confirmAlert({
      title: t(translationKey + ".title"),
      message: t(translationKey + ".message"),
      buttons: [
        {
          className: "is-outlined",
          label: t(translationKey + ".submit"),
          onClick: () => executeTransition(transition)
        },
        {
          label: t(translationKey + ".cancel"),
          onClick: () => null,
          autofocus: true
        }
      ]
    });
  };

  const handleChanges = (event: any) => {
    setChanged(true);
    setCommentText(event.target.value);
  };

  const createEditIcons = () => {
    return (
      <CommentActionToolbar
        parent={parent}
        comment={comment}
        collapsed={collapsed}
        createLink={createLink}
        onUpdate={startUpdate}
        onDelete={() => setShowConfirmDeleteModal(true)}
        deleteLoading={deleteLoading}
        onReply={() => setReplyEditor(comment)}
        onTransitionChange={confirmTransition}
      />
    );
  };

  const createMessageEditor = () => {
    return (
      <>
        <MentionTextarea
          value={commentText}
          comment={{ ...comment, comment: commentText, type: commentType, mentions }}
          onAddMention={(id: ReactText, display: string) => {
            setMentions([...mentions, { id: id as string, displayName: display, mail: "" }]);
          }}
          onChange={handleChanges}
          onSubmit={updateComment}
          onCancel={cancelUpdate}
          onPaste={handleImagePaste(createWithImageLink || "", setImages, setCommentText)}
          autofocus
        />
      </>
    );
  };

  const collectTags = (c: Comment) => {
    let onOpenContext;
    if (c.context) {
      onOpenContext = () => setContextModalOpen(true);
    }

    return <CommentTags comment={c} onOpenContext={onOpenContext} />;
  };

  const createReplyEditorIfNeeded = (id?: string) => {
    const replyComment = replyEditor;
    if (replyComment && replyComment.id === id) {
      return (
        <ReplyEditor
          repository={repository}
          pullRequest={pullRequest}
          comment={parent ? parent : comment}
          onCancel={() => setReplyEditor(undefined)}
        />
      );
    }
  };

  if (deleteError) {
    return <ErrorNotification error={deleteError} />;
  }

  let icons = null;
  let editButtons = null;
  let message;

  if (edit) {
    message = createMessageEditor();
    editButtons = (
      <EditButtons comment={comment} onSubmit={updateComment} onCancel={cancelUpdate} changed={() => changed} />
    );
  } else {
    message = !collapsed ? <CommentContent comment={comment} /> : "";
    icons = createEditIcons();
  }

  const collapseTitle = collapsed ? t("scm-review-plugin.comment.expand") : t("scm-review-plugin.comment.collapse");
  const collapseIcon = collapsed ? "fa-angle-right" : "fa-angle-down";

  return (
    <>
      {showConfirmDeleteModal ? (
        <ConfirmAlert
          title={t("scm-review-plugin.comment.confirmDeleteAlert.title")}
          message={t("scm-review-plugin.comment.confirmDeleteAlert.message")}
          buttons={[
            {
              label: t("scm-review-plugin.comment.confirmDeleteAlert.submit"),
              onClick: () => remove(comment)
            },
            {
              className: "is-info",
              label: t("scm-review-plugin.comment.confirmDeleteAlert.cancel"),
              onClick: () => null,
              autofocus: true
            }
          ]}
          close={() => setShowConfirmDeleteModal(false)}
        />
      ) : null}
      {contextModalOpen ? <ContextModal comment={comment} onClose={() => setContextModalOpen(false)} /> : null}
      <CommentSpacingWrapper isChildComment={!!parent}>
        <article id={`comment-${comment.id}`} className="media">
          <div className="media-content is-clipped content">
            <p>
              <LinkWithInheritColor onClick={() => setCollapsed(!collapsed)} title={collapseTitle}>
                <span className="icon is-small">
                  <i className={classNames("fa", collapseIcon)} />
                </span>
                <AuthorName>
                  <strong>{comment.author.displayName}</strong>{" "}
                </AuthorName>
              </LinkWithInheritColor>
              <CommentMetadata>
                <DateFromNow date={comment.date} /> <LastEdited comment={comment} />
              </CommentMetadata>
              {collectTags(comment)}
              <br />
              {message}
            </p>
            {editButtons}
          </div>
          {icons}
        </article>
      </CommentSpacingWrapper>
      {!collapsed && (
        <Replies
          repository={repository}
          pullRequest={pullRequest}
          comment={comment}
          createLink={createLink}
          createWithImagesLink={createWithImageLink}
        />
      )}
      {createReplyEditorIfNeeded(comment?.id)}
    </>
  );
};

export default PullRequestComment;
