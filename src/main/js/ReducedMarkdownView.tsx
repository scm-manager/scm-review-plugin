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
