# Tracking of Changes in Pull Requests

## Overview

A pull request can take different types of comments:

- __global comments__, regarding the pull request as a whole,
- __file comments__, regarding a single added, changed, or deleted file
- __inline comments__, regarding a particular added, changed, or deleted line in a file.
  A inline comment is rooted at a concrete line number, that either refers to the
  previous version of the file or the new version.

All these comments are based upon a specific diff between source and target at a given
point in time. During the lifetime of a pull requests it is possible to add new commits
to the source and/or the target branches or even to rebase the complete source branch.
This will in general change the diff, comments were based upon.

This document aims to discuss the kinds of changes that may occur and how they can be
tracked in a transparent way.

## Computation of Diffs

Before we dig deep into the ways changes to the repository can affect a diff for a pull
request, we have to take a moment to explain how a diff for a pull request is computed:

The purpose of a pull request is to discuss, whether the changes implemented by a given
branch in respect to a base branch are considered to be ok or not. To clarify what this
means, let us take a look of some examples, going from simple to complex cases:

### Fast forward

```
         X---Y  feature
        /
   A---B        base
```

This is the simplest imaginable case: The feature simply adds new commits to a base, that
has not diverged since the time the feature was branched. In this case, the diff simply is
the sum of commits `X` and `Y` and therefore is equal to the diff between `feature` and
`base`.

### Diverged history

```
         X---Y  feature
        /
   A---B---C    base
```

In this case, the base branch has evolved since the time, the feature branch has been
created. If we would only "diff" branches `feature` and `base`, we would get the wrong
impression that branch `feature` reverts changes from commit `C`. For example a file
added in `C` would be shown as "deleted" in the diff.

Instead of this we have to compute the diff between `feature` and commit `B`, because
this only shows the actual changed done by `X` and `Y`.

### Partially merged base branch

```
         X---Y---Z  feature
        /   /              
   A---B---C---D    base
```

To compute the diff, we have to take into account that all changed made by `C` are already
merged into the feature. So in this case we must not compute the diff between `feature`
and the once-upon-a-time branch point `B`, but between `faature` and `C`. The commit
`C` is what git calls the "merge base".

### Partially merged feature branch

```
         X---Y---Z  feature
        /     \            
   A---B---C---D    base
```

Having this history, we have to admit that parts of the feature are already included in
the base branch and therefore are no longer taken into account. Here the only commit
of interest is commit `Z`.

## Kinds of Changes

As we have shown in the previous chapter, the diff of a feature branch can not only change
by changing the feature branch itself, but also by changing the base branch (eg. by merging
a part of the feature branch).

Taking this into account, we do not have to care for changes on the commit level, but
solely on the level of the resulting diff. Even a complete rebase of a feature branch may
not change anything in the diff result, as the following example shows:

Initial state of the repository at the time of creation of the pull request:
```
         X---Y  feature
        /                 
   A---B        base
```
State of the repository after a rebase of the feature branch:
```
             X'--Y'  feature
            /                 
   A---B---C         base
```

As long as commit `C` does not change any files touched by commits `X` or `Y`, this
rebase of the branch `feature` does not change anything regarding the relevant diff for
the feature.

Therefore we will concentrate on the result, only (one could say we have to take into
account the diff of the diffs before and after the change).

### Added file

A new file may effect the semantics of global comments for a pull request, but it is
nearly impossible to determine whether this is the case or not (for example a comment like
"_You forgot to checkin the translation file_" may be obsolete when this file is checked
in by a new commit, but this cannot be detected by a non general AI system).

### Deleted file

The deletion of a file can have more severe impacts. All file or inline comments made for
this file cannot be displayed in the current diff any more.

### Renamed file

A renamed file is a special case of a deletion and a addition of a file. If one wants to
handle renamed files in a specific way, one has to take the following into account:

- It may not be completely deterministic, what a "rename" is. Where Mercurial or
  Subversion track these changes, Git uses a heuristic for this (when a file was deleted,
  another one was added and these two files do not differ too much, this may have been a
  renaming). But even when a renaming was tracked directly, this may lead to wrong
  conclusions (maybe the file was just renamed to bootstrap a new file with a completely
  different purpose).
- When a file was renamed multiple times in different commits, this may not be tracked
  in the same way as it would be done in a single commit. So it may be confusing, when
  these changes are handled differently.

### Changed file

When new lines are deleted, added or modified in a file, this effects all inline comments
at or following these lines. If you want to show these comments in their correct context,
one has to determine

- where the original line has moved to, and
- whether this line or relevant context has been changed.

One can argue that a comment at a line that has changed can cause more confusion than
anything else. Therefore it is questionable, if possibly outdated inline comments still
should be displayed as such inside the diff.

To be honest, the same can be said for file comments. Though here we do not have a
concrete context in the form of added, changed or deleted lines, the comment itself
may no longer make any sense for the changed version.

## Metadata

Before we draw conclusions of what should be, one should take a look at the available
information we have for comments. At the time of writing a comment, we have (at least
for Git repositories; this may be different for Mercurial):

- For global comments
  - the current revision of the source branch,
  - the current revision of the target branch,
  - the current revision of the merge base.
- For file comments additionally
  - the revision of the old file (may be not set or artificial like `0000000` for added files),
  - the revision of the new file (may be not set or artificial like `0000000` for deleted files),
  - the name of the old file (may be not set or artificial like `/dev/null` for added files),
  - the name of the new file (may be not set or artificial like `/dev/null` for deleted files).
- For inline comments additionally
  - the line number in the new version for added lines,
  - the line number in the old version for deleted lines,
  - the line numbers for both the old and the new version for changed lines,
  - the content of the line,
  - the context of the line (preceding and following content),
  - the "hunk" notation for the block of the change (eg. `@@ -1,6 +1,8 @@`).

