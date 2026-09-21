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

import React, { FC, ReactNode, useEffect, useState } from "react";
import styled from "styled-components";
import { useBranch } from "@scm-manager/ui-api";
import { Branch, Hunk, Repository } from "@scm-manager/ui-types";
import {
  AnnotationFactoryContext,
  DiffEventContext,
  diffs,
  File,
  FileContentFactory,
} from "@scm-manager/ui-components";
import { Comment, Comments, Location, PullRequest } from "../types/PullRequest";
import { createChangeIdFromLocation, createHunkId, createHunkIdFromLocation, isInlineLocation } from "./locations";
import PullRequestComment from "../comment/PullRequestComment";
import CreateComment from "../comment/CreateComment";
import CommentSpacingWrapper from "../comment/CommentSpacingWrapper";
import InlineComments from "./InlineComments";
import StyledDiffWrapper from "./StyledDiffWrapper";
import AddCommentButton from "./AddCommentButton";
import FileComments from "./FileComments";
import MarkReviewedButton from "./MarkReviewedButton";
import { useDiffCollapseState } from "./useDiffCollapseReducer";
import ChangeNotificationToast from "../ChangeNotificationToast";
import { useInvalidateDiff } from "../pullRequest";
import { useChangeNotificationContext } from "../ChangeNotificationContext";
import LoadingDiff from "./LoadingDiff";
import { addInlineEditor, InlineEditorState, removeInlineEditor } from "./inlineEditorState";

const CommentWrapper = styled.div`
  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb;
  }
`;

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  comments?: Comments;
  diffUrl: string;
  createLink?: string;
  createLinkWithImages?: string;
  fileContentFactory: FileContentFactory;
  reviewedFiles: string[];
  sourceBranch?: Branch;
  stickyHeaderHeight: number;
};

type Revisions = {
  source: string;
  target: string;
  changed: boolean;
};

const useHasChanged = (repository: Repository, pullRequest: PullRequest, source: Branch | undefined) => {
  const [revisions, setRevisions] = useState<Revisions>();
  const invalidate = useInvalidateDiff(repository, pullRequest);

  const { data: target } = useBranch(repository, pullRequest.target);

  const ctx = useChangeNotificationContext();

  const ignore = () => {
    if (revisions) {
      setRevisions({
        ...revisions,
        changed: false,
      });
    }
  };

  const reload = async () => {
    await invalidate();
    ignore();
  };

  // clear revisions on reload triggered by ChangeNotification to avoid duplicate notification
  useEffect(() => ctx.onReload(() => setRevisions(undefined)), [ctx]);

  // show notification only when source or target revision has changed
  useEffect(() => {
    if (source && target) {
      setRevisions((rev) => ({
        source: source.revision,
        target: target.revision,
        changed: !!(rev && (rev.source !== source.revision || rev.target !== target.revision)),
      }));
    }
  }, [source, target]);

  return {
    changed: revisions?.changed || false,
    ignore,
    reload,
  };
};

