package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService

val COURT_USER = courtUser(username = "court_user", name = "Court User", email = "court_user")
val PRISON_USER_BIRMINGHAM = prisonUser()
val PRISON_USER_PENTONVILLE = prisonUser(activeCaseLoadId = PENTONVILLE)
val PRISON_USER_RISLEY = prisonUser(activeCaseLoadId = RISLEY)
val PRISON_USER_WANDSWORTH = prisonUser(activeCaseLoadId = WANDSWORTH)
val PROBATION_USER = probationUser(username = "probation_user", name = "Probation User", email = "probation.user@probation.com")
val SERVICE_USER = UserService.getServiceAsUser()
