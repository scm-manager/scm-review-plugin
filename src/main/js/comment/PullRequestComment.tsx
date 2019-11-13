import React, { Dispatch } from "react";
import classNames from "classnames";
import { WithTranslation, withTranslation } from "react-i18next";
import styled from "styled-components";
import { confirmAlert, DateFromNow, ErrorNotification, Loading, Textarea, apiClient } from "@scm-manager/ui-components";
import { Link } from "@scm-manager/ui-types";
import { BasicComment, Comment, PossibleTransition } from "../types/PullRequest";
import { deletePullRequestComment, transformPullRequestComment, updatePullRequestComment } from "../pullRequest";
import CommentSpacingWrapper from "./CommentSpacingWrapper";
import CommentActionToolbar from "./CommentActionToolbar";
import CommentTags from "./CommentTags";
import CommentContent from "./CommentContent";
import CommentMetadata from "./CommentMetadata";
import ContextModal from "./ContextModal";
import LastEdited from "./LastEdited";
import EditButtons from "./EditButtons";
import Replies from "./Replies";
import ReplyEditor from "./ReplyEditor";
import { createReply, deleteComment, deleteReply, updateComment, updateReply } from "./actiontypes";

const LinkWithInheritColor = styled.a`
  color: inherit;
`;

const AuthorName = styled.span`
  margin-left: 5px;
`;

type Props = WithTranslation & {
  parent?: Comment;
  comment: Comment;
  dispatch: Dispatch<any>; // ???
  createLink?: string;
};

type State = {
  collapsed: boolean;
  edit: boolean;
  updatedComment: BasicComment;
  loading: boolean;
  contextModalOpen: boolean;
  replyEditor?: Comment;
  error?: Error;
};

