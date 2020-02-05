import React from "react";
import { WithTranslation, withTranslation } from "react-i18next";
import { Icon, Level, InputField, AddButton, Notification } from "@scm-manager/ui-components";
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

const StyledLevel = styled(Level)`
  align-items: stretch;
  margin-bottom: 1rem !important; // same margin as field
`;

const FullWidthInputField = styled(InputField)`
  width: 100%;
  margin-right: 1.5rem;
`;

const FlexEndField = styled.div.attrs(() => ({
  className: "field"
}))`
  align-self: flex-end;
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
    const { branches, t } = this.props;
    const { newBranch } = this.state;
    const table =
      branches.length === 0 ? (
        <Notification type={"info"}>{t("scm-review-plugin.config.noBranches")}</Notification>
      ) : (
        <table className="card-table table is-hoverable is-fullwidth">
          <thead>
            <tr>
              <th>{t("scm-review-plugin.config.newBranch.pattern")}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {branches.map(branch => (
              <tr>
                <td>{branch}</td>
                <VCenteredTd className="is-darker">
                  <a
                    className="level-item"
                    onClick={() => this.deleteBranch(branch)}
                    title={t("scm-review-plugin.config.deleteBranch")}
                  >
                    <span className="icon is-small">
                      <Icon name="trash" color="inherit" />
                    </span>
                  </a>
                </VCenteredTd>
              </tr>
            ))}
          </tbody>
        </table>
      );

    return (
      <>
        {table}
        <StyledLevel
          children={
            <FullWidthInputField
              label={t("scm-review-plugin.config.newBranch.label")}
              onChange={this.handleNewBranchChange}
              value={newBranch}
              helpText={t("scm-review-plugin.config.newBranch.helpText")}
            />
          }
          right={
            <FlexEndField>
              <AddButton
                title={t("scm-review-plugin.config.newBranch.add.helpText")}
                label={t("scm-review-plugin.config.newBranch.add.label")}
                action={this.addBranch}
                disabled={newBranch.trim().length === 0}
              />
            </FlexEndField>
          }
        />
      </>
    );
  }
}

export default withTranslation("plugins")(BranchList);
