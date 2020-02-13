import React, { FC } from "react";
import { MarkdownView } from "@scm-manager/ui-components";
import styled from "styled-components";

const MarkdownWrapper = styled.div`
  .content {
    > h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
      margin: 0.5rem 0 !important;
      font-size: 0.9rem !important;
    }
    > h1 {
      font-weight: 700;
    }
    > h2 {
      font-weight: 600;
    }
    > h3,
    h4,
    h5,
    h6 {
      font-weight: 500;
    }
    & strong {
      font-weight: 500;
    }
  }
`;

type Props = {
  content: string;
};

const ReducedMarkdownView: FC<Props> = ({ content }) => {
  return (
    <MarkdownWrapper>
      <MarkdownView content={content} />
    </MarkdownWrapper>
  );
};

export default ReducedMarkdownView;
