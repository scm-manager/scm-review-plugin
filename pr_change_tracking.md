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

## Computation of diffs

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

## Kinds of changes

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

### Added or newly changed file

A new file or the change of a file not changed before may effect the semantics of global
comments for a pull request, but it is nearly impossible to determine whether this is the
case or not (for example a comment like "_You forgot to checkin the translation file_" may
be obsolete when this file is checked in by a new commit, but this cannot be detected by a
non general AI system).

### Reverted file

The reversion of a file can have more severe impacts. All file or inline comments made for
this file cannot be displayed in the current diff any more, because the file does not
differ from the original version any more and therefore is no part of the diff any more.

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

### Further changes of a file

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

Before we draw conclusions of what should be, one should take a look at the (at least
theoretically) available information we have for comments. At the time of writing a
comment, we have (at least for Git repositories; this may be different for Mercurial or
other systems):

1. For global comments
   1. the current revision of the source branch,
   1. the current revision of the target branch,
   1. the current revision of the merge base.
1. For file comments additionally
   1. (depending on the scm provider) a index hash of the old file (may be not set or
      artificial like `0000000` for added files),
   1. (depending on the scm provider) a index hash of the new file (may be not set or
      artificial like `0000000` for deleted files),
   1. the name of the old file (may be not set or artificial like `/dev/null` for added
      files),
   1. the name of the new file (may be not set or artificial like `/dev/null` for deleted
      files).
1. For inline comments additionally
   1. the line number in the new version for added lines,
   1. the line number in the old version for deleted lines,
   1. the line numbers for both the old and the new version for changed lines,
   1. the content of the line,
   1. the context of the line (preceding and following content),
   1. the "hunk" notation for the block of the change (eg. `@@ -1,6 +1,8 @@`).

_Mind that the index hash seems not to be supported by Mercurial._

## Target vision

This is what we want to achieve:

1. In the "Comments" view, __inline comments__ shall be displayed with a partial context,
   that is they shall be displayed with a number of preceding and following lines from the
   diff at the time of creation. When the current revision of the file does not match the
   revision of the file at creation time, the comment shall be marked as outdated.
1. __Global comments__ shall be marked as "outdated", when they were written for a release
   that is no longer the head of the source branch.
1. __Global comments__ shall _not_ be marked as "outdated", when the merge base changes.
1. __File comments__ shall be marked as "outdated", when the current file version does not
   match the version of the file at the time comment was created.
1. __File comments__ shall _not_ be marked as "outdated", when the merge base file version
   does not match the base version of the file at the time comment was created.
1. Outdated __file comments__ shall still be shown in the diff view, provided that the file
   is still part of the diff. When the file was renamed, the comment shall only be shown
   in the comment overview.
1. __File or inline comments__ for reverted files (that is, the file is no longer part of
   the diff) shall be marked as outdated and only be visible in the comment overview.
1. __Inline comments__ for files that have another source _or_ target revision than at the
   time of the creation of the comment shall not be displayed inline in the diff, but the
   file shall have a mark that there are outdated inline comments present. These comments
   shall be shown in a popup when the mark is clicked, each with its original context.

## Possible solution

To achieve this vision, we have to keep different metadata.

### Global comments

The only demand for global commands is that they will be marked as outdated as soon as
the head revision of the source branch is different from the revision at the time of the
comment creation. To achieve this, we only have to store the current source revision
alongside with the comment.

### File comments

For file comments it is necessary to store the old and the new index hash of the file
and the current name alongside the comment. The revisions of the source or the base
branch are of no interest for file comments, because for instance a new commit does not
necessarily imply that a specific file was changed.

### Inline comments

Like for file comments, we have to store the old and the new index hash of the file and
its current name. Additionally we have keep the line number of either the old or the new
version.

To be able to show the comment in its original context there are two possible solutions:

1. We can store a part of the diff.
2. We can compute the diff between the two versions "on the fly" each time it is needed.

Both solutions have their own pros and cons: The first solution limits the context to the
part that was stored. The second solution needs additional computation each time we want
to display the context. Furthermore one has to take into account, that after some time
the file revisions can be garbage collected, when they are no longer part of an active
branch (this can be the case when feature branches are squashed after the review process).

## Implementation

### First version

At the time of writing, diffs are parsed only on the frontend side. That is, the diff itself
is calculated in the backend (directly using the scm implementation), but the frontend
interprets this diff. This has a few implications:

1. The backend cannot determine, whether or not a comment is outdated.
1. The information about the state of comments has no representation in the REST layer. 
1. The frontend needs the diff to compute the state of comments even when the diff itself
   does not have to be rendered.
1. We only do have the diff itself for computations. If this diff does not support specific
   parts (like the index hash, that is missing in Mercurials diff), there is no other way
   to draw conclusions.
1. There is no way to cache computations (though this is no problem for the server, because
   all computation is done on the client).
1. The server has to rely solely on client requests without the possibility to check input
   (eg. the server cannot check whether the location of a comment is a valid location).

### Preferable changes

1. **Compute outdated comments on the server side.**
   
   This would have the following advantages:
   - The REST layer would be "complete"
   - The client would not have to fetch the diff only because it would like to compute the
     status of some comments
   - It is easier to create a provider agnostic abstraction (saying we could not only
     support Git, but also Mercurial without having to meddle with its diff implementation)
   
   The disadvantage is simple: Implementation cost.
1. **Mark file and inline comments only as outdated, when the files have changed.**
   
   That is, not just because the revision of either the source or the target branch has
   changed.
1. **Support Mercurial, not only Git.**
