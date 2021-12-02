# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased
### Changed
- Improve structure of headings ([#170](https://github.com/scm-manager/scm-review-plugin/pull/170))

## 2.13.0 - 2021-11-17
### Added
- Exclude users from branch protection ([#167](https://github.com/scm-manager/scm-review-plugin/pull/167))

## 2.12.1 - 2021-11-05
### Fixed
- Fix eslint errors and replace <a> without href ([#159](https://github.com/scm-manager/scm-review-plugin/pull/159))
- Fix pull request person object for avatarImage ([#158](https://github.com/scm-manager/scm-review-plugin/pull/158))

## 2.12.0 - 2021-10-21
### Changed
- Styling to match landing-page-plugin update ([#154](https://github.com/scm-manager/scm-review-plugin/pull/154))

### Fixed
- Not found error after merge ([#156](https://github.com/scm-manager/scm-review-plugin/pull/156))

## 2.11.2 - 2021-10-19
### Fixed
- Fix internal server error in simple merge ([#155](https://github.com/scm-manager/scm-review-plugin/pull/155))

## 2.11.1 - 2021-10-08
### Fixed
- Merges by users without configured mail address ([#149](https://github.com/scm-manager/scm-review-plugin/pull/149))
- Correct matching of pull-request navigation entry ([#150](https://github.com/scm-manager/scm-review-plugin/pull/150))
- Remove the Pull Request Link from the push message on a single branch repository ([#112](https://github.com/scm-manager/scm-review-plugin/issues/112))

## 2.11.0 - 2021-09-08
### Added
- Create index for pull requests to make them searchable ([#143](https://github.com/scm-manager/scm-review-plugin/pull/143))
- Create index for pull request comments to make them searchable ([#145](https://github.com/scm-manager/scm-review-plugin/pull/145))

### Fixed
- Reject pull requests if branch was deleted on merge ([#144](https://github.com/scm-manager/scm-review-plugin/pull/144))
- Error on diff view for new pull request ([#147](https://github.com/scm-manager/scm-review-plugin/pull/147))
- Do not show update notification if changes already fetched ([146](https://github.com/scm-manager/scm-review-plugin/pull/146))

## 2.10.1 - 2021-08-25
### Fixed
- Closing comment editors on parallel changes ([#140](https://github.com/scm-manager/scm-review-plugin/pull/140))
- Missing merge button update after approval and comment action ([#141](https://github.com/scm-manager/scm-review-plugin/pull/141))
- Too wide branch selection when creating pr ([#142](https://github.com/scm-manager/scm-review-plugin/pull/142))

## 2.10.0 - 2021-08-04
### Changed
- Use react-query to enable frontend caching ([#138](https://github.com/scm-manager/scm-review-plugin/pull/138))

## 2.9.2 - 2021-07-06
### Fixed
- Close toast notification after refresh ([#136](https://github.com/scm-manager/scm-review-plugin/pull/136))
- Update comment content on refresh ([#136](https://github.com/scm-manager/scm-review-plugin/pull/136))

## 2.9.1 - 2021-05-05
### Changed
- Set pull request author as commit author for squash commits via SCMM ([#134](https://github.com/scm-manager/scm-review-plugin/pull/134))

### Fixed
- Fix pull request comments not opening/closing unless the window is refocused ([#135](https://github.com/scm-manager/scm-review-plugin/pull/135))

## 2.9.0 - 2021-04-22
### Added
- System replies which are not modifiable nor deletable ([#130](https://github.com/scm-manager/scm-review-plugin/pull/130)
- Show pull request reviser for merged/rejected pull requests ([#132](https://github.com/scm-manager/scm-review-plugin/pull/132))

### Changed
- Make pull request enrichable by embedded objects ([#130](https://github.com/scm-manager/scm-review-plugin/pull/130)

### Fixed
- Prevent collapsed diffs from reopen on page refocus ([#131](https://github.com/scm-manager/scm-review-plugin/pull/131))
- Show forbidden notification if there is no read permission ([#133](https://github.com/scm-manager/scm-review-plugin/pull/133))

## 2.8.0 - 2021-04-07
### Added
- Enable anchor links for pull request comments ([#127](https://github.com/scm-manager/scm-review-plugin/pull/127))
- ExtensionPoint to modify title ([#129](https://github.com/scm-manager/scm-review-plugin/pull/129))

### Fixed
- Collapse of file in diff when adding comment ([#126](https://github.com/scm-manager/scm-review-plugin/pull/126))
- Correct styling of modal footer and capitalize titles ([#128](https://github.com/scm-manager/scm-review-plugin/pull/128))

## 2.7.1 - 2021-03-26
### Fixed
- Path for open api spec ([#123](https://github.com/scm-manager/scm-review-plugin/pull/123))
- fix outdated tag without context being displayed as clickable ([#125](https://github.com/scm-manager/scm-review-plugin/pull/125))

### Added
- add extension points for markdown ast plugins to pr description & comments ([#122](https://github.com/scm-manager/scm-review-plugin/pull/122))

## 2.6.3 - 2021-03-01
### Fixed
- Prevents branch pr table from overlapping with navigation ([#121](https://github.com/scm-manager/scm-review-plugin/pull/121))

## 2.6.2 - 2021-01-29
### Fixed
- Wrap long branch names in PR overview table ([#118](https://github.com/scm-manager/scm-review-plugin/pull/118))

## 2.6.1 - 2021-01-13
### Fixed
- Reject pull requests for branches deleted from the UI ([#116](https://github.com/scm-manager/scm-review-plugin/pull/116))

## 2.6.0 - 2020-12-17
### Added
- Mark read only verbs to be able to see pull requests in archived repositories ([#114](https://github.com/scm-manager/scm-review-plugin/pull/114))

## 2.5.1 - 2020-11-25
### Fixed
- User not found" error for Pull Requests with approvals from deleted users ([#108](https://github.com/scm-manager/scm-review-plugin/pull/108))

## 2.5.0 - 2020-11-20
### Added
- New endpoint to check pull request before creation ([#105](https://github.com/scm-manager/scm-review-plugin/pull/105))

### Fixed
- Error on diff viewer during pull request creation ([#106](https://github.com/scm-manager/scm-review-plugin/pull/106))

## 2.4.2 - 2020-11-06
### Fixed
- Missing email notification for updated pull requests ([#103](https://github.com/scm-manager/scm-review-plugin/pull/103))
- Broken collapse state for reviewed files ([#104](https://github.com/scm-manager/scm-review-plugin/pull/104))

## 2.4.1 - 2020-10-27
### Fixed
- Handle users without an email address ([#101](https://github.com/scm-manager/scm-review-plugin/pull/101))

## 2.4.0.1 - 2020-11-25
### Fixed
- User not found" error for Pull Requests with approvals from deleted users (backport from 2.5.1) ([#108](https://github.com/scm-manager/scm-review-plugin/pull/108))

## 2.4.0 - 2020-09-25
### Added
- Add support for pr merge with prior rebase ([#99](https://github.com/scm-manager/scm-review-plugin/pull/99))

## 2.3.0 - 2020-08-14
### Added
- Sort mechanism for rules in "Add Rule" dropdown ([#88](https://github.com/scm-manager/scm-review-plugin/pull/88))
- Append source and target branch links to pull request ([#87](https://github.com/scm-manager/scm-review-plugin/pull/87))
- Extends permission role `CI-SERVER` with the permission to read pull requests ([#91](https://github.com/scm-manager/scm-review-plugin/pull/91))

### Fixed
- Checks workflow engine and possible other rules for merges, when pull requests are merge by pushes ([#94](https://github.com/scm-manager/scm-review-plugin/pull/94))

## 2.2.0 - 2020-07-03
### Added
- Use mail topics so users can unsubscribe from mails for specific events ([#85](https://github.com/scm-manager/scm-review-plugin/pull/85))
- Send mails for replies ([#85](https://github.com/scm-manager/scm-review-plugin/pull/85))

### Fixed
- Add missing permission check on accessing open pull requests ([#86](https://github.com/scm-manager/scm-review-plugin/pull/86))

## 2.1.0 - 2020-06-19
### Added
- Documentation in English and German ([#84](https://github.com/scm-manager/scm-review-plugin/pull/84))
- Disable the possibility to add line comments to expanded diff lines ([#83](https://github.com/scm-manager/scm-review-plugin/pull/83))

## 2.0.0 - 2020-06-04
### Added
- Toast messages are enabled, again

### Changed
- Adapted to new modification api from core ([#77](https://github.com/scm-manager/scm-review-plugin/pull/77))
- Rebuild for api changes from core

### Fixed
- Exception on push in repository without merge support ([#78](https://github.com/scm-manager/scm-review-plugin/pull/78))

## 2.0.0-rc9 - 2020-03-26
### Added
- Add a link to the pull requests inside the repository card overview ([#57](https://github.com/scm-manager/scm-review-plugin/pull/57))

### Changed
- Notification channel are now cleaned every 30 seconds
- Every notifcation client is closed after 30 seconds

### Fixed
- Threads for toast navigation could be in blocking state up to 1 hour

## 2.0.0-rc8 - 2020-03-16
### Fixed
- Removed toast messages, as these are not closed correctly on the server

## 2.0.0-rc7 - 2020-03-16
### Changed
- Changeover to MIT license ([#58](https://github.com/scm-manager/scm-review-plugin/pull/58))

### Fixed
- rc4 with removed toast messages, as these are not closed correctly on the server

### Added
- Add swagger rest annotations to generate openAPI specs for the scm-openapi-plugin. ([#54](https://github.com/scm-manager/scm-review-plugin/pull/54))
- Make navigation item collapsable ([#55](https://github.com/scm-manager/scm-review-plugin/pull/55))

## 2.0.0-rc5 - 2020-02-18
