//@flow
import React from "react";
import type {BaseContext, DiffEventContext} from "@scm-manager/ui-components";
import {
  AnnotationFactoryContext,
  LoadingDiff,
  Notification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import {createDiffUrl, getPullRequestComments} from "./pullRequest";
import { translate } from "react-i18next";
import type { Comment, PullRequest, Location } from "./types/PullRequest";
import CreateComment from "./comment/CreateComment";
import CreateCommentInlineWrapper from "./comment/CreateCommentInlineWrapper";
import {getPath} from "@scm-manager/ui-components/src/repos/diffs";
import PullRequestComment from "./comment/PullRequestComment";
import InlineComments from "./comment/InlineComments";
import StyledDiffWrapper from './StyledDiffWrapper';

type Props = {
  repository: Repository,
  pullRequest: PullRequest,
  source: string,
  target: string,

  //context props
  t: string => string
};

type State = {
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
      getPullRequestComments(pullRequest._links.comments.href)
        .then(comments => comments._embedded.pullRequestComments.filter((comment) => !!comment.location))
        .then(comments => {
          const commentLines = {};

          comments.forEach((comment) => {

            const hunkId = this.createHunkIdFromLocation(comment.location);
            const commentsByChangeId = commentLines[hunkId] || {};

            const commentsForChangeId = commentsByChangeId[comment.location.changeId] || [];
            commentsForChangeId.push( comment );
            commentsByChangeId[comment.location.changeId] = commentsForChangeId;

            commentLines[hunkId] = commentsByChangeId;
          });

          this.setState({
            commentLines
          })
        })
        .catch(err => {
          console.log(err);
        });
    }
  };

  render() {
    const { repository, source, target, t } = this.props;
    const url = createDiffUrl(repository, source, target);

    if (!url) {
      return (
        <Notification type="danger">
          {t("scm-review-plugin.diff.not-supported")}
        </Notification>
      );
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

    const hunkId = this.createHunkId(context);


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
          const location = this.createLocation(context, changeId);

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
      wrappedAnnotations[ changeId ] = (<InlineComments>{annotations[changeId]}</InlineComments>);
    });
    return wrappedAnnotations;
  };

  createComments = (comments: Comment[]) => (
    <>
      {comments.map((comment) => (
        <CreateCommentInlineWrapper>
          <PullRequestComment comment={comment} refresh={this.fetchComments} handleError={console.log} />
        </CreateCommentInlineWrapper>)
      )}
    </>
  );

  createHunkId(context: BaseContext): string {
    return getPath(context.file) + "_" + context.hunk.content;
  }

  createHunkIdFromLocation(location: Location): string {
    return location.file + "_" + location.hunk;
  }

  createLocation(context: BaseContext, changeId: string): Location {
    return {
      file: getPath(context.file),
      hunk: context.hunk.content,
      changeId
    };
  }

  closeEditor = (location: Location, callback: () => void) => {
    const hunkId = this.createHunkIdFromLocation(location);

    this.setState((state) => {
      return {
        editorLines: {
          ...state.editorLines,
          [hunkId]: {
            ...state.editorLines[hunkId],
            [location.changeId]: false
          }
        }
      }
    }, callback);
  };

  createNewCommentEditor = (location: Location) => {
    const { pullRequest } = this.props;
    if (pullRequest._links.createComment){


      const onSubmit = () => {
        this.closeEditor(location, this.fetchComments);
      };

      return (
        <CreateCommentInlineWrapper>
          <CreateComment url={pullRequest._links.createComment.href} location={location} refresh={onSubmit} handleError={console.log} />
        </CreateCommentInlineWrapper>
      );
    }
    return null;
  };

  openEditor = (context: DiffEventContext) => {
    const hunkId = this.createHunkId(context);

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