const Diff: FC<Props> = ({
  repository,
  pullRequest,
  comments,
  diffUrl,
  createLink,
  createLinkWithImages,
  fileContentFactory,
  reviewedFiles,
  sourceBranch,
  stickyHeaderHeight,
}) => {
  const { actions, isCollapsed } = useDiffCollapseState(pullRequest);
  const [openInlineEditors, setOpenInlineEditors] = useState<InlineEditorState>({});
  const [openFileEditors, setOpenFileEditors] = useState<string[]>([]);
  const { changed, ignore, reload } = useHasChanged(repository, pullRequest, sourceBranch);

  const openInlineEditor = (context: DiffEventContext) => {
    setOpenInlineEditors((state) => addInlineEditor(state, context));
  };

  const closeInlineEditor = (hunkId: string, changeId: string) => {
    setOpenInlineEditors((state) => removeInlineEditor(state, hunkId, changeId));
  };

  const closeFileEditor = (path: string) => {
    setOpenFileEditors((editors) => editors.filter((editorPath) => editorPath !== path));
  };

  const fileAnnotationFactory = (file: File) => {
    const path = diffs.getPath(file);
    const annotations = [];
    const fileComments: Comment[] = [];

    comments?._embedded.pullRequestComments.forEach((comment) => {
      if (!isInlineLocation(comment.location) && comment.location?.file === path) {
        fileComments.push(comment);
      }
    });

    if (fileComments && fileComments.length > 0) {
      annotations.push(createComments(fileComments));
    }

    if (openFileEditors.includes(path)) {
      annotations.push(
        createNewCommentEditor(
          {
            file: path,
          },
          () => closeFileEditor(path),
        ),
      );
    }

    if (annotations.length > 0) {
      return [<FileComments>{annotations}</FileComments>];
    }

    return [];
  };

  const annotationFactory = (context: AnnotationFactoryContext) => {
    const annotations: { [key: string]: React.ReactNode } = {};
    const hunkId = createHunkId(context);
    const commentsByLine: { [key: string]: Comment[] } = {};

    comments?._embedded.pullRequestComments
      .filter((comment) => comment.location?.hunk)
      .filter(
        (comment) =>
          comment.location?.hunk &&
          isInlineLocation(comment.location) &&
          createHunkIdFromLocation(comment.location) === hunkId,
      )
      .forEach((comment) => {
        if (comment.location) {
          const changeId = createChangeIdFromLocation(comment.location);
          let lineComments = commentsByLine[changeId];
          if (!lineComments) {
            lineComments = [];
            commentsByLine[changeId] = lineComments;
          }
          lineComments.push(comment);
        }
      });

    const lineAnnotations: { [key: string]: ReactNode[] } = {};
    Object.keys(commentsByLine).forEach((changeId) => {
      const createdComments = createComments(commentsByLine[changeId]);
      lineAnnotations[changeId] = [createdComments];
    });

    const editors = openInlineEditors[hunkId] || {};

    Object.entries(editors).forEach(([changeId, location]) => {
      let line = lineAnnotations[changeId];
      if (!line) {
        line = [];
        lineAnnotations[changeId] = line;
      }
      line.push(createNewCommentEditor(location, () => closeInlineEditor(hunkId, changeId)));
    });

    Object.keys(lineAnnotations).forEach((changeId) => {
      annotations[changeId] = <InlineComments>{lineAnnotations[changeId]}</InlineComments>;
    });

    return annotations;
  };

  const createFileControlsFactory = (contentFactory: FileContentFactory) => (file: File) => {
    const setReviewMark = (filepath: string, reviewed: boolean) => {
      if (reviewed) {
        actions.markAsReviewed(file);
      } else {
        actions.unmarkAsReviewed(file);
      }
    };

    if (isPermittedToComment()) {
      const openFileEditor = () => {
        const path = diffs.getPath(file);
        actions.openFileCommentEditor(file);
        setOpenFileEditors((editors) => (editors.includes(path) ? editors : [...editors, path]));
      };
      return (
        <>
          <MarkReviewedButton
            repository={repository}
            pullRequest={pullRequest}
            newPath={file.newPath}
            oldPath={file.oldPath}
            setReviewed={setReviewMark}
            reviewedFiles={reviewedFiles}
          />
          <AddCommentButton action={openFileEditor} />
          {contentFactory(file)}
        </>
      );
    } else {
      return <>{contentFactory(file)}</>;
    }
  };

  const onGutterClick = (context: DiffEventContext) => {
    if (isPermittedToComment() && !context.hunk.expansion) {
      openInlineEditor(context);
    }
  };

  const isPermittedToComment = () => {
    return !!createLink;
  };

  const createComments = (comments: Comment[]): ReactNode => {
    return (
      <>
        {comments.map((comment: Comment) => (
          <CommentWrapper key={comment.id} className="comment-wrapper">
            <PullRequestComment
              repository={repository}
              pullRequest={pullRequest}
              comment={comment}
              createLink={createLink}
              createWithImageLink={createLinkWithImages}
            />
          </CommentWrapper>
        ))}
      </>
    );
  };

  const createNewCommentEditor = (location: Location, closeEditor: () => void) => {
    if (createLink) {
      return (
        <CommentSpacingWrapper>
          <CreateComment
            repository={repository}
            pullRequest={pullRequest}
            url={createLink}
            commentWithImageUrl={createLinkWithImages}
            location={location}
            onCancel={closeEditor}
            autofocus={true}
          />
        </CommentSpacingWrapper>
      );
    }
    return null;
  };

  return (
    <StyledDiffWrapper commentable={isPermittedToComment()}>
      {changed ? <ChangeNotificationToast reload={reload} ignore={ignore} /> : null}
      <LoadingDiff
        isCollapsed={isCollapsed}
        onCollapseStateChange={actions.toggleCollapse}
        fileControlFactory={createFileControlsFactory(fileContentFactory)}
        fileAnnotationFactory={fileAnnotationFactory}
        annotationFactory={annotationFactory}
        onClick={onGutterClick}
        refetchOnWindowFocus={false}
        hunkClass={(hunk: Hunk) => (hunk.expansion ? "expanded" : "commentable")}
        diffUrl={diffUrl}
        actions={actions}
        pullRequestComments={comments?._embedded.pullRequestComments || []}
        stickyHeader={stickyHeaderHeight}
        reviewedFiles={reviewedFiles}
      />
    </StyledDiffWrapper>
  );
};

export default Diff;
