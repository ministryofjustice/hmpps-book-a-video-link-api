package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtHearingLinkReminderEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingProbationEmail

class EmailTemplatesTest {

  private val emailTypes = mapOf(
    NewCourtBookingUserEmail::class.java to "newCourtBookingUser",
    NewCourtBookingCourtEmail::class.java to "newCourtBookingCourt",
    NewCourtBookingPrisonCourtEmail::class.java to "newCourtBookingPrisonCourtEmail",
    NewCourtBookingPrisonNoCourtEmail::class.java to "newCourtBookingPrisonNoCourtEmail",
    AmendedCourtBookingUserEmail::class.java to "amendedCourtBookingUser",
    AmendedCourtBookingCourtEmail::class.java to "amendedCourtBookingCourtEmail",
    AmendedCourtBookingPrisonCourtEmail::class.java to "amendedCourtBookingPrisonCourtEmail",
    AmendedCourtBookingPrisonNoCourtEmail::class.java to "amendedCourtBookingPrisonNoCourtEmail",
    CancelledCourtBookingUserEmail::class.java to "cancelledCourtBookingUser",
    CancelledCourtBookingCourtEmail::class.java to "cancelledCourtBookingCourtEmail",
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
    AmendedProbationBookingUserEmail::class.java to "amendedProbationBookingUser",
    AmendedProbationBookingPrisonProbationEmail::class.java to "amendedProbationBookingPrisonProbationEmail",
    AmendedProbationBookingPrisonNoProbationEmail::class.java to "amendedProbationBookingPrisonNoProbationEmail",
    AmendedProbationBookingProbationEmail::class.java to "amendedProbationBookingProbationEmail",
    CancelledProbationBookingUserEmail::class.java to "cancelledProbationBookingUser",
    CancelledProbationBookingProbationEmail::class.java to "cancelledProbationBookingProbationEmail",
    CancelledProbationBookingPrisonProbationEmail::class.java to "cancelledProbationBookingPrisonProbationEmail",
    CancelledProbationBookingPrisonNoProbationEmail::class.java to "cancelledProbationBookingPrisonNoProbationEmail",
    ReleasedProbationBookingProbationEmail::class.java to "releasedProbationBookingProbationEmail",
    ReleasedProbationBookingPrisonProbationEmail::class.java to "releaseProbationBookingPrisonProbationEmail",
    ReleasedProbationBookingPrisonNoProbationEmail::class.java to "releaseProbationBookingPrisonNoProbationEmail",
    TransferredProbationBookingProbationEmail::class.java to "transferProbationBookingProbation",
    TransferredProbationBookingPrisonProbationEmail::class.java to "transferProbationBookingPrisonProbationEmail",
    TransferredProbationBookingPrisonNoProbationEmail::class.java to "transferProbationBookingPrisonNoProbationEmail",
    CourtHearingLinkReminderEmail::class.java to "courtHearingLinkReminderEmail"
  )

  private val templates = EmailTemplates(
    newCourtBookingUser = "newCourtBookingUser",
    newCourtBookingCourt = "newCourtBookingCourt",
    newCourtBookingPrisonCourtEmail = "newCourtBookingPrisonCourtEmail",
    newCourtBookingPrisonNoCourtEmail = "newCourtBookingPrisonNoCourtEmail",
    amendedCourtBookingUser = "amendedCourtBookingUser",
    amendedCourtBookingCourtEmail = "amendedCourtBookingCourtEmail",
    amendedCourtBookingPrisonCourtEmail = "amendedCourtBookingPrisonCourtEmail",
    amendedCourtBookingPrisonNoCourtEmail = "amendedCourtBookingPrisonNoCourtEmail",
    cancelledCourtBookingUser = "cancelledCourtBookingUser",
    cancelledCourtBookingCourtEmail = "cancelledCourtBookingCourtEmail",
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
    cancelledProbationBookingUser = "cancelledProbationBookingUser",
    cancelledProbationBookingProbationEmail = "cancelledProbationBookingProbationEmail",
    cancelledProbationBookingPrisonProbationEmail = "cancelledProbationBookingPrisonProbationEmail",
    cancelledProbationBookingPrisonNoProbationEmail = "cancelledProbationBookingPrisonNoProbationEmail",
    releaseProbationBookingProbation = "releasedProbationBookingProbationEmail",
    releaseProbationBookingPrisonProbationEmail = "releaseProbationBookingPrisonProbationEmail",
    releaseProbationBookingPrisonNoProbationEmail = "releaseProbationBookingPrisonNoProbationEmail",
    transferProbationBookingProbation = "transferProbationBookingProbation",
    transferProbationBookingPrisonProbationEmail = "transferProbationBookingPrisonProbationEmail",
    transferProbationBookingPrisonNoProbationEmail = "transferProbationBookingPrisonNoProbationEmail",
    courtHearingLinkReminderEmail = "courtHearingLinkReminderEmail",
  )

  @Test
  fun `should be a matching template for each email type`() {
    EmailTemplates::class.constructors.single().parameters.size isEqualTo emailTypes.size

    emailTypes.forEach {
      (templates.templateFor(it.key) ?: "${it.key} not found") isEqualTo it.value
    }
  }

  @Test
  fun `should be a unique template identifier for each email type`() {
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
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "10",
      "11",
      "12",
      "13",
      "14",
      "15",
      courtHearingLinkReminderEmail = "16",
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
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "10",
        "11",
        "12",
        "13",
        "duplicate",
        "duplicate",
        courtHearingLinkReminderEmail = "14",
      )
    }

    error.message isEqualTo "Template IDs must be unique."
  }
}
