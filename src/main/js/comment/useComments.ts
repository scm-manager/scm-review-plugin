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
