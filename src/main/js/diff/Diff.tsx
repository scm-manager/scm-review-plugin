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

import React, { FC, ReactNode, useEffect, useState } from "react";
import styled from "styled-components";
import { useBranch } from "@scm-manager/ui-api";
import { Hunk, Repository } from "@scm-manager/ui-types";
import {
  AnnotationFactoryContext,
  DiffEventContext,
  diffs,
  File,
  FileContentFactory
} from "@scm-manager/ui-components";
import { Comment, Comments, Location, PullRequest } from "../types/PullRequest";
import {
  createChangeIdFromLocation,
  createHunkId,
  createHunkIdFromLocation,
  createInlineLocation,
  evaluateLineNumbersForChangeId,
  isInlineLocation
} from "./locations";
import PullRequestComment from "../comment/PullRequestComment";
import CreateComment from "../comment/CreateComment";
import CommentSpacingWrapper from "../comment/CommentSpacingWrapper";
import InlineComments from "./InlineComments";
import AddCommentButton from "./AddCommentButton";
import FileComments from "./FileComments";
import MarkReviewedButton from "./MarkReviewedButton";
import { useDiffCollapseState } from "./useDiffCollapseReducer";
import ChangeNotificationToast from "../ChangeNotificationToast";
import { useInvalidateDiff } from "../pullRequest";
import { useChangeNotificationContext } from "../ChangeNotificationContext";
import LoadingDiff from "./LoadingDiff";

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
  fileContentFactory: FileContentFactory;
  reviewedFiles: string[];
};

type Revisions = {
  source: string;
  target: string;
  changed: boolean;
};

const useHasChanged = (repository: Repository, pullRequest: PullRequest) => {
  const [revisions, setRevisions] = useState<Revisions>();
  const invalidate = useInvalidateDiff(repository, pullRequest);

  const { data: source } = useBranch(repository, pullRequest.source);
  const { data: target } = useBranch(repository, pullRequest.target);

  const ctx = useChangeNotificationContext();

  const ignore = () => {
    if (revisions) {
      setRevisions({
        ...revisions,
        changed: false
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
      setRevisions(rev => ({
        source: source.revision,
        target: target.revision,
        changed: !!(rev && (rev.source !== source.revision || rev.target !== target.revision))
      }));
    }
  }, [source, target]);

  return {
    changed: revisions?.changed || false,
    ignore,
    reload
  };
};

const Diff: FC<Props> = ({
  repository,
  pullRequest,
  comments,
  diffUrl,
  createLink,
  fileContentFactory,
  reviewedFiles
}) => {
  const { actions, isCollapsed } = useDiffCollapseState(pullRequest);
  const [openEditors, setOpenEditors] = useState<{ [hunkId: string]: string[] }>({});
  const { changed, ignore, reload } = useHasChanged(repository, pullRequest);

  const openInlineEditor = (location: Location) => {
    if (isInlineLocation(location)) {
      const preUpdateOpenEditors = openEditors[createHunkIdFromLocation(location)] || [];
      setOpenEditors(prevState => ({
        ...prevState,
        [createHunkIdFromLocation(location)]: [
          ...preUpdateOpenEditors,
          !preUpdateOpenEditors.includes(createChangeIdFromLocation(location))
            ? createChangeIdFromLocation(location)
            : ""
        ]
      }));
    }
  };

  const closeEditor = (location: Location) => {
    if (isInlineLocation(location)) {
      const hunkId = createHunkIdFromLocation(location);
      const changeId = createChangeIdFromLocation(location);
      setOpenEditors(prevState => ({
        ...prevState,
        [hunkId]: [...prevState[hunkId].filter(l => l !== changeId)]
      }));
    } else {
      setOpenEditors(prevState => {
        delete prevState[location.file];
        return { ...prevState };
      });
    }
  };

  const isFileEditorOpen = (path: string) => {
    return openEditors[path];
  };

  const fileAnnotationFactory = (file: File) => {
    const path = diffs.getPath(file);
    const annotations = [];
    const fileComments: Comment[] = [];

    comments?._embedded.pullRequestComments.forEach(comment => {
      if (!isInlineLocation(comment.location) && comment.location?.file === path) {
        fileComments.push(comment);
      }
    });

    if (fileComments && fileComments.length > 0) {
      annotations.push(createComments(fileComments));
    }

    if (isFileEditorOpen(path)) {
      annotations.push(
        createNewCommentEditor({
          file: path
        })
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
      .filter(comment => comment.location?.hunk)
      .filter(
        comment =>
          comment.location?.hunk &&
          isInlineLocation(comment.location) &&
          createHunkIdFromLocation(comment.location) === hunkId
      )
      .forEach(comment => {
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
    Object.keys(commentsByLine).forEach(changeId => {
      const createdComments = createComments(commentsByLine[changeId]);
      lineAnnotations[changeId] = [createdComments];
    });

    const editors = openEditors[hunkId] || [];

    editors.forEach(changeId => {
      let line = lineAnnotations[changeId];
      if (!line) {
        line = [];
        lineAnnotations[changeId] = line;
      }
      const lineNumbers = evaluateLineNumbersForChangeId(changeId);
      line.push(
        createNewCommentEditor({
          file: diffs.getPath(context.file),
          hunk: context.hunk.content,
          ...lineNumbers
        })
      );
    });

    Object.keys(lineAnnotations).forEach(changeId => {
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
        setOpenEditors(prevState => ({ ...prevState, [path]: [] }));
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
      const location = createInlineLocation(context);
      openInlineEditor(location);
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
            />
          </CommentWrapper>
        ))}
      </>
    );
  };

  const createNewCommentEditor = (location: Location) => {
    if (createLink) {
      return (
        <CommentSpacingWrapper>
          <CreateComment
            repository={repository}
            pullRequest={pullRequest}
            url={createLink}
            location={location}
            onCancel={() => closeEditor(location)}
            autofocus={true}
          />
        </CommentSpacingWrapper>
      );
    }
    return null;
  };

  return (
    <>
      {changed ? <ChangeNotificationToast reload={reload} ignore={ignore} /> : null}
      <LoadingDiff
        isCollapsed={isCollapsed}
        onCollapseStateChange={actions.toggleCollapse}
        fileControlFactory={createFileControlsFactory(fileContentFactory)}
        fileAnnotationFactory={fileAnnotationFactory}
        annotationFactory={annotationFactory}
        onClick={onGutterClick}
        refetchOnWindowFocus={false}
        hunkGutterHoverIcon={isPermittedToComment() ? "\\f075" : ""}
        highlightLineOnHover={isPermittedToComment()}
        diffUrl={diffUrl}
        actions={actions}
        pullRequestComments={comments?._embedded.pullRequestComments || []}
      />
    </>
  );
};

export default Diff;
