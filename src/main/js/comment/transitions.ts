import {Comment, Transition} from "../types/PullRequest";

export const findLatestTransition = (comment: Comment, type: string) => {
  if (comment._embedded && comment._embedded.transitions) {
    const latestTransitions = comment._embedded.transitions.filter((t: Transition) => t.transition === type);
    if (latestTransitions.length > 0) {
      return latestTransitions[latestTransitions.length - 1];
    }
  }
};
