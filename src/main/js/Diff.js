//@flow
import React from "react";
import { LoadingDiff, Notification } from "@scm-manager/ui-components";
import type {Repository} from "@scm-manager/ui-types";
import {createDiffUrl} from "./pullRequest";
import { translate } from "react-i18next";

type Props = {
  repository: Repository,
  source: string,
  target: string,

  //context props
  t: string => string
}

class Diff extends React.Component<Props> {

  render() {
    const { repository, source, target, t } = this.props;
    const url = createDiffUrl(repository, source, target);

    if (!url) {
      return <Notification type="danger">{t("scm-review-plugin.diff.not-supported")}</Notification>;
    } else {
      return <LoadingDiff url={url} />;
    }
  }

}

export default translate("plugins")(Diff);
