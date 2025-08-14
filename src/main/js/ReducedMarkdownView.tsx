/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React, { FC } from "react";
import { AstPlugin, MarkdownView } from "@scm-manager/ui-components";
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
  plugins?: AstPlugin[];
};

const ReducedMarkdownView: FC<Props> = ({ content, plugins = [] }) => {
  return (
    <MarkdownWrapper>
      <MarkdownView content={content} mdastPlugins={plugins} />
    </MarkdownWrapper>
  );
};

export default ReducedMarkdownView;
