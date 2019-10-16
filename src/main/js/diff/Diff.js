//@flow
import React from "react";
import { translate, type TFunction } from "react-i18next";
import styled from "styled-components";
import type { Repository, Link } from "@scm-manager/ui-types";
import type {
  DiffEventContext,
  File,
  AnnotationFactoryContext
} from "@scm-manager/ui-components";
import type { PullRequest, Location, RootComment } from "../types/PullRequest";
import {
  ErrorNotification,
  Loading,
  Level,
  Button,
  LoadingDiff,
  Notification,
  diffs
} from "@scm-manager/ui-components";
import { createDiffUrl } from "../pullRequest";
import {
  createHunkId,
  createHunkIdFromLocation,
  createInlineLocation,
  isInlineLocation,
  createChangeIdFromLocation
} from "./locations";
import { fetchDiffRelatedComments } from "./fetchDiffRelatedComments";
import PullRequestComment from "../comment/PullRequestComment";
import CreateComment from "../comment/CreateComment";
import CreateCommentInlineWrapper from "./CreateCommentInlineWrapper";
import InlineComments from "./InlineComments";
import StyledDiffWrapper from "./StyledDiffWrapper";
import AddCommentButton from "./AddCommentButton";
import FileComments from "./FileComments";

const LevelWithMargin = styled(Level)`
  margin-bottom: 1rem !important;
`;

const CommentWrapper = styled.div`
  & .inline-comment + .inline-comment {
    border-top: 1px solid #dbdbdb
  }
`;

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  source: string,
  target: string,

  // context props
  classes: any,
  t: TFunction
};

type State = {
  loading: boolean,
  error?: Error,
  lines: {
    [string]: {
      [string]: {
        editor: boolean,
        location: Location,
        comments: RootComment[]
      }
    }
  },
  files: {
    [string]: {
      editor: boolean,
      comments: RootComment[]
    }
  },
  createLink: Link,
  collapsed: boolean
};

class Diff extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      files: {},
      lines: {},
      createLink: null,
      collapsed: false
    };
  }

  componentDidMount() {
    this.fetchComments();
  }

  fetchComments = () => {
    const { pullRequest } = this.props;
    if (pullRequest && pullRequest._links && pullRequest._links.comments) {
      this.setState({
        loading: true
      });

      fetchDiffRelatedComments(pullRequest._links.comments.href)
        .then(comments => {
          this.setState({
            loading: false,
            error: undefined,
            // TODO do we need to keep editor state?
            ...comments
          });
        })
        .catch(error =>
          this.setState({
            loading: false,
            error
          })
        );
    } else {
      this.setState({
        loading: false
      });
    }
  };

  render() {
    const { repository, source, target, t } = this.props;
    const { loading, error, collapsed } = this.state;
    const url = createDiffUrl(repository, source, target);

    if (!url) {
      return (
        <Notification type="danger">
          {t("scm-review-plugin.diff.notSupported")}
        </Notification>
      );
    } else if (loading) {
      return <Loading />;
    } else if (error) {
      return <ErrorNotification error={error} />;
    } else {
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
            url={url}
            defaultCollapse={collapsed}
            fileControlFactory={this.createFileControls}
            fileAnnotationFactory={this.fileAnnotationFactory}
            annotationFactory={this.annotationFactory}
            onClick={this.onGutterClick}
          />
        </StyledDiffWrapper>
      );
    }
  }

  fileAnnotationFactory = (file: File) => {
    const path = diffs.getPath(file);

    const annotations = [];
    const fileState = this.state.files[path] || [];
    if (fileState.comments && fileState.comments.length > 0) {
      annotations.push(this.createComments(fileState));
    }

    if (fileState.editor) {
      annotations.push(
        this.createNewCommentEditor({
          file: path
        })
      );
    }

    if (annotations.length > 0) {
      return <FileComments>{annotations}</FileComments>;
    }
    return [];
  };

  annotationFactory = (context: AnnotationFactoryContext) => {
    const annotations = {};

    const hunkId = createHunkId(context);
    const hunkState = this.state.lines[hunkId];
    if (hunkState) {
      Object.keys(hunkState).forEach((changeId: string) => {
        const lineState = hunkState[changeId];

        if (lineState) {
          const lineAnnotations = [];
          if (lineState.comments && lineState.comments.length > 0) {
            lineAnnotations.push(this.createComments(lineState));
          }
          if (lineState.editor) {
            lineAnnotations.push(
              this.createNewCommentEditor(lineState.location)
            );
          }
          if (lineAnnotations.length > 0) {
            annotations[changeId] = (
              <InlineComments>{lineAnnotations}</InlineComments>
            );
          }
        }
      });
    }

    return annotations;
  };

  collapseDiffs = () => {
    this.setState(state => ({
      collapsed: !state.collapsed
    }));
  };

  createFileControls = (file: File, setCollapse: boolean => void) => {
    if (this.isPermittedToComment()) {
      const openFileEditor = () => {
        const path = diffs.getPath(file);
        setCollapse(false);
        this.setFileEditor(path, true);
      };
      return <AddCommentButton action={openFileEditor} />;
    }
  };

  onGutterClick = (context: DiffEventContext) => {
    if (this.isPermittedToComment()) {
      const location = createInlineLocation(context);
      this.openEditor(location);
    }
  };

  openEditor = (location: Location) => {
    if (isInlineLocation(location)) {
      this.setLineEditor(location, true);
    } else {
      this.setFileEditor(location.file, true);
    }
  };

  closeEditor = (location: Location, callback?: () => void) => {
    if (isInlineLocation(location)) {
      this.setLineEditor(location, false, callback);
    } else {
      this.setFileEditor(location.file, false, callback);
    }
  };

  setFileEditor = (
    path: string,
    showEditor: boolean,
    callback?: () => void
  ) => {
    this.setState(state => {
      const current = state.files[path] || {};
      return {
        files: {
          ...state.files,
          [path]: {
            editor: showEditor,
            comments: current.comments || []
          }
        }
      };
    }, callback);
  };

  isPermittedToComment = () => {
    return !!this.state.createLink;
  };

  setLineEditor = (
    location: Location,
    showEditor: boolean,
    callback?: () => void
  ) => {
    const hunkId = createHunkIdFromLocation(location);
    const changeId = createChangeIdFromLocation(location);

    this.setState(state => {
      const currentHunk = state.lines[hunkId] || {};
      const currentLine = currentHunk[changeId] || {};
      return {
        lines: {
          ...state.lines,
          [hunkId]: {
            ...currentHunk,
            [changeId]: {
              editor: showEditor,
              location,
              comments: currentLine.comments || []
            }
          }
        }
      };
    }, callback);
  };

  createComments = fileState => {
    const comments = fileState.comments;

    return (
      <>
        {comments.map(rootComment => (
          <CommentWrapper className="comment-wrapper">
            <PullRequestComment
              comment={rootComment}
              refresh={this.fetchComments}
              handleError={this.onError}
              createLink={this.state.createLink}
            />
          </CommentWrapper>
        ))}
      </>
    );
  };

  createNewCommentEditor = (location: Location) => {
    if (this.state.createLink) {
      return (
        <CreateCommentInlineWrapper>
          <CreateComment
            url={this.state.createLink.href}
            location={location}
            refresh={() => this.closeEditor(location, this.fetchComments)}
            onCancel={() => this.closeEditor(location)}
            autofocus={true}
          />
        </CreateCommentInlineWrapper>
      );
    }
    return null;
  };

  onError = (error: Error) => {
    this.setState({
      error
    });
  };
}

export default translate("plugins")(Diff);
