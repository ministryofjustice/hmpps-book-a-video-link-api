package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType

val EXTERNAL_USER = user("TEST USER")
val PRISON_USER = user(username = "PRISON USER", userType = UserType.PRISON)
