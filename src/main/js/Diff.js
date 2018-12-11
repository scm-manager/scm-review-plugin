//@flow
import React from 'react';
import type {Repository} from "@scm-manager/ui-types";

type Props = {
  repository: Repository,
  source: string,
  target: string
}

class Diff extends React.Component<Props> {

  render() {
    return (
      <div>
        <h1>Diff</h1>
      </div>
    );
  }

}

export default Diff;
