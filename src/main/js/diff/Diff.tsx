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
import React, { Dispatch } from "react";
import { WithTranslation, withTranslation } from "react-i18next";
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
import { createHunkId, createInlineLocation } from "./locations";
import PullRequestComment from "../comment/PullRequestComment";
import CreateComment from "../comment/CreateComment";
import CommentSpacingWrapper from "../comment/CommentSpacingWrapper";
import InlineComments from "./InlineComments";
import StyledDiffWrapper from "./StyledDiffWrapper";
import AddCommentButton from "./AddCommentButton";
import FileComments from "./FileComments";
import { DiffAction, markAsReviewed, State as DiffState, unmarkAsReviewed } from "./reducer";
import { closeEditor, createComment, openEditor } from "../comment/actiontypes";
import MarkReviewedButton from "./MarkReviewedButton";

const LevelWithMargin = styled(Level)`
  margin-bottom: 1rem !important;
`;

const CommentWrapper = styled.div`
  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb;
  }
`;

type Props = WithTranslation & {
  diffUrl: string;
  diffState: DiffState;
  createLink?: string;
  dispatch: Dispatch<DiffAction>;
  pullRequest: PullRequest;
  fileContentFactory: FileContentFactory;
};

type State = {
  collapsed: boolean;
  explicitlyOpenedFiles: string[];
};

