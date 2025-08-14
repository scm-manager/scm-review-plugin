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
import { Link } from "@scm-manager/ui-types";
import { Radio } from "@scm-manager/ui-components";
import styled from "styled-components";

type Props = {
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  strategyLinks: Link[];
};

type InnerProps = Props & {
  innerRef: React.Ref<HTMLInputElement>;
};

const RadioList = styled.div`
  > label:not(:last-child) {
    margin-bottom: 0.75rem;
  }
`;

const MergeStrategies: FC<InnerProps> = ({ strategyLinks, selectedStrategy, selectStrategy, innerRef }) => {
  const [t] = useTranslation("plugins");

  const isSelected = (strategyLink: string) => selectedStrategy === strategyLink;

  return (
    <>
      <RadioList className="is-flex is-flex-direction-column">
        {strategyLinks &&
          strategyLinks.map((link, index) =>
            index === 0 ? (
              <Radio
                name={link.name}
                value={link.href}
                checked={isSelected(link.name ?? "")}
                onChange={() => selectStrategy(link.name ?? "")}
                label={t(`scm-review-plugin.showPullRequest.mergeStrategies.${link.name}`)}
                helpText={t(`scm-review-plugin.showPullRequest.mergeStrategies.help.${link.name}`)}
                ref={innerRef}
              />
            ) : (
              <Radio
                name={link.name}
                value={link.href}
                checked={isSelected(link.name ?? "")}
                onChange={() => selectStrategy(link.name ?? "")}
                label={t(`scm-review-plugin.showPullRequest.mergeStrategies.${link.name}`)}
                helpText={t(`scm-review-plugin.showPullRequest.mergeStrategies.help.${link.name}`)}
              />
            )
          )}
      </RadioList>
    </>
  );
};

export default React.forwardRef<HTMLInputElement, Props>((props, ref) => <MergeStrategies {...props} innerRef={ref} />);
