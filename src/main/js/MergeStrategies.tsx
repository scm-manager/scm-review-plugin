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
import { Link } from "@scm-manager/ui-types";
import { Radio } from "@scm-manager/ui-components";
import styled from "styled-components";

type Props = WithTranslation & {
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  strategyLinks: Link[];
};

const RadioList = styled.div`
  > label:not(:last-child) {
    margin-bottom: 0.75rem;
  }
`;

class MergeStrategies extends React.Component<Props> {
  isSelected = (strategyLink: string) => {
    return this.props.selectedStrategy === strategyLink;
  };

  render() {
    const { strategyLinks, selectStrategy, t } = this.props;
    return (
      <>
        <RadioList className="is-flex is-flex-direction-column">
          {strategyLinks &&
            strategyLinks.map(link => {
              return (
                <Radio
                  name={link.name}
                  value={link.href}
                  checked={this.isSelected(link.name || "")}
                  onChange={() => selectStrategy(link.name || "")}
                  label={t(`scm-review-plugin.showPullRequest.mergeStrategies.${link.name}`)}
                  helpText={t(`scm-review-plugin.showPullRequest.mergeStrategies.help.${link.name}`)}
                />
              );
            })}
        </RadioList>
      </>
    );
  }
}

export default withTranslation("plugins")(MergeStrategies);
