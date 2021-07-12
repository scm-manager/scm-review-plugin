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
import React, { FC, ReactText, useState } from "react";
import classNames from "classnames";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import { ConfirmAlert, confirmAlert, DateFromNow, ErrorNotification } from "@scm-manager/ui-components";
import { Repository } from "@scm-manager/ui-types";
import { Comment, PossibleTransition, PullRequest } from "../types/PullRequest";
import {
  useDeletePullRequestComment,
  useTransformPullRequestComment,
  useUpdatePullRequestComment
} from "../pullRequest";
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
};

const PullRequestComment: FC<Props> = ({ repository, pullRequest, parent, comment, createLink }) => {
  const [t] = useTranslation("plugins");
  const [collapsed, setCollapsed] = useState(comment.type === "TASK_DONE");
  const [edit, setEdit] = useState(false);
  const [changed, setChanged] = useState(false);
  const [contextModalOpen, setContextModalOpen] = useState(false);
  const [showConfirmDeleteModal, setShowConfirmDeleteModal] = useState(false);
  const [updatedComment, setUpdatedComment] = useState({ ...comment });
  const [replyEditor, setReplyEditor] = useState<Comment>();

  const { deleteComment, isLoading: deleteCommentLoading, error: deleteCommentError } = useDeletePullRequestComment(
    repository,
    pullRequest
  );

  const { updateComment } = useUpdatePullRequestComment(repository, pullRequest);
  const { transformComment } = useTransformPullRequestComment(repository, pullRequest);

  const startUpdate = () => {
    setEdit(true);
    setChanged(false);
  };

  const cancelUpdate = () => {
    setEdit(false);
    setUpdatedComment(comment);
  };

  const update = () => {
    if (!changed) {
      setEdit(false);
      return;
    }

    updateComment(updatedComment);
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

    transformComment(transformation);
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
          onClick: () => null
        }
      ]
    });
  };

  const handleChanges = (event: any) => {
    setChanged(true);
    setUpdatedComment({
      ...updatedComment,
      comment: event.target.value
    });
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
        deleteLoading={deleteCommentLoading}
        onReply={() => setReplyEditor(comment)}
        onTransitionChange={confirmTransition}
      />
    );
  };

  const createMessageEditor = () => {
    return (
      <>
        <MentionTextarea
          value={updatedComment?.comment ? updatedComment.comment : ""}
          comment={updatedComment}
          onAddMention={(id: ReactText, display: string) => {
            setUpdatedComment({
              ...updatedComment,
              mentions: [...updatedComment.mentions, { id: id as string, displayName: display, mail: "" }]
            });
          }}
          onChange={handleChanges}
          onSubmit={update}
          onCancel={cancelUpdate}
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

  if (deleteCommentError) {
    return <ErrorNotification error={deleteCommentError} />;
  }

  let icons = null;
  let editButtons = null;
  let message;

  if (edit) {
    message = createMessageEditor();
    editButtons = <EditButtons comment={comment} onSubmit={update} onCancel={cancelUpdate} changed={() => changed} />;
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
              className: "is-outlined",
              label: t("scm-review-plugin.comment.confirmDeleteAlert.submit"),
              onClick: () => deleteComment(comment)
            },
            {
              label: t("scm-review-plugin.comment.confirmDeleteAlert.cancel"),
              onClick: () => null
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
        <Replies repository={repository} pullRequest={pullRequest} comment={comment} createLink={createLink} />
      )}
      {createReplyEditorIfNeeded(comment?.id)}
    </>
  );
};

export default PullRequestComment;
