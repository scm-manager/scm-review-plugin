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
import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import {
  AnnotationFactoryContext,
  Button,
  ButtonGroup,
  DefaultCollapsedFunction,
  DiffEventContext,
  diffs,
  File,
  FileContentFactory,
  Level,
  LoadingDiff
} from "@scm-manager/ui-components";
import { Comment, Location, PullRequest } from "../types/PullRequest";
import {
  createChangeIdFromLocation,
  createHunkId,
  createHunkIdFromLocation,
  createInlineLocation,
  isInlineLocation
} from "./locations";
import PullRequestComment from "../comment/PullRequestComment";
import CreateComment from "../comment/CreateComment";
import CommentSpacingWrapper from "../comment/CommentSpacingWrapper";
import InlineComments from "./InlineComments";
import StyledDiffWrapper from "./StyledDiffWrapper";
import AddCommentButton from "./AddCommentButton";
import FileComments from "./FileComments";
import MarkReviewedButton from "./MarkReviewedButton";
import { Repository } from "@scm-manager/ui-types";

const LevelWithMargin = styled(Level)`
  margin-bottom: 1rem !important;
`;

const CommentWrapper = styled.div`
  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb;
  }
`;

export type FileCommentState = {
  comments: string[];
  editor?: boolean;
};

export type FileCommentCollection = {
  // path
  [key: string]: FileCommentState;
};
export type LineCommentCollection = {
  // hunkid
  [key: string]: {
    // changeid
    [key: string]: {
      location: Location;
      comments: string[];
      editor?: boolean;
    };
  };
};

export type DiffState = {
  files: FileCommentCollection;
  lines: LineCommentCollection;
  comments: Comment[];
  reviewedFiles: string[];
};

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  diffUrl: string;
  diffState: DiffState;
  updateDiffState: (state: DiffState) => void;
  createLink?: string;
  fileContentFactory: FileContentFactory;
};

const openEditor = (
  diffState: DiffState,
  updateDiffState: (state: DiffState) => void,
  editor: boolean,
  location?: Location
) => {
  if (isInlineLocation(location)) {
    const lineComments = diffState.lines;
    const hunkId = createHunkIdFromLocation(location!);
    const changeId = createChangeIdFromLocation(location!);
    const hunkComments = lineComments[hunkId] || {};
    const changeComments = hunkComments[changeId] || {
      location,
      comments: []
    };

    changeComments.editor = editor;
    hunkComments[changeId] = changeComments;
    lineComments[hunkId] = hunkComments;
    updateDiffState({ ...diffState, lines: lineComments });
  } else {
    const fileComments = diffState.files;
    const file = location!.file;
    const fileState = fileComments[file] || {
      comments: []
    };
    fileState.editor = editor;
    fileComments[file] = fileState;
    updateDiffState({ ...diffState, files: fileComments });
  }
};

