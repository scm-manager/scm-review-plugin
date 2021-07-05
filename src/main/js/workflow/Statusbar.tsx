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

import React, { FC, useState } from "react";
import { PullRequest } from "../types/PullRequest";
import { ErrorNotification, Icon } from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import styled from "styled-components";
import { useTranslation } from "react-i18next";
import classNames from "classnames";
import StatusModalView from "./StatusModalView";
import { useStatusbar } from "./statusbar";

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

const Statusbar: FC<Props> = ({ pullRequest }) => {
  const [t] = useTranslation("plugins");
  const { data, error, isLoading } = useStatusbar((pullRequest._links.workflowResult as Link).href);
  const [modalOpen, setModalOpen] = useState(false);

  // no workflow rules configured
  if (isLoading || !data || data?.results?.length === 0) {
    return null;
  }

  if (error) {
    return <ErrorNotification error={error} />;
  }

  const failedRules = data.results.filter(r => r.failed).length;
  const failed = failedRules > 0;
  const color = failed ? "warning" : "success";
  const icon = failed ? "exclamation-triangle" : "check-circle";

  const toggleModal = () => {
    setModalOpen(!modalOpen);
  };

  return (
    <>
      {modalOpen && <StatusModalView onClose={toggleModal} result={data.results} failed={failed} />}
      <Notification
        className={classNames("media", `notification is-grey-lighter`, "has-cursor-pointer")}
        onClick={() => toggleModal()}
      >
        <PaddingRightIcon className="fa-lg" color={color} name={icon} />
        <span className="has-text-weight-bold">
          {t("scm-review-plugin.workflow.statusbar.rules", {
            count: data?.results && data.results.length
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
