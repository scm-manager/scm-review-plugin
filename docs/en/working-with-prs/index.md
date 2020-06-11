---
title: Working with Pull Requests
---
To work efficiently with pull requests there are several ways to exchange information between the author and the reviewer(s).

### Add reviewer
When creating or editing his pull request, the author can add reviewer(s). All reviewers are informed about the upcoming review by e-mail and they are listed in the details of the pull request.

### Subscribe to a pull request
Reviewers can subscribe ("+") or unsubscribe ("-") from a pull request in the upper right corner of the details page of the pull request. Subscribers are informed about changes and comments on the pull request by e-mail.

### Approve pull request
After a successful review, the reviewer can approve the pull request for a merge. At this point, the approval is merely an information for the author that the review is done. Once a reviewer approved a review, there is a green checkmark behind his name in the list of reviewers.

### Create comments
There are three different kinds of comments for pull requests.

* **General comment:** Is related to the pull request in general. It can be created by using the comment editor in the "Comments" tab.
* **File comment:** Can be created in the head of a file in the "Diff" tab. There is an "Add comment for file" button with a speech bubble icon.
* **Line comment:** Can be created by clicking on a line in the diff view.

All comments are shown in the "Comments" tab. The file and line comments are also shown in the "Diff" tab.

![Pull Request - Create comment](assets/createComment.png)

There are these options to interact with a comment:

|Icon|Meaning|
|---|--------------------------------------------|
|![Delete](assets/icon-delete.png){ height=5mm }|Delete comment|
|![Edit](assets/icon-edit.png){ height=5mm }|Edit comment|
|![Task](assets/icon-make-task.png){ height=5mm }|Convert comment into task|
|![Comment](assets/icon-make-comment.png){ height=5mm }|Convert task into comment|
|![Reply](assets/icon-reply.png){ height=5mm }|Reply to comment or task|
|![Done](assets/icon-done.png){ height=5mm }|Mark task as done|
|![Reopen](assets/icon-reopen.png){ height=5mm }|Mark task as *not* done|

### Outdated comments
As soon as there are new commits in a pull request, some comments are marked as outdated. General comments are outdated after each new commit. File and line comments are outdated once a commit changes the related file.

![Pull Request - Outdated comment](assets/outdatedComment.png)

These comments are marked with an "outdated" tag. By clicking on the blue "outdated" tag or a blue file name the original context of the comment is shown. This allows to backtrace why a comment was made.

![Pull Request - Outdated context](assets/outdatedContext.png)

### Create tasks
If a reviewer finds errors or wants to propose changes, he can do that by using tasks. The comments editor can be changed into a task editor by changing the radio button. Tasks are shown in the details page of the pull request and in the list of pull requests, e.g. "2 / 5 tasks done".

Open tasks can be marked as done using the "Mark as done" icon. At this point tasks solely informative and therefore no condition to merge a pull request.

![Pull Request - Create task](assets/createTask.png)

### Reject a pull request
If a reviewer thinks that a pull request should not be merged, he can reject it. That should happen in coordination with the author. It is not possible to reopen a rejected pull request. Instead a new pull request has to be opened.

### Merge a pull request 
If a pull request can be merged, the changes can be applied in different ways after clicking the button "Merge Pull Request":

* **Merge Commit:** The branches are merged with a new commit.
* **Fast forward, if possible:** If possible, the target branch is set to the state of the source branch without a merge. If this is not possible, a regular merge commit is created.
* **Squash:** All changes from the source branch are condensed into one commit on the target branch. The commit history of the source branch is not transferred to the target branch.

If desired, it is possible to replace the default message with an individual commit message. If the box "Delete branch" is checked, the source branch is deleted from the repository after the successful merge. 

In case of a merge conflict, the pull request cannot be merged automatically. The conflicts have to be resolved manually before the merge.

![Pull Request - Merge-Modal](assets/mergeModal.png)
