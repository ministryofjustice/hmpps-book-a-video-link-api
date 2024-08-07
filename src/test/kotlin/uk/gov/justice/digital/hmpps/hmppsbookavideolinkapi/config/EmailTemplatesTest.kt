package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingUserEmail
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
    NewCourtBookingUserEmail::class.java to "newCourtBookingUser",
    NewCourtBookingCourtEmail::class.java to "newCourtBookingCourt",
    NewCourtBookingPrisonCourtEmail::class.java to "newCourtBookingPrisonCourtEmail",
    NewCourtBookingPrisonNoCourtEmail::class.java to "newCourtBookingPrisonNoCourtEmail",
    AmendedCourtBookingUserEmail::class.java to "amendedCourtBookingUser",
    AmendedCourtBookingPrisonCourtEmail::class.java to "amendedCourtBookingPrisonCourtEmail",
    AmendedCourtBookingPrisonNoCourtEmail::class.java to "amendedCourtBookingPrisonNoCourtEmail",
    CancelledCourtBookingUserEmail::class.java to "cancelledCourtBookingUser",
    CancelledCourtBookingPrisonCourtEmail::class.java to "cancelledCourtBookingPrisonCourtEmail",
    CancelledCourtBookingPrisonNoCourtEmail::class.java to "cancelledCourtBookingPrisonNoCourtEmail",
    CourtBookingRequestUserEmail::class.java to "courtBookingRequestUser",
    CourtBookingRequestPrisonCourtEmail::class.java to "courtBookingRequestPrisonCourtEmail",
    CourtBookingRequestPrisonNoCourtEmail::class.java to "courtBookingRequestPrisonNoCourtEmail",
    ProbationBookingRequestUserEmail::class.java to "probationBookingRequestUser",
    ProbationBookingRequestPrisonProbationTeamEmail::class.java to "probationBookingRequestPrisonProbationTeamEmail",
    ProbationBookingRequestPrisonNoProbationTeamEmail::class.java to "probationBookingRequestPrisonNoProbationTeamEmail",
    TransferredCourtBookingCourtEmail::class.java to "transferCourtBookingCourt",
    TransferredCourtBookingPrisonCourtEmail::class.java to "transferCourtBookingPrisonCourtEmail",
    TransferredCourtBookingPrisonNoCourtEmail::class.java to "transferCourtBookingPrisonNoCourtEmail",
    ReleasedCourtBookingCourtEmail::class.java to "releaseCourtBookingCourt",
    ReleasedCourtBookingPrisonCourtEmail::class.java to "releaseCourtBookingPrisonCourtEmail",
    ReleasedCourtBookingPrisonNoCourtEmail::class.java to "releaseCourtBookingPrisonNoCourtEmail",
    NewProbationBookingUserEmail::class.java to "newProbationBookingUser",
    NewProbationBookingPrisonProbationEmail::class.java to "newProbationBookingPrisonProbationEmail",
    NewProbationBookingPrisonNoProbationEmail::class.java to "newProbationBookingPrisonNoProbationEmail",
  )

  private val templates = EmailTemplates(
    newCourtBookingUser = "newCourtBookingUser",
    newCourtBookingCourt = "newCourtBookingCourt",
    newCourtBookingPrisonCourtEmail = "newCourtBookingPrisonCourtEmail",
    newCourtBookingPrisonNoCourtEmail = "newCourtBookingPrisonNoCourtEmail",
    amendedCourtBookingUser = "amendedCourtBookingUser",
    amendedCourtBookingPrisonCourtEmail = "amendedCourtBookingPrisonCourtEmail",
    amendedCourtBookingPrisonNoCourtEmail = "amendedCourtBookingPrisonNoCourtEmail",
    cancelledCourtBookingUser = "cancelledCourtBookingUser",
    cancelledCourtBookingPrisonCourtEmail = "cancelledCourtBookingPrisonCourtEmail",
    cancelledCourtBookingPrisonNoCourtEmail = "cancelledCourtBookingPrisonNoCourtEmail",
    courtBookingRequestUser = "courtBookingRequestUser",
    courtBookingRequestPrisonCourtEmail = "courtBookingRequestPrisonCourtEmail",
    courtBookingRequestPrisonNoCourtEmail = "courtBookingRequestPrisonNoCourtEmail",
    probationBookingRequestUser = "probationBookingRequestUser",
    probationBookingRequestPrisonProbationTeamEmail = "probationBookingRequestPrisonProbationTeamEmail",
    probationBookingRequestPrisonNoProbationTeamEmail = "probationBookingRequestPrisonNoProbationTeamEmail",
    transferCourtBookingCourt = "transferCourtBookingCourt",
    transferCourtBookingPrisonCourtEmail = "transferCourtBookingPrisonCourtEmail",
    transferCourtBookingPrisonNoCourtEmail = "transferCourtBookingPrisonNoCourtEmail",
    releaseCourtBookingCourt = "releaseCourtBookingCourt",
    releaseCourtBookingPrisonCourtEmail = "releaseCourtBookingPrisonCourtEmail",
    releaseCourtBookingPrisonNoCourtEmail = "releaseCourtBookingPrisonNoCourtEmail",
    newProbationBookingUser = "newProbationBookingUser",
    newProbationBookingPrisonProbationEmail = "newProbationBookingPrisonProbationEmail",
    newProbationBookingPrisonNoProbationEmail = "newProbationBookingPrisonNoProbationEmail",
    amendedProbationBookingUser = "amendedProbationBookingUser",
    amendedProbationBookingPrisonProbationEmail = "amendedProbationBookingPrisonProbationEmail",
    amendedProbationBookingPrisonNoProbationEmail = "amendedProbationBookingPrisonNoProbationEmail",
    amendedProbationBookingProbationEmail = "amendedProbationBookingProbationEmail",
  )

  @Test
  fun `should be a matching template for each email type`() {
    emailTypes.forEach {
      templates.templateFor(it.key) isEqualTo it.value
    }
  }

  @Test
  fun `should be a distinct template for each email type`() {
    EmailTemplates(
      "a",
      "b",
      "c",
      "d",
      "e",
      "f",
      "g",
      "h",
      "i",
      "j",
      "k",
      "l",
      "m",
      "n",
      "o",
      "p",
      "q",
      "r",
      "s",
      "t",
      "u",
      "v",
      "w",
      "x",
      "y",
      "z",
      "1",
      "2",
      "3",
    )

    val error = assertThrows<IllegalArgumentException> {
      EmailTemplates(
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i",
        "j",
        "k",
        "l",
        "m",
        "n",
        "o",
        "p",
        "q",
        "r",
        "s",
        "t",
        "u",
        "v",
        "w",
        "x",
        "y",
        "z",
        "1",
        "duplicate",
        "duplicate",
      )
    }

    error.message isEqualTo "Template IDs must be unique."
  }
}
