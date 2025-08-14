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

import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import styled from "styled-components";

type Props = {
  labels: string[];
};

const Label = styled.div.attrs(() => ({
  className: "field-label is-inline-flex"
}))`
  text-align: left;
  margin-right: 0;
  min-width: 5.5em;
`;

const Field = styled.div.attrs(() => ({
  className: "field-body is-inline-flex"
}))`
  flex-grow: 8;
`;

const LabelsList: FC<Props> = ({ labels }) => {
  const [t] = useTranslation("plugins");

  return labels?.length > 0 ? (
    <div className="field is-horizontal">
      <Label>{t("scm-review-plugin.pullRequest.labels")}:</Label>
      <Field>
        <ul className="is-separated">
          {labels.map(label => (
            <li className="is-inline-block has-text-weight-bold" key={label}>
              {label}
            </li>
          ))}
        </ul>
      </Field>
    </div>
  ) : null;
};

export default LabelsList;
