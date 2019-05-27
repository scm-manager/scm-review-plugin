//@flow
import React from "react";
import type { DiffEventContext } from "@scm-manager/ui-components";
import {
  AnnotationFactoryContext,
  ErrorNotification,
  Loading,
  LoadingDiff,
  Notification,
  diffs
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import { createDiffUrl } from "../pullRequest";
import { translate, type TFunction } from "react-i18next";
import type { Comment, PullRequest, Location } from "../types/PullRequest";
import CreateComment from "../comment/CreateComment";
import CreateCommentInlineWrapper from "./CreateCommentInlineWrapper";
import PullRequestComment from "../comment/PullRequestComment";
import InlineComments from "./InlineComments";
import StyledDiffWrapper from "./StyledDiffWrapper";
import {
  createHunkId,
  createHunkIdFromLocation,
  createLocation
} from "./locations";
import { fetchComments } from "./fetchComments";
import AddCommentButton from "./AddCommentButton";
import FileComments from "./FileComments";

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  source: string,
  target: string,

  //context props
  t: TFunction
};

type State = {
  loading: boolean,
  error?: Error,
  lines: {
    [string]: {
      [string]: {
        editor: boolean,
        comments: Comment[]
      }
    }
  },
  files: {
    [string]: {
      editor: boolean,
      parentId: string,
      comments: Comment[]
    }
  }
};

class Diff extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      files: {},
      lines: {}
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

      fetchComments(pullRequest._links.comments.href)
        .then(comments =>
          this.setState({
            loading: false,
            error: undefined,
            // TODO do we need to keep editor state?
            ...comments
          })
        )
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
    const { loading, error } = this.state;
    const url = createDiffUrl(repository, source, target);

    if (!url) {
      return (
        <Notification type="danger">
          {t("scm-review-plugin.diff.not-supported")}
        </Notification>
      );
    } else if (loading) {
      return <Loading />;
    } else if (error) {
      return <ErrorNotification error={error} />;
    } else {
      return (
        <StyledDiffWrapper commentable={this.isPermittedToComment()}>
          <LoadingDiff
            url={url}
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
    const location = { file: file.newPath };

    const annotations = [];
    const fileState = this.state.files[path] || [];
    if (fileState.comments && fileState.comments.length > 0) {
      annotations.push(this.createComments(fileState, location));
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
        const location = createLocation(context, changeId);

        if (lineState) {
          const lineAnnotations = [];
          if (lineState.comments && lineState.comments.length > 0) {
            lineAnnotations.push(this.createComments(lineState, location));
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

  createFileControls = (file: File, setCollapse: boolean => void) => {
    if (this.isPermittedToComment()) {
      const openFileEditor = () => {
        const path = diffs.getPath(file);
        setCollapse(false);
        this.setFileEditor(path, true, null, null);
      };
      return <AddCommentButton action={openFileEditor} />;
    }
  };

  onGutterClick = (context: DiffEventContext) => {
    if (this.isPermittedToComment()) {
      const location = createLocation(context, context.changeId);
      this.openEditor(location);
    }
  };

  reply = (comment: Comment) => {
    const location = comment.location;
    if (location) {
      this.openEditor(location, comment.id);
    }
  };

  openEditor = (location: Location, parentId?: string) => {
    const changeId = location.changeId;
    if (location.hunk && changeId) {
      this.setLineEditor(location, true, parentId, null);
    } else {
      this.setFileEditor(location.file, true, parentId, null);
    }
  };

  closeEditor = (location: Location, callback?: () => void) => {
    if (location.hunk && location.changeId) {
      this.setLineEditor(location, false, null, callback);
    } else {
      this.setFileEditor(location.file, false, null, callback);
    }
  };

  setFileEditor = (
    path: string,
    showEditor: boolean,
    parentId: string,
    callback?: () => void
  ) => {
    this.setState(state => {
      const current = state.files[path] || {};
      return {
        files: {
          ...state.files,
          [path]: {
            editor: showEditor,
            parentId: parentId,
            comments: current.comments || []
          }
        }
      };
    }, callback);
  };

  isPermittedToComment = () => {
    const { pullRequest } = this.props;
    return (
      pullRequest && pullRequest._links && !!pullRequest._links.createComment
    );
  };

  setLineEditor = (
    location: Location,
    showEditor: boolean,
    parentId: string,
    callback?: () => void
  ) => {
    const hunkId = createHunkIdFromLocation(location);
    const changeId = location.changeId;
    if (!changeId) {
      throw new Error("invalid state change id is required");
    }

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
              parentId: parentId,
              comments: currentLine.comments || []
            }
          }
        }
      };
    }, callback);
  };

  createComments = (fileState, location) => {
    const comments = fileState.comments;
    const onReply = (isReplyable: boolean) => {
      if (isReplyable && this.isPermittedToComment()) {
        return this.reply;
      }
    };

    return (
      <>
        {comments.map
        (rootComment => (
          <div className="comment-wrapper">
            <CreateCommentInlineWrapper>
              <PullRequestComment
                comment={rootComment}
                refresh={this.fetchComments}
                onReply={onReply(!!rootComment._links.reply)}
                handleError={this.onError}
              />
            </CreateCommentInlineWrapper>
            rootComment.replies.map(childComment => (
            <CreateCommentInlineWrapper isChildComment={true}>
              <PullRequestComment
                comment={rootComment}
                refresh={this.fetchComments}
                onReply={onReply(rootComment.replies.length === index + 1)}
                handleError={this.onError}
              />
            </CreateCommentInlineWrapper>
            ){this.createReplyEditorIfNeeded(filestate, rootComment)}
          </div>
        ))}
      </>
    );
  };

  createReplyEditorIfNeeded = (fileState, rootComment) => {
    if (!!fileState.editor && fileState.parentId === rootComment.id) {
      return this.createNewReplyEditor(rootComment);
    }
  };

  createNewCommentEditor = (location: Location) => {
    const { pullRequest } = this.props;
    if (pullRequest._links.createComment) {
      return (
        <CreateCommentInlineWrapper>
          <CreateComment
            url={pullRequest._links.createComment.href}
            location={location}
            refresh={() => this.closeEditor(location, this.fetchComments)}
            onCancel={() => this.closeEditor(location)}
            autofocus={true}
            handleError={this.onError}
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

  createNewReplyEditor(rootComment) {
    return (
      <CreateCommentInlineWrapper>
        <CreateComment
          url={rootComment._links.reply.href}
          location={rootComment.location}
          refresh={() => this.closeEditor(location, this.fetchComments)}
          onCancel={() => this.closeEditor(location)}
          autofocus={true}
          handleError={this.onError}
        />
      </CreateCommentInlineWrapper>
    );
  }
}

export default translate("plugins")(Diff);
