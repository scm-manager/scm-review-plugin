import React from "react";
import {WithTranslation, withTranslation} from "react-i18next";
import {Link} from "@scm-manager/ui-types";
import {Radio} from "@scm-manager/ui-components";
import styled from "styled-components";

type Props = WithTranslation & {
  selectStrategy: (strategy: string) => void;
  selectedStrategy: string;
  strategyLinks: Link[];
};

const RadioList = styled.div`
  display: flex;
  flex-direction: column;
  > label:not(:last-child) {
    margin-bottom: 0.6em;
  }
  > :first-child {
    margin-left: 0.5em;
  }
`;

class MergeStrategies extends React.Component<Props> {
  isSelected = (strategyLink: String) => {
    return this.props.selectedStrategy === strategyLink;
  };

  render() {
    const { strategyLinks, selectStrategy, t } = this.props;
    return (
      <>
        <RadioList>
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
