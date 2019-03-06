//@flow
import React from "react";
import type { DiffEventContext } from "@scm-manager/ui-components";
import {
  AnnotationFactoryContext, ErrorNotification, Loading,
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
import AddCommentButton from './AddCommentButton';
import FileComments from './FileComments';

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
  fileComments: {
    [string]: Comment[]
  },
  lineComments: {
    [string]: {
      [string]: Comment[]
    }
  },
  fileCommentEditors: {
    [string]: boolean
  },
  lineCommentEditors: {
    [string]: {
      [string]: boolean
    }
  }
};

class Diff extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      fileComments: {},
      lineComments: {},
      fileCommentEditors: {},
      lineCommentEditors: {}
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
      })
    }
  };

  createFileControls = (file: File) => {
    const openFileEditor = () => {
      const path = diffs.getPath(file);
      this.setState(state => {
        return {
          fileCommentEditors: {
            ...state.fileCommentEditors,
            [path]: true
          }
        };

      });
    };
    return <AddCommentButton action={openFileEditor} />;
  };

  fileAnnotationFactory = (file: File) => {
    const path = diffs.getPath(file);

    const annotations = [];

    const fileComments = this.state.fileComments[path];
    if (fileComments) {
      annotations.push(this.createComments(fileComments));
    }

    if (this.state.fileCommentEditors[path]) {
      annotations.push(this.createNewCommentEditor({
        file: path
      }));
    }

    if (annotations.length > 0) {
      return <FileComments>{ annotations }</FileComments>;
    }
    return [];
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
        <StyledDiffWrapper>
          <LoadingDiff
            url={url}
            annotationFactory={this.annotationFactory}
            onClick={this.onGutterClick}
            fileControlFactory={this.createFileControls}
            fileAnnotationFactory={this.fileAnnotationFactory}
          />
        </StyledDiffWrapper>
      );
    }
  }

  annotationFactory = (context: AnnotationFactoryContext) => {
    const annotations = {};

    const hunkId = createHunkId(context);

    const commentLines = this.state.lineComments[hunkId];
    if (commentLines) {
      Object.keys(commentLines).forEach((changeId: string) => {
        const comment = commentLines[changeId];
        if (comment) {
          annotations[changeId] = this.createComments(comment);
        }
      });
    }

    const editorLines = this.state.lineCommentEditors[hunkId];
    if (editorLines) {
      Object.keys(editorLines).forEach((changeId: string) => {
        if (editorLines[changeId]) {
          const location = createLocation(context, changeId);

          if (annotations[changeId]) {
            annotations[changeId] = [
              annotations[changeId],
              this.createNewCommentEditor(location)
            ];
          } else {
            annotations[changeId] = this.createNewCommentEditor(location);
          }
        }
      });
    }

    const wrappedAnnotations = {};
    Object.keys(annotations).forEach((changeId: string) => {
      wrappedAnnotations[changeId] = (
        <InlineComments>{annotations[changeId]}</InlineComments>
      );
    });
    return wrappedAnnotations;
  };

  createComments = (comments: Comment[]) => (
    <>
      {comments.map((comment, index) => (
        <CreateCommentInlineWrapper>
          <PullRequestComment
            comment={comment}
            refresh={this.fetchComments}
            onReply={index === comments.length - 1 ? this.reply : undefined}
            handleError={console.log}
          />
        </CreateCommentInlineWrapper>
      ))}
    </>
  );

  reply = (comment: Comment) => {
    const location = comment.location;
    if (location) {
      this.openEditor(location);
    }
  };

  closeEditor = (location: Location, callback?: () => void) => {
    if (location.hunk && location.changeId) {
      const hunkId = createHunkIdFromLocation(location);
      this.setState(state => {
        return {
          lineCommentEditors: {
            ...state.lineCommentEditors,
            [hunkId]: {
              ...state.lineCommentEditors[hunkId],
              [location.changeId]: false
            }
          }
        };
      }, callback);
    } else {
      this.setState(state => {
        return {
          fileCommentEditors: {
            ...state.fileCommentEditors,
            [location.file]: false
          }
        };
      }, callback);
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
            handleError={console.log}
          />
        </CreateCommentInlineWrapper>
      );
    }
    return null;
  };

  onGutterClick = (context: DiffEventContext) => {
    const location = createLocation(context, context.changeId);
    this.openEditor(location);
  };

  openEditor = (location: Location) => {
    const changeId = location.changeId;
    if (location.hunk && changeId) {
      const hunkId = createHunkIdFromLocation(location);
      this.setState(state => {
        const hunkState = state.lineCommentEditors[hunkId] || {};

        const currentValue = hunkState[changeId];
        let newValue = false;
        if (!currentValue) {
          newValue = true;
        }
        return {
          lineCommentEditors: {
            [hunkId]: {
              ...state.lineCommentEditors[hunkId],
              [changeId]: newValue
            }
          }
        };
      });
    } else {
      this.setState(state => {
        return {
          fileCommentEditors: {
            ...state.fileCommentEditors,
            [location.file]: true
          }
        };
      });
    }
  };
}

export default translate("plugins")(Diff);
