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

import React, { FC, useEffect, useState } from "react";
import { PullRequest } from "../types/PullRequest";
import { apiClient, ErrorNotification, Icon } from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import classNames from "classnames";
import { Result } from "../types/EngineConfig";
import StatusModalView from "./StatusModalView";

type Props = {
  pullRequest: PullRequest;
};

const AngleRight = styled.i`
  margin: 0 0.5rem;
`;

const Notification = styled.div`
  margin-top: 1rem;
  margin-bottom: 0 !important;
  padding: 1rem 1.25rem;
  line-height: 1;
  border-top: none !important;
`;

const PaddingRightIcon = styled(Icon)`
  padding-right: 0.5rem;
`;

const LoadingText = styled.span`
  margin-left: 0.5rem;
`;

const LoadingStatusBar = () => {
  const [t] = useTranslation("plugins");
  return (
    <Notification className={classNames("media", "notification is-grey-lighter")}>
      <Icon className="fa-lg fa-spin" name="circle-notch" />
      <LoadingText className="has-text-weight-bold">{t("scm-review-plugin.workflow.statusbar.loading")}</LoadingText>
    </Notification>
  );
};

const workflowLink = (pullRequest: PullRequest) => {
  const link = pullRequest._links.workflowResult;
  if (link) {
    return (link as Link).href;
  }
};

const Statusbar: FC<Props> = ({ pullRequest }) => {
  const [t] = useTranslation("plugins");
  const [error, setError] = useState<undefined | Error>(undefined);
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState<Result[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const workflowResultHref = workflowLink(pullRequest);

  useEffect(() => {
    // we need to check the link existence inside the hook,
    // because hooks must be used unconditionally
    if (workflowResultHref) {
      apiClient
        .get(workflowResultHref)
        .then(r => r.json())
        .then(r => {
          setResult(r.results);
          setLoading(false);
        })
        .catch(err => {
          setError(err);
          setLoading(false);
        });
    }
  }, [workflowResultHref]);

  // engine not enabled or no rules defined
  if (!workflowResultHref) {
    return null;
  }

  if (loading) {
    return <LoadingStatusBar />;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  const failedRules = result?.filter(r => r.failed).length;
  const failed = failedRules > 0;
  const color = failed ? "warning" : "success";
  const icon = failed ? "exclamation-triangle" : "check-circle";

  const toggleModal = () => {
    setModalOpen(!modalOpen);
  };

  return (
    <>
      {modalOpen && <StatusModalView onClose={toggleModal} result={result} failed={failed} />}
      <Notification
        className={classNames("media", `notification is-grey-lighter`, "has-cursor-pointer")}
        onClick={() => toggleModal()}
      >
        <PaddingRightIcon className="fa-lg" color={color} name={icon} />
        <span className="has-text-weight-bold">
          {t("scm-review-plugin.workflow.statusbar.rules", {
            count: result && result.length
          })}
        </span>
        <AngleRight className="fas fa-angle-right" />
        <span>
          {failedRules === 0
            ? t("scm-review-plugin.workflow.statusbar.noFailedRules")
            : t("scm-review-plugin.workflow.statusbar.failedRules", {
                count: failedRules
              })}
        </span>
      </Notification>
    </>
  );
};

export default Statusbar;
