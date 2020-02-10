import React, { FC, useReducer } from "react";
import reducer, { initialState } from "./reducer";
import { PullRequest } from "../types/PullRequest";
import { Repository, Link } from "@scm-manager/ui-types";
import { Notification, Loading, ErrorNotification } from "@scm-manager/ui-components";
import useComments from "../comment/useComments";
import { createDiffUrl } from "../pullRequest";
import { useTranslation } from "react-i18next";
import Diff from "./Diff";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  source: string;
  target: string;
};

const DiffRoute: FC<Props> = ({ repository, pullRequest, source, target }) => {
  const [comments, dispatch] = useReducer(reducer, initialState);
  const { error, loading, links } = useComments(pullRequest, dispatch);
  const { t } = useTranslation("plugins");

  const diffUrl = createDiffUrl(repository, source, target);
  if (!diffUrl) {
    return <Notification type="danger">{t("scm-review-plugin.diff.notSupported")}</Notification>;
  } else if (loading) {
    return <Loading />;
  } else if (error) {
    return <ErrorNotification error={error} />;
  } else {
    const createLink = links && links.create ? (links.create as Link).href : undefined;
    return (
      <Diff
        diffUrl={diffUrl}
        comments={comments}
        createLink={createLink}
        dispatch={dispatch}
        pullRequest={pullRequest}
      />
    );
  }
};

export default DiffRoute;