const Diff: FC<Props> = ({
  repository,
  pullRequest,
  diffUrl,
  diffState,
  updateDiffState,
  createLink,
  fileContentFactory
}) => {
  const [t] = useTranslation("plugins");
  const [collapsed, setCollapsed] = useState(false);
  const [explicitlyOpenedFiles, setExplicitlyOpenedFiles] = useState<string[]>([]);

  let globalCollapsedOrByMarks: DefaultCollapsedFunction = (oldPath: string, newPath: string) =>
    collapsed
      ? !hasOpenEditor(oldPath, newPath)
      : !hasOpenEditor(oldPath, newPath) &&
        diffState.reviewedFiles.some((path: string) => path === oldPath || path === newPath);

  const fileAnnotationFactory = (file: File) => {
    const path = diffs.getPath(file);

    const annotations = [];
    const fileState = diffState?.files[path] || [];
    if (fileState.comments && fileState.comments.length > 0) {
      annotations.push(createComments(fileState.comments));
    }

    if (fileState.editor) {
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

  const hasOpenEditor = (oldPath: string, newPath: string) => {
    const path = newPath === "/dev/null" ? oldPath : newPath;
    const fileState = diffState.files[path] || [];
    if (explicitlyOpenedFiles.find(o => o === path)) {
      return true;
    }

    if (!!fileState.editor) {
      markAsExplicitlyOpened(path);
      return true;
    }

    const lineEditor = !!Object.values(diffState.lines)
      .flatMap(line => Object.values(line))
      .filter(line => line.location.file === path)
      .find(line => line.editor);
    if (lineEditor) {
      markAsExplicitlyOpened(path);
    }
    return lineEditor;
  };

  const markAsExplicitlyOpened = (file: string) => {
    setExplicitlyOpenedFiles([...explicitlyOpenedFiles, file]);
  };

  const annotationFactory = (context: AnnotationFactoryContext) => {
    const annotations: { [key: string]: React.ReactNode } = {};

    const hunkId = createHunkId(context);
    const hunkState = diffState.lines[hunkId];
    if (hunkState) {
      Object.keys(hunkState).forEach((changeId: string) => {
        const lineState = hunkState[changeId];
        if (lineState) {
          const lineAnnotations = [];
          if (lineState.comments && lineState.comments.length > 0) {
            lineAnnotations.push(createComments(lineState.comments));
          }
          if (lineState.editor) {
            lineAnnotations.push(createNewCommentEditor(lineState.location));
          }
          if (lineAnnotations.length > 0) {
            annotations[changeId] = <InlineComments>{lineAnnotations}</InlineComments>;
          }
        }
      });
    }

    return annotations;
  };

  const collapseDiffs = () => {
    setCollapsed(!collapsed);
    setExplicitlyOpenedFiles([]);
  };

  const createFileControlsFactory = (contentFactory: FileContentFactory) => (
    file: File,
    setCollapse: (p: boolean) => void
  ) => {
    const setReviewMark = (filepath: string, reviewed: boolean) => {
      if (reviewed) {
        updateDiffState({ ...diffState, reviewedFiles: [...diffState.reviewedFiles, filepath] });
      } else {
        updateDiffState({ ...diffState, reviewedFiles: [...diffState.reviewedFiles.filter(f => f !== filepath)] });
      }
    };

    if (isPermittedToComment()) {
      const openFileEditor = () => {
        const path = diffs.getPath(file);
        setCollapse(false);
        openEditor(diffState, updateDiffState, true, { file: path });
      };
      return (
        <ButtonGroup>
          <MarkReviewedButton
            repository={repository}
            pullRequest={pullRequest}
            newPath={file.newPath}
            oldPath={file.oldPath}
            setReviewed={setReviewMark}
            diffState={diffState}
          />
          <AddCommentButton action={openFileEditor} />
          {contentFactory(file)}
        </ButtonGroup>
      );
    } else {
      return <ButtonGroup>{contentFactory(file)}</ButtonGroup>;
    }
  };

  const onGutterClick = (context: DiffEventContext) => {
    if (isPermittedToComment() && !context.hunk.expansion) {
      const location = createInlineLocation(context);
      openEditor(diffState, updateDiffState, true, location);
    }
  };

  const isPermittedToComment = () => {
    return !!createLink;
  };

  const findComment = (id: string): Comment => {
    const comment = diffState?.comments.find(c => c.id === id);
    if (!comment) {
      throw new Error("could not find comment with id " + id);
    }
    return comment;
  };

  const createComments = (commentIds: string[]) => {
    return (
      <>
        {commentIds.map((commentId: string) => (
          <CommentWrapper key={commentId} className="comment-wrapper">
            <PullRequestComment
              repository={repository}
              pullRequest={pullRequest}
              comment={findComment(commentId)}
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
            onCancel={() => openEditor(diffState, updateDiffState, false, location)}
            autofocus={true}
          />
        </CommentSpacingWrapper>
      );
    }
    return null;
  };

  return (
    <StyledDiffWrapper commentable={isPermittedToComment()}>
      <LevelWithMargin
        right={
          <Button
            action={collapseDiffs}
            color="default"
            icon={collapsed ? "eye" : "eye-slash"}
            label={t("scm-review-plugin.diff.collapseDiffs")}
            reducedMobile={true}
          />
        }
      />
      <LoadingDiff
        url={diffUrl}
        defaultCollapse={globalCollapsedOrByMarks}
        fileControlFactory={createFileControlsFactory(fileContentFactory)}
        fileAnnotationFactory={fileAnnotationFactory}
        annotationFactory={annotationFactory}
        onClick={onGutterClick}
        hunkClass={hunk => (hunk.expansion ? "expanded" : "commentable")}
      />
    </StyledDiffWrapper>
  );
};

export default Diff;
