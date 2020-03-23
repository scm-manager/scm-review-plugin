///
/// MIT License
///
/// Copyright (c) 2020-present Cloudogu GmbH and Contributors
///
/// Permission is hereby granted, free of charge, to any person obtaining a copy
/// of this software and associated documentation files (the "Software"), to deal
/// in the Software without restriction, including without limitation the rights
/// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
/// copies of the Software, and to permit persons to whom the Software is
/// furnished to do so, subject to the following conditions:
///
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
///
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
/// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
/// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
/// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
/// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
/// SOFTWARE.
///

import { useEffect, useState, Dispatch } from "react";
import { Comments, PullRequest } from "../types/PullRequest";
import { Link, Links } from "@scm-manager/ui-types";
import { getPullRequestComments } from "../pullRequest";
import { fetchAll } from "./actiontypes";

type Response = {
  loading: boolean;
  error?: Error;
  links?: Links;
};

const useComments = (pullRequest: PullRequest, dispatch: Dispatch<any>): Response => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [links, setLinks] = useState();

  useEffect(() => {
    const url = pullRequest && pullRequest._links && (pullRequest._links.comments as Link).href;
    if (url) {
      getPullRequestComments(url)
        .then((commentResponse: Comments) => {
          let comments = [];
          if (commentResponse && commentResponse._embedded) {
            comments = commentResponse._embedded.pullRequestComments;
          }
          setLinks(commentResponse._links);
          setLoading(false);
          dispatch(fetchAll(comments));
        })
        .catch(error => {
          setLoading(false);
          setError(error);
        });
    } else {
      setLoading(false);
    }
  }, [dispatch, pullRequest]);

  return {
    loading,
    error,
    links
  };
};

export default useComments;
