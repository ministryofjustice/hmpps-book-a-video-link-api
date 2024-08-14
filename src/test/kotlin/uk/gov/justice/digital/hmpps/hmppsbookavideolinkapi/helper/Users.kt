package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType

val COURT_USER = user(username = "court_user", userType = UserType.EXTERNAL, name = "Court User", email = "court.user@court.com")
val EXTERNAL_USER = user(username = "external_user", userType = UserType.EXTERNAL, name = "External User", email = "external.user@external.com")
val PRISON_USER = user(username = "prison_user", userType = UserType.PRISON, name = "Prison User", email = "prison.user@prison.com")
val PROBATION_USER = user(username = "probation_user", userType = UserType.EXTERNAL, name = "Probation User", email = "probation.user@probation.com")
val SERVICE_USER = user(username = "service_user", userType = UserType.SERVICE, name = "service user")
