import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Button, Icon, InputField } from "@scm-manager/ui-components";
import styled from "styled-components";

type Props = WithTranslation & {
  branches: string[];
  onChange: (branches: string[]) => void;
};

type State = {
  newBranch: string;
};

const VCenteredTd = styled.td`
  display: table-cell;
  vertical-align: middle !important;
`;

class BranchList extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { newBranch: "" };
  }

  handleNewBranchChange = (newBranch: string) => {
    this.setState({ newBranch });
  };

  addBranch = () => {
    this.props.onChange([...this.props.branches, this.state.newBranch]);
    this.setState({ newBranch: "" });
  };

  deleteBranch = (branchToDelete: string) => {
    this.props.onChange([...this.props.branches.filter(branch => branch !== branchToDelete)]);
  };

  render() {
    const { t } = this.props;
    const { newBranch } = this.state;
    return (
      <>
        <table className="card-table table is-hoverable is-fullwidth">
          {this.props.branches.map(branch => (
            <tr>
              <td>{branch}</td>
              <VCenteredTd className="is-darker">
                <a
                  className="level-item"
                  onClick={() => this.deleteBranch(branch)}
                  title={t("scm-review-plugin.config.deleteBranch.label")}
                >
                  <span className="icon is-small">
                    <Icon name="trash" color="inherit" />
                  </span>
                </a>
              </VCenteredTd>
            </tr>
          ))}
        </table>
        <InputField
          label={t("scm-review-plugin.config.newBranch.label")}
          onChange={this.handleNewBranchChange}
          value={newBranch}
          helpText={t("scm-review-plugin.config.newBranch.helpText")}
        />
        <Button
          title={t("scm-review-plugin.config.newBranch.add.helpText")}
          label={t("scm-review-plugin.config.newBranch.add.label")}
          action={this.addBranch}
          disabled={newBranch.trim().length === 0}
        />
      </>
    );
  }
}

export default withTranslation("plugins")(BranchList);
