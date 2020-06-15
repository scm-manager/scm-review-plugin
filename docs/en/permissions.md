---
title: Permissions
---
The Review plugin comes with these permissions:

* Global permissions:
    * Read all pull requests: May see all pull requests for all repositories
    * Modify all pull requests: May create, comment and reject pull requests for all repositories
    * Configure all pull requests: May configure pull requests for all repositories
    * Perform emergency merge: May perform an emergency merge and ignore the configured workflow rules
    * Configure workflow engine configuration: May configure workflow engine configuration for all repositories
* Repository-specific permissions:
    * Create pull request: May create new pull requests
    * Read pull requests: May read all pull requests
    * Comment pull request: May add comments to pull requests
    * Modify pull request: May modify the title, the description and the reviewers of the pull request itself and modify/delete comments from other users
    * Merge/reject pull requests: May merge or reject pull requests (merge needs additional permission to push the repository)
    * Configure pull requests: May configure pull requests for this repository
    * Read workflow engine configuration: May read the workflow engine configuration for this repository
    * Write workflow engine configuration: May configure the workflow engine configuration for this repository
    * Performe emergency merge: May merge pull requests even though not all rules configured in the workflow are fulfilled

Users with the READ role can automatically read the pull requests of the repository. The WRITE role adds the permissions to create, comment accept resp. reject pull requests.
