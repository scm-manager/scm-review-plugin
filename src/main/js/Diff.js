//@flow
import React from "react";
import type { Change, DiffEventContext } from "@scm-manager/ui-components";
import {
  AnnotationFactoryContext,
  diffs,
  LoadingDiff,
  Notification
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import { createDiffUrl } from "./pullRequest";
import { translate } from "react-i18next";
import type { Comments, PullRequest } from "./types/PullRequest";
import PullRequestComment from "./comment/PullRequestComment";
import CreateComment from "./comment/CreateComment";

type Props = {
  repository: Repository,
  source: string,
  target: string,

  //context props
  t: string => string
};

type State = {
  lineComments: {
    [string]: {
      [string]: Comments
    }
  },
  editorLines: {
    [string]: {
      [string]: boolean
    }
  }
};

const comments = {
  _links: {
    self: {
      href:
        "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/"
    },
    create: {
      href:
        "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/"
    }
  },
  _embedded: {
    pullRequestComments: [
      {
        comment: "Neuer Kommentar",
        author: "scmadmin",
        date: "2019-02-27T18:04:21.100Z",
        _links: {
          self: {
            href:
              "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/3GRJInWMa1"
          },
          update: {
            href:
              "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/3GRJInWMa1"
          },
          delete: {
            href:
              "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/3GRJInWMa1"
          }
        }
      },
      {
        comment: "Noch ein Kommentar, dieses Mal aber von Tricia",
        author: "tricia",
        date: "2019-02-27T20:05:21.100Z",
        _links: {
          self: {
            href:
              "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/3GRJInWMa1"
          },
          update: {
            href:
              "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/3GRJInWMa1"
          },
          delete: {
            href:
              "http://localhost:8081/scm/api/v2/pull-requests/scmadmin/bat/1/comments/3GRJInWMa1"
          }
        }
      }
    ]
  }
};
class Diff extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      lineComments: {
        "modify_src/app.rs_@@ -100,7 +100,9 @@": { N100: comments }
      },
      editorLines: {}
    };
  }

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
        <LoadingDiff
          url={url}
          annotationFactory={this.annotationFactory}
          onClick={this.diffEventHandler}
        />
      );
    }
  }

  annotationFactory = (context: AnnotationFactoryContext) => {
    const createHunkId = diffs.createHunkIdentifierFromContext(context);
    return context.hunk.changes.reduce((widgets, change) => {
      const changeId = this.createChangeId(change);
      const comments = this.getComments(createHunkId, changeId);

      if (comments) {
        const rawComments = comments._embedded.pullRequestComments;
        return {
          ...widgets,
          [changeId]: (
            <>
              {rawComments.map(comment => {
                return <PullRequestComment comment={comment} />;
              })}
              {this.renderNewCommentEditor()}
            </>
          )
        };
      } else if (this.lineEditorOpen(createHunkId, changeId)) {
        return { ...widgets, [changeId]: this.renderNewCommentEditor() };
      }
      return widgets;
    }, {});
  };

  renderNewCommentEditor = () => { // TODO: Incorporate File/Line
    return (
      <CreateComment url={"foo"} refresh={() => {}} handleError={() => {}} />
    );
  };

  getComments = (hunkId: string, changeId: string) => {
    if (this.state.lineComments[hunkId]) {
      return this.state.lineComments[hunkId][changeId];
    }
  };

  lineEditorOpen = (hunkId: string, changeId: string) => {
    if (this.state.editorLines[hunkId]) {
      return this.state.editorLines[hunkId][changeId];
    }
  };

  createChangeId = (change: Change) => {
    switch (change.type) {
      case "delete":
        return "D" + change.lineNumber;
      case "insert":
        return "I" + change.lineNumber;
      case "normal":
        return "N" + change.oldLineNumber;
      default:
        return "";
    }
  };

  diffEventHandler = (context: DiffEventContext) => {
    const hunkId = diffs.createHunkIdentifierFromContext(context);

    this.setState(state => {
      const hunkState = state.editorLines[hunkId] || {};

      const currentValue = hunkState[context.changeId];
      let newValue = false;
      if (!currentValue) {
        // newValue = this.createAnnotation(context);
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
