# Contributing to Bonita getting started tutorial

The following is a set of guidelines for contributing to Bonita getting started tutorial, to make it easy to follow, useful for a new user and consistent with the product.

## Help us 

To help us, try to verify if your contribution have a clear title on the top of the page.  
Also verify if new or updated pages have a summary of content directly after the title. This summary must have less than 140 characters. When updating an already existing page with no summary, try to write a little summary.

```
# Bonita overview

Bonita is a powerful BPM-based application platform for building highly engaging, personalized, process-based business applications to get things done.
```

## Pull requests


A branch should be name in kebab case prefixed by the type of the feature the branch has been created for (feat, fix, style, tr, chore, howto,...).  
For instance, for a branch fixing a typo in the User REST API, its name should be `fix/user-delete-rest-api-typo`.


```
My awesome contribution to Bonita Community.

```

Ideally, a pull request, at first, should contain a single commit containing the changes you want to suggest. Other commits may be added after reviews.
Having a single commit allows GitHub to use the commit title as pull request name.

The commit message format should be the following :
```
# Commit title / format: <type>(<scope>): <subject> 
# type in (feat, fix, style, howto, tr, ...)
 
 
# Explain why this change is being made
 
 
# Provide links to any relevant tickets, articles or other resources
```

For instance, when adding an warning alert inside the User DELETE REST API method
```
fix(api): add alert on the User REST API DELETE action

Allows users to be warned about the consequences to DELETE a user
instead of deactivating it.
```

### Convention for Contribution

A contribution is composed of a `TITLE` and a `DESCRIPTION`

- TITLE must NOT exceed 80 characters, for readability in Github interface.
- DESCRIPTION is not limited to any number of characters and can extend to several lines.

#### Title
TITLE MUST respect the following format: `<type>(<scope>): <subject>`

`<Type>` is the nature of the change. Can be a value in (feat, fix, style, refactor, ...)

> (feat: feature, fix: correction, style: appearance correction, refactor: rewriting of the article, ...)

`<scope>` is the business domain on which the change is done

`<subject>` is a short description of the change
 
 
 * E.g. "style(Business Data): fix display of the supported RDBMS matrix table"
 * E.g. "feat(APIs): provide code sample to show usage of Living Application creation"
 * E.g. "feat(dates): describe step by step tutorial on Date types in Bonita"

#### Description

If needed, description allow a more detailed explanation on why this change is being made.
You can write as many lines of description as needed:

* E.g.:
   "This change is part of a larger scale rewriting of how to deal with Dates in Bonita.
   Usage has proven that the feature was not correctly understood.  
   This article tends to start over on the date notions and how to use them."

#### Useful links

Please provide links to any relevant tickets, articles or other resources if available

* E.g. : References [JIRA_ISSUE_ID](https://bonitasoft.atlassian.net/browse/JIRA_ISSUE_ID)

