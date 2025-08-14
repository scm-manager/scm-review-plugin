/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React, { FC, useEffect, useState } from "react";
import { InputField } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";

type Configuration = {
  numberOfReviewers: number;
};

type Props = {
  configurationChanged: (newRuleConfiguration: Configuration, valid: boolean) => void;
};

const ApprovedByXReviewersRuleConfiguration: FC<Props> = ({ configurationChanged }) => {
  const [t] = useTranslation("plugins");
  const [numberOfReviewers, setNumberOfReviewers] = useState<string | undefined>();
  const [validationError, setValidationError] = useState(false);

  const onValueChange = (val: string) => {
    setNumberOfReviewers(val);
    const numberVal = parseInt(val);
    if (!isNaN(numberVal) && numberVal > 0) {
      setValidationError(false);
      configurationChanged({ numberOfReviewers: numberVal }, true);
    } else {
      setValidationError(true);
      configurationChanged({ numberOfReviewers: 0 }, false);
    }
  };

  useEffect(() => configurationChanged({ numberOfReviewers: 0 }, false), []);

  return (
    <InputField
      type="number"
      value={numberOfReviewers}
      label={t("workflow.rule.ApprovedByXReviewersRule.form.numberOfReviewers.label")}
      helpText={t("workflow.rule.ApprovedByXReviewersRule.form.numberOfReviewers.helpText")}
      validationError={validationError}
      errorMessage={t("workflow.rule.ApprovedByXReviewersRule.form.numberOfReviewers.errorMessage")}
      autofocus={true}
      onChange={onValueChange}
    />
  );
};

export default ApprovedByXReviewersRuleConfiguration;