class PullRequestComment extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      updatedComment: {
        ...props.comment
      },
      collapsed: props.comment.type === "TASK_DONE",
      edit: false,
      contextModalOpen: false
    };
  }

  startUpdate = () => {
    this.setState({
      loading: false,
      edit: true
    });
  };

  cancelUpdate = () => {
    this.setState({
      loading: false,
      edit: false,
      updatedComment: {
        ...this.props.comment
      }
    });
  };

  update = () => {
    const { updatedComment } = this.state;
    const comment = {
      ...this.props.comment,
      comment: updatedComment.comment,
      type: updatedComment.type
    };

    this.setState({
      loading: true
    });

    if (!comment._links.update) {
      throw new Error("comment has no update link");
    }

    const link = comment._links.update as Link;

    updatePullRequestComment(link.href, comment)
      .then(() => {
        return this.fetchRefreshed(comment, this.dispatchUpdate);
      })
      .catch(error => {
        this.setState({
          loading: false,
          error
        });
      });
  };

  dispatchUpdate = (comment: Comment) => {
    const { parent, dispatch } = this.props;
    if (parent) {
      dispatch(updateReply(parent.id, comment));
    } else {
      dispatch(updateComment(comment));
    }
  };

  dispatchDelete = (comment: Comment) => {
    const { parent, dispatch } = this.props;
    if (parent) {
      dispatch(deleteReply(parent.id, comment));
    } else {
      dispatch(deleteComment(comment));
    }
  };

  fetchRefreshed = (comment: Comment, callback: (c: Comment) => void) => {
    const link = comment._links.self as Link;
    return apiClient
      .get(link.href)
      .then(response => response.json())
      .then((c: Comment) => {
        callback(c);
        this.setState({
          loading: false,
          edit: false,
          updatedComment: {
            ...c
          }
        });
      })
      .catch(error => {
        this.setState({
          loading: false,
          error
        });
      });
  };

  getChildCount = (comment: Comment) => {
    if (comment._embedded && comment._embedded.replies) {
      return comment._embedded.replies.length;
    }
    return 0;
  };

  delete = () => {
    const { parent, comment, dispatch } = this.props;
    if (comment._links.delete) {
      const href = (comment._links.delete as Link).href;
      this.setState({
        loading: true
      });

      deletePullRequestComment(href)
        .then(response => {
          this.dispatchDelete(comment);
          if (parent && this.getChildCount(parent) === 1) {
            return this.fetchRefreshed(parent, (c: Comment) => dispatch(updateComment(c))).then(() => {
              this.setState({
                loading: false
              });
            });
          } else {
            this.setState({
              loading: false
            });
          }
        })
        .catch(error => {
          this.setState({
            loading: false,
            error
          });
        });
    } else {
      throw new Error("could not find required delete link");
    }
  };

  confirmDelete = () => {
    const { t } = this.props;
    confirmAlert({
      title: t("scm-review-plugin.comment.confirmDeleteAlert.title"),
      message: t("scm-review-plugin.comment.confirmDeleteAlert.message"),
      buttons: [
        {
          label: t("scm-review-plugin.comment.confirmDeleteAlert.submit"),
          onClick: () => this.delete()
        },
        {
          label: t("scm-review-plugin.comment.confirmDeleteAlert.cancel"),
          onClick: () => null
        }
      ]
    });
  };

  executeTransition = (transition: string) => {
    const { comment } = this.props;

    if (!(comment && comment._embedded && comment._embedded.possibleTransitions)) {
      throw new Error("comment has no possible transitions");
    }

    const transformation = comment._embedded.possibleTransitions.find((t: PossibleTransition) => t.name === transition);
    if (!transformation) {
      throw new Error("comment does not have transition " + transition);
    }

    transformPullRequestComment(transformation)
      .then(response => {
        return this.fetchRefreshed(comment, this.dispatchUpdate);
      })
      .catch(error => {
        this.setState({
          loading: false,
          error
        });
      });
  };

  confirmTransition = (transition: string, translationKey: string) => {
    const { t } = this.props;
    confirmAlert({
      title: t(translationKey + ".title"),
      message: t(translationKey + ".message"),
      buttons: [
        {
          label: t(translationKey + ".submit"),
          onClick: () => this.executeTransition(transition)
        },
        {
          label: t(translationKey + ".cancel"),
          onClick: () => null
        }
      ]
    });
  };

  toggleCollapse = () => {
    this.setState(prevState => ({
      collapsed: !prevState.collapsed
    }));
  };

  handleUpdateChange = (comment: string) => {
    this.setState({
      updatedComment: {
        ...this.state.updatedComment,
        comment: comment
      }
    });
  };

  createEditIcons = () => {
    const { parent, comment, createLink } = this.props;
    const { collapsed } = this.state;
    return (
      <CommentActionToolbar
        parent={parent}
        comment={comment}
        collapsed={collapsed}
        createLink={createLink}
        onUpdate={this.startUpdate}
        onDelete={this.confirmDelete}
        onReply={() => this.reply(comment)}
        onTransitionChange={this.confirmTransition}
      />
    );
  };

  createMessageEditor = () => {
    const { updatedComment, error } = this.state;
    return (
      <>
        <Textarea name="comment" value={updatedComment.comment} onChange={this.handleUpdateChange} />
        {error && <ErrorNotification error={error} />}
      </>
    );
  };

  onClose = () => {
    this.setState({
      contextModalOpen: false
    });
  };

  collectTags = (comment: Comment) => {
    let onOpenContext = () => {};
    if (comment.context) {
      onOpenContext = () => {
        this.setState({
          contextModalOpen: true
        });
      };
    }

    return <CommentTags comment={comment} onOpenContext={onOpenContext} />;
  };

  render() {
    const { parent, comment, dispatch, t } = this.props;
    const { loading, collapsed, edit, contextModalOpen } = this.state;

    if (loading) {
      return <Loading />;
    }

    let icons = null;
    let editButtons = null;
    let message = null;

    if (edit) {
      message = this.createMessageEditor();
      editButtons = <EditButtons comment={comment} onSubmit={this.update} onCancel={this.cancelUpdate} />;
    } else {
      message = !collapsed ? <CommentContent comment={comment} /> : "";
      icons = this.createEditIcons();
    }

    const collapseTitle = collapsed ? t("scm-review-plugin.comment.expand") : t("scm-review-plugin.comment.collapse");
    const collapseIcon = collapsed ? "fa-angle-right" : "fa-angle-down";

    return (
      <>
        {contextModalOpen && <ContextModal comment={comment} onClose={this.onClose} />}
        <CommentSpacingWrapper isChildComment={!!parent}>
          <article className="media">
            <div className="media-content is-clipped content">
              <p>
                <LinkWithInheritColor onClick={this.toggleCollapse} title={collapseTitle}>
                  <span className="icon is-small">
                    <i className={classNames("fa", collapseIcon)} />
                  </span>
                  <AuthorName>
                    <strong>{comment.author.displayName}</strong>{" "}
                  </AuthorName>
                </LinkWithInheritColor>
                <CommentMetadata>
                  <DateFromNow date={comment.date} /> <LastEdited comment={comment} />
                </CommentMetadata>
                {this.collectTags(comment)}
                <br />
                {message}
              </p>
              {editButtons}
            </div>
            {icons}
          </article>
        </CommentSpacingWrapper>
        {!collapsed && <Replies comment={comment} createLink={this.props.createLink} dispatch={dispatch} />}
        {this.createReplyEditorIfNeeded(comment.id)}
      </>
    );
  }

  reply = (comment: Comment) => {
    this.openReplyEditor(comment);
  };

  createReplyEditorIfNeeded = (id: string) => {
    const replyComment = this.state.replyEditor;
    if (replyComment && replyComment.id === id) {
      const { parent, comment } = this.props;
      return (
        <ReplyEditor
          comment={parent ? parent : comment}
          onCreation={this.onReplyCreated}
          onCancel={this.closeReplyEditor}
        />
      );
    }
  };

  onReplyCreated = (reply: Comment) => {
    const { parent, comment, dispatch } = this.props;
    dispatch(createReply(parent ? parent.id : comment.id, reply));
    this.closeReplyEditor();
  };

  openReplyEditor(comment: Comment) {
    this.setState({
      replyEditor: comment
    });
  }

  closeReplyEditor = () => {
    this.setState({
      replyEditor: undefined
    });
  };
}

export default withTranslation("plugins")(PullRequestComment);
