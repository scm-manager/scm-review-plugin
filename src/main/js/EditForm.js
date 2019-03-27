// @flow
import React from "react";
import { InputField, Autocomplete, Textarea } from "@scm-manager/ui-components";
import { translate } from "react-i18next";
import type { DisplayedUser } from "./types/PullRequest";
import type { SelectValue } from "@scm-manager/ui-types";

type Props = {
  handleFormChange: (value: string, name: string) => void,
  title: string,
  description: string,
  reviewer: DisplayedUser[],
  userAutocompleteLink: string,
  // Context props
  t: string => string
};

type State = {
  title: string,
  reviewer: DisplayedUser[],
  description: string,
  selectedValue: SelectValue
};

class EditForm extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      title: this.props.title,
      description: this.props.description,
      reviewer: this.props.reviewer
    };
  }

  onChange = (value: string, name: string) => {
    this.setState({ [name]: value });
    this.props.handleFormChange(value, name);
  };

  loadUserSuggestions = (inputValue: string) => {
    return this.loadAutocompletion(this.props.userAutocompleteLink, inputValue);
  };

  loadAutocompletion = (url: string, inputValue: string) => {
    const link = url + "?q=";
    return fetch(link + inputValue)
      .then(response => response.json())
      .then(json => {
        return json.map(element => {
          const label = element.displayName
            ? `${element.displayName} (${element.id})`
            : element.id;
          return {
            value: element,
            label
          };
        });
      });
  };

  selectName = (selection: SelectValue) => {
    let reviewer = this.state.reviewer;
    reviewer.push({
      id: selection.value.id,
      displayName: selection.value.displayName
    });
    this.setState({
      ...this.state,
      reviewer,
      selectedValue: selection
    });
  };

  removeReviewer(reviewer) {
    console.log("reviewer: ", reviewer);
  }

  render() {
    const { t } = this.props;
    const { title, description, reviewer } = this.state;
    return (
      <>
        <InputField
          name="title"
          value={title}
          label={t("scm-review-plugin.pull-request.title")}
          onChange={this.onChange}
        />
        <Textarea
          name="description"
          value={description}
          label={t("scm-review-plugin.pull-request.description")}
          onChange={this.onChange}
        />
        <div className="field is-grouped is-grouped-multiline">
          {reviewer ? (
            <div className="control">
              {t("scm-review-plugin.pull-request.reviewer")} :
            </div>
          ) : (
            ""
          )}
          {reviewer.map(reviewer => {
            return (
              <div className="control">
                <div className="tags has-addons">
                  <span className="tag is-info">{reviewer.displayName}</span>
                  <a className="tag is-delete" onClick={this.removeReviewer(reviewer)} />
                </div>
              </div>
            );
          })}
        </div>
        <div className="field">
          <div className="control">
            <Autocomplete
              creatable={false}
              loadSuggestions={this.loadUserSuggestions}
              valueSelected={this.selectName}
              placeholder={t("scm-review-plugin.pull-request.addReviewer")}
            />
          </div>
        </div>
      </>
     );
  }
}
export default translate("plugins")(EditForm);
