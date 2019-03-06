//@flow
import React from "react";
import type { DiffEventContext } from "@scm-manager/ui-components";
import {
  AnnotationFactoryContext, ErrorNotification, Loading,
  LoadingDiff,
  Notification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import { createDiffUrl } from "../pullRequest";
import { translate } from "react-i18next";
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

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  source: string,
  target: string,

  //context props
  t: string => string
};

type State = {
  loading: boolean,
  error?: Error,
  commentLines: {
    [string]: {
      [string]: Comment[]
    }
  },
  editorLines: {
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
      commentLines: {},
      editorLines: {}
    };
  }

  componentDidMount() {
    this.fetchComments();
  }

  fetchComments = () => {
    const { pullRequest } = this.props;
    if (pullRequest._links.comments) {
      this.setState({
        loading: true
      });

      fetchComments(pullRequest._links.comments.href)
        .then(commentLines =>
          this.setState({
            loading: false,
            error: undefined,
            commentLines
          })
        )
        .catch(error =>
          this.setState({
            loading: false,
            error
          })
        );
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
        <StyledDiffWrapper>
          <LoadingDiff
            url={url}
            annotationFactory={this.annotationFactory}
            onClick={this.openEditor}
          />
        </StyledDiffWrapper>
      );
    }
  }

  annotationFactory = (context: AnnotationFactoryContext) => {
    const annotations = {};

    const hunkId = createHunkId(context);

    const commentLines = this.state.commentLines[hunkId];
    if (commentLines) {
      Object.keys(commentLines).forEach((changeId: string) => {
        const comment = commentLines[changeId];
        if (comment) {
          annotations[changeId] = this.createComments(comment);
        }
      });
    }

    const editorLines = this.state.editorLines[hunkId];
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
      {comments.map(comment => (
        <CreateCommentInlineWrapper>
          <PullRequestComment
            comment={comment}
            refresh={this.fetchComments}
            handleError={console.log}
          />
        </CreateCommentInlineWrapper>
      ))}
    </>
  );

  closeEditor = (location: Location, callback?: () => void) => {
    const hunkId = createHunkIdFromLocation(location);
    this.setState(state => {
      return {
        editorLines: {
          ...state.editorLines,
          [hunkId]: {
            ...state.editorLines[hunkId],
            [location.changeId]: false
          }
        }
      };
    }, callback);
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

  openEditor = (context: DiffEventContext) => {
    const hunkId = createHunkId(context);

    this.setState(state => {
      const hunkState = state.editorLines[hunkId] || {};

      const currentValue = hunkState[context.changeId];
      let newValue = false;
      if (!currentValue) {
        newValue = true;
      }
      return {
        editorLines: {
          [hunkId]: {
            ...state.editorLines[hunkId],
            [context.changeId]: newValue
          }
        }
      };
    });
  };
}

export default translate("plugins")(Diff);
