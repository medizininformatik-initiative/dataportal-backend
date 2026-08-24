# Development

## Release Checklist

This project uses a single long-lived branch, `main`. There is no `develop` branch; all release
branches are cut from `main` and merged back into `main`.

* create a release branch called `release/v<version>` like `release/v1.1.0`, branched off `main`
* rename every occurrence of the old version, say `1.0.0-SNAPSHOT` into the new version, say `1.1.0`
* rename every occurrence of old Docker images like `ghcr.io/medizininformatik-initiative/dataportal-backend:1.0.0` into the new image, say `ghcr.io/medizininformatik-initiative/dataportal-backend:1.1.0`
* update the CHANGELOG based on the milestone
* create a commit with the title `Release v<version>`
* create a PR from the release branch into `main`
* merge that PR (after proper review)
* create and push a tag called `v<version>` like `v1.1.0` on `main` at the merge commit
* on `main`, bump the version in the POM to the next SNAPSHOT version, which usually increments the
  minor version, e.g. `1.2.0-SNAPSHOT`, and commit directly to `main`
* create release notes on GitHub
* delete the release branch after it has been successfully merged
