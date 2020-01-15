# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased - 2019-01-16
### Added
- Conflicts are displayed visually in a separate conflicts tab (#30)
- Introduction of Merge guard, which allows plugins to prevent a merge (#37)
- Add email notifications for approvals (#28)

### Changed
- After creating a pull request, it is called up (#34)

### Fixed
- Permission check for approve/disapprove (#31)
- Do not show confirm dialog if nothing changed (#32)
- Hide diff tab on pull request if target branch was deleted (#36)
- Do not render subscription button without permissions (#38)
- Loading of closed/merged pull requests (#39)
- Radio button disappeared when opening several comment forms (#40)

## Unreleased - 2019-12-19
### Added
- Filter and sort function of the pull requests in the overview (#19, #21)
- Show toast on changes in the detailed view (#26)
- Tooltip that shows all reviewers for the respective PR in the overview (#20)
- User will be notified if the open pull request or its comments change while working on it
- Frontend validation whether PR can be created (#18)
- Display of open tasks in title of PR overview and detailed view (#25)

### Changed
- Merged PRs keep the diff and commits as long as the source branch or the revision of the source and target branch are still available (#27)

### Fixed
- Description of a PR changes immediately after editing (#22)

## [2.0.0-rc1] - 2019-12-02
### Added
- Add pull request infos to branch view (#16)

### Fixed
- Corrected migration of inline-comments
- When creating a new pull request, the PR number was zero in the notification email

## Unreleased - 2019-11-15
### Added
- Fast-forward-if possible and squash function with custom commit message (#9, #6)
- Option to remove the source branch after merge (#8)
- As a reviewer, I can explicitly give my consent to merge via button

### Changed
- New comments are displayed without a complete reload (#11)

## Unreleased - 2019-10-17
### Added
- Implement plugin bundler with webpack

### Changed
- Switched from React-JSS to StyledComponents

### Fixed
- Comments were not rendered as markdown in the context view
- Inline comments on deleted files did not work

## Unreleased - 2019-09-25
### Added
- Integration of the CI status from scm-ci-plugin in the detailed view

### Fixed
- Diff with binary files fixed

## Unreleased - 2019-08-20
### Added
- Save context for comments
- Outdated comments are marked as "outdated"
- Original context of inline comments is presented in a modal after clicking on the file name in the comments tab
- Get an error when writing comments on outdated code

## Unreleased - 2019-07-23
### Added
- Top-level comments can either be created as a comment or as a task
- Tasks can be marked as completed by anyone who has the right to make comments
- Responses to a task are collapsed if the task has been marked as completed
- It is saved and displayed who has marked a task as completed and who last edited it

### Changed
- Comments can no longer be marked as "done"
- New responses can no longer be written to closed tasks

### Fixed
- Missing error handling when editing deleted comments

## Unreleased - 2019-05-31
### Added
- Responses to comments are shown as a thread
- Comments can be marked as done

### Changed
- File name of file comments is shown in the comments tab instead of "inline"

### Fixed
- The diff was only calculated after the source branch had been changed once

## Unreleased - 2019-05-03
### Added
- When creating a PR, the default branch is selected as target by default

### Changed
- Empty PRs can no longer be created

## Unreleased - 2019-03-27
### Added
- You can subscribe to a pull request (Author is automatically added to list)
- Subscribers are informed about changes by email
- Consistent mail layout with mustache template
- Desired reviewers can be defined

## Unreleased - 2019-03-19
### Added
- Markdown in description and comments
- Save who merged or rejected a pull request
- Marking of inline and file comments in the comments section

### Fixed
- Handle deleted branches in open pull requests
- Merge Dry Run is only run once per call
- PR navigation link is only displayed when user is authorized
- Merge button is only displayed for users with push permission
- After a merge, the comments of a pull request are displayed again

## Unreleased - 2019-03-08
### Added
- Inline and file comments implemented

### Fixed
- Comments permissions
- Incorrect routing with merged PR

## Unreleased - 2019-01-23
### Fixed
- Corrected deadlock regarding following message "Currently there are no Pull Requests available."

## Unreleased - 2018-12-12
### Added
- List overview for pull requests
- Detailed view of PRs including new comments, changesets and diff tab

## Unreleased - 2018-11-14
### Added
- Initial functionality for review plugin

[2.0.0-rc1]: https://github.com/scm-manager/scm-review-plugin/releases/tag/2.0.0-rc1
