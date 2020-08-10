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
import React, {FC, useReducer} from "react";
import reducer, {initialState} from "./reducer";
import {PullRequest} from "../types/PullRequest";
import {Link, Repository} from "@scm-manager/ui-types";
import {ErrorNotification, JumpToFileButton, Loading, Notification} from "@scm-manager/ui-components";
import useComments from "../comment/useComments";
import {createDiffUrl} from "../pullRequest";
import {useTranslation} from "react-i18next";
import Diff from "./Diff";

type Props = {
  repository: Repository;
  pullRequest: PullRequest;
  source: string;
  target: string;
};

const DiffRoute: FC<Props> = ({repository, pullRequest, source, target}) => {
  const [comments, dispatch] = useReducer(reducer, initialState);
  const {error, loading, links} = useComments(pullRequest, dispatch);
  const {t} = useTranslation("plugins");

  const fileContentFactory: FileContentFactory = (file) => {
    const baseUrl = `/repo/${repository.namespace}/${repository.name}/code/sources`;
    const sourceLink = {
      url: `${baseUrl}/${pullRequest && pullRequest.source && encodeURIComponent(pullRequest.source)}/${file.newPath}/`,
      label: t("scm-review-plugin.diff.jumpToSource")
    };
    const targetLink = pullRequest.target && {
      url: `${baseUrl}/${encodeURIComponent(pullRequest.target)}/${file.oldPath}`,
      label: t("scm-review-plugin.diff.jumpToTarget")
    };

    const links = [];
    switch (file.type) {
      case "add":
        links.push(sourceLink);
        break;
      case "delete":
        if (targetLink) {
          links.push(targetLink);
        }
        break;
      default:
        if (targetLink) {
          links.push(sourceLink, targetLink);
        } else {
          links.push(sourceLink);
        }
    }

    return links.map(({url, label}) => <JumpToFileButton
      tooltip={label}
      link={url}
    />);
  };

  const diffUrl = createDiffUrl(repository, source, target);
  if (!diffUrl) {
    return <Notification type="danger">{t("scm-review-plugin.diff.notSupported")}</Notification>;
  } else if (loading) {
    return <Loading/>;
  } else if (error) {
    return <ErrorNotification error={error}/>;
  } else {
    const createLink = links && links.create ? (links.create as Link).href : undefined;
    return (
      <Diff
        diffUrl={diffUrl}
        comments={comments}
        createLink={createLink}
        dispatch={dispatch}
        pullRequest={pullRequest}
        fileContentFactory={fileContentFactory}
      />
    );
  }
};

export default DiffRoute;
