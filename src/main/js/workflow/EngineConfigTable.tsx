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

import React, { FC } from "react";
import { EngineConfiguration } from "../types/EngineConfig";
import { useTranslation } from "react-i18next";
import { Icon } from "@scm-manager/ui-components";

type Props = {
  configuration: EngineConfiguration;
  deleteRule: (rule: string) => void;
};

const EngineConfigTable: FC<Props> = ({ configuration, deleteRule }) => {
  const [t] = useTranslation("plugins");

  return (
    <table className="card-table table is-hoverable is-fullwidth">
      <thead>
        <tr>
          <th>{t("scm-review-plugin.workflow.rule.column.name")}</th>
          <th>{t("scm-review-plugin.workflow.rule.column.description")}</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {configuration.rules?.map(rule => (
          <tr>
            <td>{t("scm-review-plugin.workflow.rule.name." + rule)}</td>
            <td>{t("scm-review-plugin.workflow.rule.description." + rule)}</td>
            <td>
              <a
                className="level-item"
                onClick={() => deleteRule(rule)}
                title={t("scm-review-plugin.workflow.deleteRule")}
              >
                <span className="icon is-small">
                  <Icon name="trash" color="inherit" />
                </span>
              </a>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default EngineConfigTable;
