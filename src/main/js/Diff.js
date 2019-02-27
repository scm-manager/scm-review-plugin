//@flow
import React from "react";
import {
  LoadingDiff,
  diffs,
  Notification,
  AnnotationFactoryContext
} from "@scm-manager/ui-components";
import type { Repository } from "@scm-manager/ui-types";
import {createDiffUrl} from "./pullRequest";
import { translate } from "react-i18next";
import type {DiffEventContext} from "@scm-manager/ui-components";
import CreateComment from "./comment/CreateComment";
import type {Comments} from "./types/PullRequest";

type Props = {
  repository: Repository,
  source: string,
  target: string,

  //context props
  t: string => string
};

type State = {
  pullRequestComments?: Comments,
  [string]: {
    [string]: any
  }
}

class Diff extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {};
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
        <LoadingDiff url={url} annotationFactory={this.annotationFactory} onClick={this.diffEventHandler} />
      );
    }
  }

  annotationFactory = (context: AnnotationFactoryContext) => {
    const createHunkId = diffs.createHunkIdentifierFromContext(context);
    return this.state[createHunkId];
  };

  createAnnotation = (context: DiffEventContext) => {
    return (
      <div style={{ border: "2px solid red", padding: "1rem" }}>
        <h4>Hello from {context.changeId}</h4>
        <div>Comment #1</div>
        <CreateComment url={"create"} handleError={(error: Error) => {console.log(error)}}/> // TODO: Implement properly
      </div>
    );
  };


  diffEventHandler = (context: DiffEventContext) => {
    const hunkId = diffs.createHunkIdentifierFromContext(context);

    this.setState(state => {
      const hunkState = state[hunkId] || {};

      const currentValue = hunkState[context.changeId];
      let newValue = undefined;
      if (!currentValue) {
        newValue = this.createAnnotation(context);
      }
      return {
        [hunkId]: {
          ...state[hunkId],
          [context.changeId]: newValue
        }
      };
    });
  }
}

export default translate("plugins")(Diff);
