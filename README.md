# hmpps-book-a-video-link-api
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-book-a-video-link-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-book-a-video-link-api "Link to report")
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/ministryofjustice/hmpps-book-a-video-link-api/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/ministryofjustice/hmpps-activities-management-api/tree/main)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-book-a-video-link-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-book-a-video-link-api)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://book-a-video-link-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html#/)

API to support the front end service allowing court and probation users to book and manage video link hearings/appointments with people in prison.

## Building the project

Tools required:

* JDK v21+
* Kotlin (Intellij)
* docker
* docker-compose

Useful tools but not essential:

* KUBECTL not essential for building the project but will be needed for other tasks. Can be installed with `brew`.
* [k9s](https://k9scli.io/) a terminal based UI to interact with your Kubernetes clusters. Can be installed with `brew`.
* [jq](https://jqlang.github.io/jq/) a lightweight and flexible command-line JSON processor. Can be installed with `brew`.

## Install gradle and build the project

```
./gradlew
```

```
./gradlew clean build
```

## Running the service

There are two key environment variables needed to run the service. The system client id and secret used to retrieve the OAuth 2.0 access token needed for service to service API calls can be set as local environment variables.
This allows API calls made from this service that do not use the caller's token to successfully authenticate.

Add the following to a local `.env` file in the root folder of this project (_you can extract the credentials from the dev k8s project namespace_).

N.B. you must escape any '$' characters with '\\$'

```
SYSTEM_CLIENT_ID=<system.client.id>
SYSTEM_CLIENT_SECRET=<system.client.secret>
```

Start up the docker dependencies using the docker-compose file in the `hmpps-activities-management-api` service.

```
docker-compose up --remove-orphans
```

There is a script to help, which sets local profiles, port and DB connection properties to the
values required.

```
./run-local.sh
```

## Testing GOV Notify locally

To test Gov Notify emails locally, you just need to add one more variable to your `.env` file.

```
export NOTIFY_API_KEY=<gov.notify.api.key>
```
If you have added it correctly, you will see the log on startup with the following output:

```
Gov Notify emails are enabled
```
