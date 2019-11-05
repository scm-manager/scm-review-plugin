import React from "react";
import { InputField, Textarea, TagGroup, Autocomplete } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { SelectValue, DisplayedUser } from "@scm-manager/ui-types";

type Props = WithTranslation & {
  handleFormChange: (value: string, name: string) => void;
  title: string;
  description: string;
  reviewer: DisplayedUser[];
  userAutocompleteLink: string;
};

type State = {
  title: string;
  reviewer: DisplayedUser[];
  description: string;
  selectedValue: SelectValue;
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
    this.setState({
      [name]: value
    });
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
          const label = element.displayName ? `${element.displayName} (${element.id})` : element.id;
          return {
            value: element,
            label
          };
        });
      });
  };

  removeReviewer = (reviewer: DisplayedUser[]) => {
    this.onChange(reviewer, "reviewer");
  };

  selectName = (selection: SelectValue) => {
    if (
      this.state.reviewer &&
      (this.state.reviewer.length === 0 ||
        this.state.reviewer.filter(value => value.id === selection.value.id).length === 0)
    ) {
      this.state.reviewer.push({
        id: selection.value.id,
        displayName: selection.value.displayName
      });
      this.props.handleFormChange(this.state.reviewer, "reviewer");
    }
  };

  render() {
    const { t } = this.props;
    const { title, description, reviewer } = this.state;
    return (
      <>
        <InputField
          name="title"
          value={title}
          label={t("scm-review-plugin.pullRequest.title")}
          onChange={this.onChange}
        />
        <Textarea
          name="description"
          value={description}
          label={t("scm-review-plugin.pullRequest.description")}
          onChange={this.onChange}
        />
        <TagGroup
          items={reviewer}
          label={t("scm-review-plugin.pullRequest.reviewer")}
          onRemove={this.removeReviewer}
        />
        <div className="field">
          <div className="control">
            <Autocomplete
              creatable={false}
              loadSuggestions={this.loadUserSuggestions}
              valueSelected={this.selectName}
              placeholder={t("scm-review-plugin.pullRequest.addReviewer")}
            />
          </div>
        </div>
      </>
    );
  }
}
export default withTranslation("plugins")(EditForm);
