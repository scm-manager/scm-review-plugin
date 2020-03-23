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
import { apiClient, Autocomplete, InputField, TagGroup, Textarea } from "@scm-manager/ui-components";
import { WithTranslation, withTranslation } from "react-i18next";
import { DisplayedUser, SelectValue, AutocompleteObject } from "@scm-manager/ui-types";

type Props = WithTranslation & {
  handleFormChange: (value: any, name: string) => void;
  title?: string;
  description: string;
  reviewer: DisplayedUser[];
  userAutocompleteLink: string;
};

type State = {
  title: string | undefined;
  reviewer: DisplayedUser[];
  description: string;
  selectedValue?: SelectValue;
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

  onChange = (value: string | DisplayedUser[], name?: string) => {
    if (!name) {
      throw new Error("name is required");
    }
    this.setState({ ...this.state, [name]: value });
    this.props.handleFormChange(value, name);
  };

  loadUserSuggestions = (inputValue: string) => {
    return this.loadAutocompletion(this.props.userAutocompleteLink, inputValue);
  };

  loadAutocompletion = (url: string, inputValue: string) => {
    const link = url + "?q=";
    return apiClient
      .get(link + inputValue)
      .then(response => response.json())
      .then(json => {
        return json.map((element: AutocompleteObject) => {
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
        displayName: selection.value.displayName,
        mail: ""
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
          validationError={title === ""}
          errorMessage={t("scm-review-plugin.pullRequest.validation.title")}
          onChange={this.onChange}
        />
        <Textarea
          name="description"
          value={description}
          label={t("scm-review-plugin.pullRequest.description")}
          onChange={this.onChange}
        />
        <TagGroup items={reviewer} label={t("scm-review-plugin.pullRequest.reviewer")} onRemove={this.removeReviewer} />
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
