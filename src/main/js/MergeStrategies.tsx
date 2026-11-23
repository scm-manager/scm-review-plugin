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
import { RadioGroup } from "@scm-manager/ui-core";

type Props = {
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  strategyLinks: Link[];
};

type InnerProps = Props & {
  innerRef: React.Ref<HTMLButtonElement>;
};

const MergeStrategies: FC<InnerProps> = ({ strategyLinks, selectedStrategy, selectStrategy, innerRef }) => {
  const [t] = useTranslation("plugins");

  return (
    <RadioGroup value={selectedStrategy} onValueChange={selectStrategy}>
      {strategyLinks &&
        strategyLinks.map((link, index) => (
          <RadioGroup.Option
            key={link.name}
            value={link.name ?? ""}
            label={t(`scm-review-plugin.showPullRequest.mergeStrategies.${link.name}`)}
            helpText={t(`scm-review-plugin.showPullRequest.mergeStrategies.help.${link.name}`)}
            ref={index === 0 ? innerRef : undefined}
          />
        ))}
    </RadioGroup>
  );
};

export default React.forwardRef<HTMLButtonElement, Props>((props, ref) => (
  <MergeStrategies {...props} innerRef={ref} />
));
