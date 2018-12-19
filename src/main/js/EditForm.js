// @flow
import React from "react";
import {
  InputField,
  Textarea
} from "@scm-manager/ui-components";
import { translate } from "react-i18next";


type Props = {
  handleFormChange: (value: string, name: string) => void,
  title: string,
  description: string,

  // Context props
  t: string => string
};

type State = {
  title: string,
  description: string
};

class EditForm extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      title : this.props.title,
      description : this.props.description
    };
  };

  onChange = (value: string, name: string) => {
    this.setState({[name]: value });
    this.props.handleFormChange(value, name);
  };

  render() {
    const {t} = this.props;
    const {title, description} = this.state;
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
        </>
     );
  }
}
export default translate("plugins")(EditForm);
