package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UNKNOWN_COURT_CODE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UNKNOWN_PROBATION_TEAM_CODE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationUser

class VideoBookingAccessKtTest {
  @Test
  fun `should fail booking check for court user accessing a probation booking`() {
    assertThrows<VideoBookingAccessException> { checkVideoBookingAccess(courtUser(), probationBooking()) }
  }

  @Test
  fun `should fail booking check for probation user accessing a court booking`() {
    assertThrows<VideoBookingAccessException> { checkVideoBookingAccess(probationUser(), courtBooking()) }
  }

  @Test
  fun `should fail booking check for court user accessing an unknown court booking`() {
    assertThrows<VideoBookingAccessException> { checkVideoBookingAccess(courtUser(), courtBooking(court = court(code = UNKNOWN_COURT_CODE))) }
  }

  @Test
  fun `should fail booking check for probation user accessing an unknown probation booking`() {
    assertThrows<VideoBookingAccessException> { checkVideoBookingAccess(probationUser(), probationBooking(probationTeam = probationTeam(code = UNKNOWN_PROBATION_TEAM_CODE))) }
  }

  @Test
  fun `should succeed booking check for court user accessing a court booking`() {
    assertDoesNotThrow { checkVideoBookingAccess(courtUser(), courtBooking()) }
  }

  @Test
  fun `should succeed booking check for probation user accessing a probation booking`() {
    assertDoesNotThrow { checkVideoBookingAccess(probationUser(), probationBooking()) }
  }

  @Test
  fun `should succeed booking check when user is not an external user`() {
    assertDoesNotThrow { checkVideoBookingAccess(PRISON_USER_BIRMINGHAM, courtBooking()) }
    assertDoesNotThrow { checkVideoBookingAccess(SERVICE_USER, courtBooking()) }
    assertDoesNotThrow { checkVideoBookingAccess(PRISON_USER_BIRMINGHAM, probationBooking()) }
    assertDoesNotThrow { checkVideoBookingAccess(SERVICE_USER, probationBooking()) }
  }
}
