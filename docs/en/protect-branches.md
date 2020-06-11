---
title: Protect branches
---
### Locally
The repository settings have the tab "Pull Requests". There, an additional repository-specific protection for branches can be activated. The protection can be activated for specific branches or based on a pattern (e.g. "feature/*"). If a branch is protected, it can only be modified by using pull requests. Thanks to this constraint a review workflow can be enforced.

### Globally
In addition, there is also the option to define an intra-repository write protection through the administration area. There it can be stated that it is not allowed by default to directly push changes onto the "master" branch if there is no other configuration in place for the repository.

If repository owners should not be allowed to define their own branch protection, the option can be deactivated globally for the SCM-Manager instance using the checkbox.