class Diff extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      collapsed: false,
      explicitlyOpenedFiles: []
    };
  }

  shouldComponentUpdate(nextProps: Readonly<Props>, nextState: Readonly<State>) {
    return this.state.collapsed !== nextState.collapsed;
  }

  render() {
    const { diffUrl, fileContentFactory, diffState, t } = this.props;
    const { collapsed } = this.state;

    let globalCollapsedOrByMarks: DefaultCollapsedFunction;
    if (collapsed) {
      globalCollapsedOrByMarks = (oldPath, newPath) => !this.hasOpenEditor(oldPath, newPath);
    } else {
      globalCollapsedOrByMarks = (oldPath: string, newPath: string) =>
        diffState ? diffState.reviewedFiles.some(path => path === oldPath || path === newPath) : false;
    }

    return (
      <StyledDiffWrapper commentable={this.isPermittedToComment()}>
        <LevelWithMargin
          right={
            <Button
              action={this.collapseDiffs}
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
          fileControlFactory={this.createFileControlsFactory(fileContentFactory)}
          fileAnnotationFactory={this.fileAnnotationFactory}
          annotationFactory={this.annotationFactory}
          onClick={this.onGutterClick}
          hunkClass={hunk => (hunk.expansion ? "expanded" : "commentable")}
        />
      </StyledDiffWrapper>
    );
  }

  fileAnnotationFactory = (file: File) => {
    const path = diffs.getPath(file);

    const annotations = [];
    const fileState = this.props.diffState.files[path] || [];
    if (fileState.comments && fileState.comments.length > 0) {
      annotations.push(this.createComments(fileState.comments));
    }

    if (fileState.editor) {
      annotations.push(
        this.createNewCommentEditor({
          file: path
        })
      );
    }

    if (annotations.length > 0) {
      return [<FileComments>{annotations}</FileComments>];
    }
    return [];
  };

  hasOpenEditor = (oldPath: string, newPath: string) => {
    const explicitlyOpenedFiles = this.state.explicitlyOpenedFiles;
    const path = newPath === "/dev/null" ? oldPath : newPath;
    const fileState = this.props.diffState.files[path] || [];
    if (explicitlyOpenedFiles.find(o => o === path)) {
      return true;
    }

    if (!!fileState.editor) {
      this.markAsExplicitlyOpened(path);
      return true;
    }

    const lineEditor = !!Object.values(this.props.diffState.lines)
      .flatMap(line => Object.values(line))
      .filter(line => line.location.file === path)
      .find(line => line.editor);
    if (lineEditor) {
      this.markAsExplicitlyOpened(path);
    }
    return lineEditor;
  };

  markAsExplicitlyOpened = (file: string) => {
    this.setState({ explicitlyOpenedFiles: [...this.state.explicitlyOpenedFiles, file] });
  };

  annotationFactory = (context: AnnotationFactoryContext) => {
    const annotations: { [key: string]: React.ReactNode } = {};

    const hunkId = createHunkId(context);
    const hunkState = this.props.diffState.lines[hunkId];
    if (hunkState) {
      Object.keys(hunkState).forEach((changeId: string) => {
        const lineState = hunkState[changeId];

        if (lineState) {
          const lineAnnotations = [];
          if (lineState.comments && lineState.comments.length > 0) {
            lineAnnotations.push(this.createComments(lineState.comments));
          }
          if (lineState.editor) {
            lineAnnotations.push(this.createNewCommentEditor(lineState.location));
          }
          if (lineAnnotations.length > 0) {
            annotations[changeId] = <InlineComments>{lineAnnotations}</InlineComments>;
          }
        }
      });
    }

    return annotations;
  };

  collapseDiffs = () => {
    this.setState(state => ({
      collapsed: !state.collapsed,
      explicitlyOpenedFiles: []
    }));
  };

  createFileControlsFactory = (fileContentFactory: FileContentFactory) => (
    file: File,
    setCollapse: (p: boolean) => void
  ) => {
    const { pullRequest, diffState, dispatch } = this.props;

    const setReviewMark = (filepath: string, reviewed: boolean) => {
      if (reviewed) {
        dispatch(markAsReviewed(filepath));
      } else {
        dispatch(unmarkAsReviewed(filepath));
      }
      setCollapse(reviewed);
    };

    if (this.isPermittedToComment()) {
      const openFileEditor = () => {
        const path = diffs.getPath(file);
        setCollapse(false);
        this.openEditor({
          file: path
        });
      };
      return (
        <ButtonGroup>
          <MarkReviewedButton
            pullRequest={pullRequest}
            newPath={file.newPath}
            oldPath={file.oldPath}
            setReviewed={setReviewMark}
            diffState={diffState}
          />
          <AddCommentButton action={openFileEditor} />
          {fileContentFactory(file)}
        </ButtonGroup>
      );
    } else {
      return <ButtonGroup>{fileContentFactory(file)}</ButtonGroup>;
    }
  };

  onGutterClick = (context: DiffEventContext) => {
    if (this.isPermittedToComment() && !context.hunk.expansion) {
      const location = createInlineLocation(context);
      this.openEditor(location);
    }
  };

  openEditor = (location: Location) => {
    const { dispatch } = this.props;
    dispatch(openEditor(location));
  };

  closeEditor = (location: Location) => {
    const { dispatch } = this.props;
    dispatch(closeEditor(location));
  };

  isPermittedToComment = () => {
    return !!this.props.createLink;
  };

  findComment = (id: string): Comment => {
    const { diffState } = this.props;
    const comment = diffState.comments[id];
    if (!comment) {
      throw new Error("could not find comment with id " + id);
    }
    return comment;
  };

  createComments = (commentIds: string[]) => {
    const { createLink, dispatch } = this.props;
    return (
      <>
        {commentIds.map((commentId: string) => (
          <CommentWrapper key={commentId} className="comment-wrapper">
            <PullRequestComment comment={this.findComment(commentId)} createLink={createLink} dispatch={dispatch} />
          </CommentWrapper>
        ))}
      </>
    );
  };

  onCreation = (location: Location, comment: Comment) => {
    const { dispatch } = this.props;
    this.closeEditor(location);
    dispatch(createComment(comment));
  };

  createNewCommentEditor = (location: Location) => {
    if (this.props.createLink) {
      return (
        <CommentSpacingWrapper>
          <CreateComment
            url={this.props.createLink}
            location={location}
            onCreation={comment => this.onCreation(location, comment)}
            onCancel={() => this.closeEditor(location)}
            autofocus={true}
          />
        </CommentSpacingWrapper>
      );
    }
    return null;
  };
}

export default withTranslation("plugins")(Diff);
