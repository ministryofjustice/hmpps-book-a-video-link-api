package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ProbationBookingRequestPrisonProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ProbationBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingPrisonNoCourtEmail

class EmailTemplatesTest {

  private val emailTypes = mapOf(
    NewCourtBookingUserEmail::class.java to "1",
    NewCourtBookingPrisonCourtEmail::class.java to "2",
    NewCourtBookingPrisonNoCourtEmail::class.java to "3",
    AmendedCourtBookingUserEmail::class.java to "4",
    AmendedCourtBookingPrisonCourtEmail::class.java to "5",
    AmendedCourtBookingPrisonNoCourtEmail::class.java to "6",
    CancelledCourtBookingUserEmail::class.java to "7",
    CancelledCourtBookingPrisonCourtEmail::class.java to "8",
    CancelledCourtBookingPrisonNoCourtEmail::class.java to "9",
    CourtBookingRequestUserEmail::class.java to "10",
    CourtBookingRequestPrisonCourtEmail::class.java to "11",
    CourtBookingRequestPrisonNoCourtEmail::class.java to "12",
    ProbationBookingRequestUserEmail::class.java to "13",
    ProbationBookingRequestPrisonProbationTeamEmail::class.java to "14",
    ProbationBookingRequestPrisonNoProbationTeamEmail::class.java to "15",
    TransferredCourtBookingCourtEmail::class.java to "16",
    TransferredCourtBookingPrisonCourtEmail::class.java to "17",
    TransferredCourtBookingPrisonNoCourtEmail::class.java to "18",
    ReleasedCourtBookingCourtEmail::class.java to "19",
    ReleasedCourtBookingPrisonCourtEmail::class.java to "20",
    ReleasedCourtBookingPrisonNoCourtEmail::class.java to "21",
  )

  private val templates = EmailTemplates(
    newCourtBookingUser = "1",
    newCourtBookingPrisonCourtEmail = "2",
    newCourtBookingPrisonNoCourtEmail = "3",
    amendedCourtBookingUser = "4",
    amendedCourtBookingPrisonCourtEmail = "5",
    amendedCourtBookingPrisonNoCourtEmail = "6",
    cancelledCourtBookingUser = "7",
    cancelledCourtBookingPrisonCourtEmail = "8",
    cancelledCourtBookingPrisonNoCourtEmail = "9",
    courtBookingRequestUser = "10",
    courtBookingRequestPrisonCourtEmail = "11",
    courtBookingRequestPrisonNoCourtEmail = "12",
    probationBookingRequestUser = "13",
    probationBookingRequestPrisonProbationTeamEmail = "14",
    probationBookingRequestPrisonNoProbationTeamEmail = "15",
    transferCourtBookingCourt = "16",
    transferCourtBookingPrisonCourtEmail = "17",
    transferCourtBookingPrisonNoCourtEmail = "18",
    releaseCourtBookingCourt = "19",
    releaseCourtBookingPrisonCourtEmail = "20",
    releaseCourtBookingPrisonNoCourtEmail = "21",
  )

  @Test
  fun `should be a matching template for each email type`() {
    emailTypes.forEach {
      templates.templateFor(it.key) isEqualTo it.value
    }
  }
}
