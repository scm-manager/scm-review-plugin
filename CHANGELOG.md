# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.0.0-rc10 - 2020-03-27
### Added
- Toast messages are enabled, again

## 2.0.0-rc9 - 2020-03-26
### Changed
- Notification channel are now cleaned every 30 seconds
- Every notifcation client is closed after 30 seconds
### Added
- Add a link to the pull requests inside the repository card overview ([#57](https://github.com/scm-manager/scm-review-plugin/pull/57))
### Fixed
- Threads for toast navigation could be in blocking state up to 1 hour

## 2.0.0-rc8 - 2020-03-16
### Fixed
- Removed toast messages, as these are not closed correctly on the server

## 2.0.0-rc7 - 2020-03-16
### Fixed
- rc4 with removed toast messages, as these are not closed correctly on the server

### Changed
- Changeover to MIT license ([#58](https://github.com/scm-manager/scm-review-plugin/pull/58))

## 2.0.0-rc6
### Added
- Add swagger rest annotations to generate openAPI specs for the scm-openapi-plugin. ([#54](https://github.com/scm-manager/scm-review-plugin/pull/54))
- Make navigation item collapsable ([#55](https://github.com/scm-manager/scm-review-plugin/pull/55))

## 2.0.0-rc5 - 2020-02-18
Re-release of 2.0.0-rc3

## 2.0.0-rc4 - 2020-02-18
Backport of [#48](https://github.com/scm-manager/scm-review-plugin/pull/48)) and [#53](https://github.com/scm-manager/scm-review-plugin/pull/53) to 2.0.0-rc2

## 2.0.0-rc3 - 2020-02-14
### Added
- Mark files as reviewed in diff view ([#48](https://github.com/scm-manager/scm-review-plugin/pull/48))
- Add @-Mentionings for User in Comments ([#50](https://github.com/scm-manager/scm-review-plugin/pull/50))

### Changed
- Use icon only buttons for diff file controls

### Fixed
- Comments are marked as outdated on commits without root permissions ([#49](https://github.com/scm-manager/scm-review-plugin/pull/49))
- Merge commits are created with the current user as author ([#53](https://github.com/scm-manager/scm-review-plugin/pull/53))

### Security
- The creator of a pull request can no longer fake approvals ([#48](https://github.com/scm-manager/scm-review-plugin/pull/48))

## 2.0.0-rc2 - 2020-01-29
### Added
- Filter and sort function of the pull requests in the overview ([#19](https://github.com/scm-manager/scm-review-plugin/pull/19), [#21](https://github.com/scm-manager/scm-review-plugin/pull/21))
- Show toast on changes in the detailed view ([#26](https://github.com/scm-manager/scm-review-plugin/pull/26))
- Tooltip that shows all reviewers for the respective PR in the overview ([#20](https://github.com/scm-manager/scm-review-plugin/pull/20))
- User will be notified if the open pull request or its comments change while working on it
- Frontend validation whether PR can be created ([#18](https://github.com/scm-manager/scm-review-plugin/pull/18))
- Display of open tasks in title of PR overview and detailed view ([#25](https://github.com/scm-manager/scm-review-plugin/pull/25))
- Conflicts are displayed visually in a separate conflicts tab ([#30](https://github.com/scm-manager/scm-review-plugin/pull/30))
- Introduction of Merge guard, which allows plugins to prevent a merge ([#37](https://github.com/scm-manager/scm-review-plugin/pull/37))
- Add email notifications for approvals ([#28](https://github.com/scm-manager/scm-review-plugin/pull/28))
- Branches can be protected, so that they are writable only with pull requests ([#42](https://github.com/scm-manager/scm-review-plugin/pull/42))

### Changed
- Merged or rejected PRs keep the diff and commits as long as the source branch or the revision of the source and target branch are still available ([#27](https://github.com/scm-manager/scm-review-plugin/pull/27))
- After creating a pull request, it is called up ([#34](https://github.com/scm-manager/scm-review-plugin/pull/34))

### Fixed
- Description of a PR changes immediately after editing ([#22](https://github.com/scm-manager/scm-review-plugin/pull/22))
- Permission check for approve/disapprove ([#31](https://github.com/scm-manager/scm-review-plugin/pull/31))
- Do not show confirm dialog if nothing changed ([#32](https://github.com/scm-manager/scm-review-plugin/pull/32))
- Hide diff tab on pull request if target branch was deleted ([#36](https://github.com/scm-manager/scm-review-plugin/pull/36))
- Do not render subscription button without permissions ([#38](https://github.com/scm-manager/scm-review-plugin/pull/38))
- Loading of closed/merged pull requests ([#39](https://github.com/scm-manager/scm-review-plugin/pull/39))
- Radio button disappeared when opening several comment forms ([#40](https://github.com/scm-manager/scm-review-plugin/pull/40))
- Fix clipped autocomplete when adding reviewer on pull request editing ([#46](https://github.com/scm-manager/scm-review-plugin/pull/46))

## [2.0.0-rc1] - 2019-12-02
### Added
- List overview for pull requests
- Detailed view of PRs including new comments, changesets and diff tab
- Markdown in description and comments
- You can subscribe to a pull request (Author is automatically added to list)
- Subscribers are informed about changes by email
- Responses to comments are shown as a thread
- Top-level comments can either be created as a comment or as a task
- Tasks can be marked as completed by anyone who has the right to make comments
- Responses to a task are collapsed if the task has been marked as completed
- It is saved and displayed who has marked a task as completed and who last edited it
- Outdated comments are marked as "outdated"
- Original context of inline comments is presented in a modal after clicking on the file name in the comments tab
- Get an error when writing comments on outdated code
- Integration of the CI status from scm-ci-plugin in the detailed view
- Implement plugin bundler with webpack
- Fast-forward-if possible and squash function with custom commit message ([#9](https://github.com/scm-manager/scm-review-plugin/pull/9), [#6](https://github.com/scm-manager/scm-review-plugin/pull/6))
- Option to remove the source branch after merge ([#8](https://github.com/scm-manager/scm-review-plugin/pull/8))
- As a reviewer, I can explicitly give my consent to merge via "Approve"-button

[2.0.0-rc1]: https://github.com/scm-manager/scm-review-plugin/releases/tag/2.0.0-rc1
