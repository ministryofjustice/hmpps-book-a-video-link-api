package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
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
import java.util.UUID

@TestConfiguration
class TestEmailConfiguration {
  @Bean
  fun emailService() =
    EmailService { email ->
      when (email) {
        is NewCourtBookingUserEmail -> Result.success(UUID.randomUUID() to "new court booking user template id")
        is NewCourtBookingCourtEmail -> Result.success(UUID.randomUUID() to "new court booking court template id")
        is NewCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "new court booking prison template id with email address")
        is NewCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "new court booking prison template id no email address")
        is AmendedCourtBookingUserEmail -> Result.success(UUID.randomUUID() to "amended court booking user template id")
        is AmendedCourtBookingCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking court template id")
        is AmendedCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking prison template id with email address")
        is AmendedCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking prison template id no email address")
        is CancelledCourtBookingUserEmail -> Result.success(UUID.randomUUID() to "cancelled court booking user template id")
        is CancelledCourtBookingCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking user template id")
        is CancelledCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking prison template id with email address")
        is CancelledCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking prison template id no email address")
        is CourtBookingRequestUserEmail -> Result.success(UUID.randomUUID() to "requested court booking user template id")
        is CourtBookingRequestPrisonCourtEmail -> Result.success(UUID.randomUUID() to "requested court booking prison template id with email address")
        is CourtBookingRequestPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "requested court booking prison template id with no email address")
        is ProbationBookingRequestUserEmail -> Result.success(UUID.randomUUID() to "requested probation booking user template id")
        is ProbationBookingRequestPrisonProbationTeamEmail -> Result.success(UUID.randomUUID() to "requested probation booking prison template id with email address")
        is ProbationBookingRequestPrisonNoProbationTeamEmail -> Result.success(UUID.randomUUID() to "requested probation booking prison template id with no email address")
        is NewProbationBookingUserEmail -> Result.success(UUID.randomUUID() to "new probation booking user template id")
        is NewProbationBookingPrisonProbationEmail -> Result.success(UUID.randomUUID() to "new probation booking prison template id with email address")
        is NewProbationBookingPrisonNoProbationEmail -> Result.success(UUID.randomUUID() to "new probation booking prison template id no email address")
        is AmendedProbationBookingUserEmail -> Result.success(UUID.randomUUID() to "amended probation booking user template id")
        is AmendedProbationBookingPrisonProbationEmail -> Result.success(UUID.randomUUID() to "amended probation booking template id with email address")
        is AmendedProbationBookingPrisonNoProbationEmail -> Result.success(UUID.randomUUID() to "amended probation booking template id no email address")
        is AmendedProbationBookingProbationEmail -> Result.success(UUID.randomUUID() to "amended probation booking probation template id")
        is CancelledProbationBookingUserEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking user template id")
        is CancelledProbationBookingProbationEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking probation template id")
        is CancelledProbationBookingPrisonProbationEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking template id with email address")
        is CancelledProbationBookingPrisonNoProbationEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking template id no email address")
        is CourtHearingLinkReminderEmail -> Result.success(UUID.randomUUID() to "court hearing link reminder template id")
        else -> throw RuntimeException("Unsupported email in test email configuration: ${email.javaClass.simpleName}")
      }
    }
}
