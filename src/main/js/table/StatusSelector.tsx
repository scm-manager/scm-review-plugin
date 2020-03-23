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
import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Select } from "@scm-manager/ui-components";

type Props = WithTranslation & {
  handleTypeChange: (p: string) => void;
  status: string;
  label?: string;
  helpText?: string;
  loading?: boolean;
};

class StatusSelector extends React.Component<Props> {
  render() {
    const { status, handleTypeChange, loading, label, helpText } = this.props;
    const types = ["OPEN", "MINE", "REVIEWER", "ALL", "REJECTED", "MERGED"];

    return (
      <Select
        onChange={handleTypeChange}
        value={status ? status : "OPEN"}
        options={this.createSelectOptions(types)}
        loading={loading}
        label={label}
        helpText={helpText}
      />
    );
  }

  createSelectOptions(status: string[]) {
    const { t } = this.props;
    return status.map(singleStatus => {
      return {
        label: t(`scm-review-plugin.pullRequest.selector.${singleStatus}`),
        value: singleStatus
      };
    });
  }
}

export default withTranslation("plugins")(StatusSelector);
