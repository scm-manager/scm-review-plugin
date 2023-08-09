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
import React, { FC, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Select } from "@scm-manager/ui-forms";
import { useGeneratedId } from "@scm-manager/ui-components";

type Props = {
  handleTypeChange: (p: string) => void;
  status: string;
  className?: string;
};

const StatusSelector: FC<Props> = ({ handleTypeChange, status, className }) => {
  const [t] = useTranslation("plugins");
  const types = ["IN_PROGRESS", "MINE", "REVIEWER", "ALL", "REJECTED", "MERGED"];
  const selectOptions = useMemo(
    () =>
      types.map(singleStatus => ({
        label: t(`scm-review-plugin.pullRequest.selector.${singleStatus}`),
        value: singleStatus
      })),
    [t, types]
  );
  const selectId = useGeneratedId();

  return (
    <span className="is-flex">
      <label className="is-flex is-align-items-center mr-2" htmlFor={selectId}>
        {t("scm-review-plugin.pullRequest.selectorLabel")}
      </label>
      <Select
        id={selectId}
        onChange={e => handleTypeChange(e.target.value)}
        value={status ? status : "IN_PROGRESS"}
        options={selectOptions}
        className={className}
      />
    </span>
  );
};

export default StatusSelector;
