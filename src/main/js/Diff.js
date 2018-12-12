//@flow
import React from 'react';
import { LoadingDiff, Notification } from "@scm-manager/ui-components";
import type {Repository} from "@scm-manager/ui-types";
import {createDiffUrl} from "./pullRequest";
import { translate } from "react-i18next";
import Loading from "@scm-manager/ui-components/src/Loading";

type Props = {
  repository: Repository,
  source: string,
  target: string,

  //context props
  t: string => string
}

type State = {
  loading: boolean,
  url?: string
};

class Diff extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true
    };
  }

  componentDidMount(): void {
    this.createUrl();
  }

  componentDidUpdate(prevProps: Props): void {
    if(prevProps.source !== this.props.source || prevProps.target !== this.props.target || prevProps.repository !== this.props.repository){
      this.setState({loading: true});
      this.createUrl();
    }
  }

  createUrl(){
    const {source, target, repository} = this.props;
    if(source && target && repository){
      const url = createDiffUrl(repository, source, target);
      this.setState({url: url, loading: false});
    }
  }

  render() {
    const { t } = this.props;
    const {loading, url} = this.state;

    console.log(url);
    if(loading){
      return <Loading/>;
    }

    if (!url) {
      return <Notification type="danger">{t("scm-review-plugin.diff.not-supported")}</Notification>;
    } else {
      return <LoadingDiff url={url} />;
    }
  }

}

export default translate("plugins")(Diff);
