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

import React, { ClipboardEvent, FC, ReactText, useState } from "react";
import { Button, ErrorNotification, Level, Loading, Radio, SubmitButton } from "@scm-manager/ui-components";
import { BasicComment, CommentImage, CommentType, Location, Mention, PullRequest } from "../types/PullRequest";
import { useTranslation } from "react-i18next";
import { useCreateComment, useCreateCommentWithImage } from "../pullRequest";
import { createChangeIdFromLocation } from "../diff/locations";
import MentionTextarea from "./MentionTextarea";
import { Repository } from "@scm-manager/ui-types";
import handleImagePaste from "./handleImagePaste";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  url: string;
  commentWithImageUrl: string;
  location?: Location;
  onCancel?: () => void;
  autofocus?: boolean;
  reply?: boolean;
};

const CreateComment: FC<Props> = ({ repository, pullRequest, url, commentWithImageUrl, location, onCancel, reply, autofocus }) => {
  const [t] = useTranslation("plugins");

  const { create, isLoading, error } = useCreateComment(repository, pullRequest);
  const createCommentWithImage = useCreateCommentWithImage(repository, pullRequest);

  const [commentType, setCommentType] = useState<CommentType>("COMMENT");
  const [commentText, setCommentText] = useState("");
  const [mentions, setMentions] = useState<Mention[]>([]);
  const [images, setImages] = useState<CommentImage[]>([]);

  const submit = () => {
    const comment: BasicComment = {
      type: commentType,
      comment: commentText,
      mentions: mentions,
      location
    };

    if (images.length !== 0 && commentWithImageUrl) {
      createCommentWithImage.create(commentWithImageUrl, comment, images);
    } else {
      create(url, comment);
    }

    setCommentText("");
    setCommentType("COMMENT");
    setMentions([]);
    setImages([]);
    if (onCancel) {
      onCancel();
    }
  };

  const createCommentEditorName = () => {
    let name = "editor";
    if (location) {
      name += location.file;
      if (location.oldLineNumber || location.newLineNumber) {
        name += "_" + createChangeIdFromLocation(location);
      }
    }
    return name;
  };

  const isValid = () => {
    return commentText && commentText.trim() !== "";
  };

  if (isLoading || createCommentWithImage.isLoading) {
    return <Loading />;
  }

  let cancelButton = null;
  if (onCancel) {
    cancelButton = <Button label={t("scm-review-plugin.comment.cancel")} color="warning" action={onCancel} />;
  }

  let toggleType = null;
  if (!reply) {
    const editorName = createCommentEditorName();
    toggleType = (
      <div className="field is-grouped">
        <div className="control">
          <Radio
            name={`comment_type_${editorName}`}
            value="COMMENT"
            checked={commentType === "COMMENT"}
            label={t("scm-review-plugin.comment.type.comment")}
            onChange={() => setCommentType("COMMENT")}
          />
          <Radio
            name={`comment_type_${editorName}`}
            value="TASK_TODO"
            checked={commentType === "TASK_TODO"}
            label={t("scm-review-plugin.comment.type.task")}
            onChange={() => setCommentType("TASK_TODO")}
          />
        </div>
      </div>
    );
  }

  const evaluateTranslation = () => {
    return commentType === "TASK_TODO"
      ? t("scm-review-plugin.comment.addTask")
      : t("scm-review-plugin.comment.addComment");
  };

  return (
    <>
      {url ? (
        <article className="media">
          <div className="media-content">
            <div className="field">
              <div className="control">
                <MentionTextarea
                  value={commentText}
                  comment={{ comment: commentText, type: commentType, mentions }}
                  placeholder={evaluateTranslation()}
                  onAddMention={(id: ReactText, displayName: string) => {
                    setMentions([...mentions, { id, displayName, mail: "" }]);
                  }}
                  onChange={event => setCommentText(event.target.value)}
                  onCancel={onCancel}
                  onSubmit={submit}
                  onPaste={handleImagePaste(commentWithImageUrl, setImages, setCommentText)}
                  autofocus={autofocus}
                />
              </div>
            </div>
            <ErrorNotification error={error} />
            <ErrorNotification error={createCommentWithImage.error} />
            {toggleType}
            <div className="field">
              <Level
                right={
                  <>
                    <div className="level-item">
                      <SubmitButton
                        label={evaluateTranslation()}
                        action={submit}
                        disabled={!isValid()}
                        loading={isLoading}
                        scrollToTop={false}
                      />
                    </div>
                    <div className="level-item">{cancelButton}</div>
                  </>
                }
              />
            </div>
          </div>
        </article>
      ) : (
        ""
      )}
    </>
  );
};

export default CreateComment;
