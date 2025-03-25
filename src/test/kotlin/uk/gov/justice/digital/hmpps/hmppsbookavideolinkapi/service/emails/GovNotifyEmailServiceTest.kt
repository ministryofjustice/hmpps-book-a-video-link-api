package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestUserEmail
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import uk.gov.service.notify.SendEmailResponse
import java.time.LocalDate
import java.util.UUID

class GovNotifyEmailServiceTest {
  private val today = LocalDate.now()
  private val notificationId = UUID.randomUUID()
  private val sendEmailResponse: SendEmailResponse = mock { on { notificationId } doReturn notificationId }
  private val client: NotificationClient = mock { on { sendEmail(any(), any(), any(), anyOrNull()) } doReturn sendEmailResponse }
  private val emailTemplates = EmailTemplates(
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
    releaseProbationBookingProbation = "releaseProbationBookingProbation",
    releaseProbationBookingPrisonProbationEmail = "releaseProbationBookingPrisonProbationEmail",
    releaseProbationBookingPrisonNoProbationEmail = "releaseProbationBookingPrisonNoProbationEmail",
    transferProbationBookingProbation = "transferProbationBookingProbation",
    transferProbationBookingPrisonProbationEmail = "transferProbationBookingPrisonProbationEmail",
    transferProbationBookingPrisonNoProbationEmail = "transferProbationBookingPrisonNoProbationEmail",
    courtHearingLinkReminderEmail = "courtHearingLinkReminderEmail",
  )

  private val service = GovNotifyEmailService(client, emailTemplates, "http://localhost:3000")

  @Test
  fun `should send user new booking email and return a notification ID`() {
    val result = service.send(
      NewCourtBookingUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        userName = "username",
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "newCourtBookingUser")

    verify(client).sendEmail(
      "newCourtBookingUser",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "userName" to "username",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send user new booking email with no pre and post appointment, no comments, and no court hearing link, and return a notification ID`() {
    val result = service.send(
      NewCourtBookingUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        userName = "username",
        comments = null,
        preAppointmentInfo = null,
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = null,
        court = "the court",
        prison = "the prison",
        courtHearingLink = null,
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "newCourtBookingUser")

    verify(client).sendEmail(
      "newCourtBookingUser",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "None entered",
        "userName" to "username",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "Not required",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "Not required",
        "courtHearingLink" to "Not yet known",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send court new booking with court email and return a notification ID`() {
    val result = service.send(
      NewCourtBookingCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "newCourtBookingCourt")

    verify(client).sendEmail(
      "newCourtBookingCourt",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison new booking with court email and return a notification ID`() {
    val result = service.send(
      NewCourtBookingPrisonCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        courtEmailAddress = "court@emailaddress.com",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "newCourtBookingPrisonCourtEmail")

    verify(client).sendEmail(
      "newCourtBookingPrisonCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "court" to "the court",
        "courtEmailAddress" to "court@emailaddress.com",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison new booking with no court email and return a notification ID`() {
    val result = service.send(
      NewCourtBookingPrisonNoCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "newCourtBookingPrisonNoCourtEmail")

    verify(client).sendEmail(
      "newCourtBookingPrisonNoCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send court amended booking email and return a notification ID`() {
    val result = service.send(
      AmendedCourtBookingUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        userName = "username",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "amendedCourtBookingUser")

    verify(client).sendEmail(
      "amendedCourtBookingUser",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "userName" to "username",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison amended booking from court with email and return a notification ID`() {
    val result = service.send(
      AmendedCourtBookingPrisonCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        courtEmailAddress = "court@emailaddress.com",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "amendedCourtBookingPrisonCourtEmail")

    verify(client).sendEmail(
      "amendedCourtBookingPrisonCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "court" to "the court",
        "courtEmailAddress" to "court@emailaddress.com",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison amended booking from court with no email and return a notification ID`() {
    val result = service.send(
      AmendedCourtBookingPrisonNoCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "amendedCourtBookingPrisonNoCourtEmail")

    verify(client).sendEmail(
      "amendedCourtBookingPrisonNoCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send court cancelled booking email and return a notification ID`() {
    val result = service.send(
      CancelledCourtBookingUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        userName = "username",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "cancelledCourtBookingUser")

    verify(client).sendEmail(
      "cancelledCourtBookingUser",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "userName" to "username",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison cancelled booking from court with email and return a notification ID`() {
    val result = service.send(
      CancelledCourtBookingPrisonCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        courtEmailAddress = "court@emailaddress.com",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "cancelledCourtBookingPrisonCourtEmail")

    verify(client).sendEmail(
      "cancelledCourtBookingPrisonCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "court" to "the court",
        "courtEmailAddress" to "court@emailaddress.com",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison cancelled booking from court with no email and return a notification ID`() {
    val result = service.send(
      CancelledCourtBookingPrisonNoCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "cancelledCourtBookingPrisonNoCourtEmail")

    verify(client).sendEmail(
      "cancelledCourtBookingPrisonNoCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "court" to "the court",
        "prison" to "the prison",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send court booking request user email and return a notification ID`() {
    val result = service.send(
      CourtBookingRequestUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        userName = "username",
        date = today,
        comments = "comments for bob",
        hearingType = "Appeal",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "courtBookingRequestUser")

    verify(client).sendEmail(
      "courtBookingRequestUser",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "dateOfBirth" to "1 Jan 1970",
        "comments" to "comments for bob",
        "userName" to "username",
        "court" to "the court",
        "prison" to "the prison",
        "hearingType" to "Appeal",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison booking request from court with email and return a notification ID`() {
    val result = service.send(
      CourtBookingRequestPrisonCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        date = today,
        comments = "comments for bob",
        hearingType = "Appeal",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        courtEmailAddress = "court@emailaddress.com",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "courtBookingRequestPrisonCourtEmail")

    verify(client).sendEmail(
      "courtBookingRequestPrisonCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "dateOfBirth" to "1 Jan 1970",
        "comments" to "comments for bob",
        "court" to "the court",
        "courtEmailAddress" to "court@emailaddress.com",
        "prison" to "the prison",
        "hearingType" to "Appeal",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison booking request from court with no email and return a notification ID`() {
    val result = service.send(
      CourtBookingRequestPrisonNoCourtEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        date = today,
        comments = "comments for bob",
        hearingType = "Appeal",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "courtBookingRequestPrisonNoCourtEmail")

    verify(client).sendEmail(
      "courtBookingRequestPrisonNoCourtEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "dateOfBirth" to "1 Jan 1970",
        "comments" to "comments for bob",
        "court" to "the court",
        "prison" to "the prison",
        "hearingType" to "Appeal",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
        "courtHearingLink" to "https://video.link.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send probation booking request user email and return a notification ID`() {
    val result = service.send(
      ProbationBookingRequestUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        userName = "username",
        date = today,
        comments = "comments for bob",
        meetingType = "Recall report",
        appointmentInfo = "bobs appointment info",
        probationTeam = "the probation team",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "probationBookingRequestUser")

    verify(client).sendEmail(
      "probationBookingRequestUser",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "dateOfBirth" to "1 Jan 1970",
        "comments" to "comments for bob",
        "userName" to "username",
        "probationTeam" to "the probation team",
        "prison" to "the prison",
        "meetingType" to "Recall report",
        "appointmentInfo" to "bobs appointment info",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison booking request from probation team with email and return a notification ID`() {
    val result = service.send(
      ProbationBookingRequestPrisonProbationTeamEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        date = today,
        comments = "comments for bob",
        meetingType = "Recall report",
        appointmentInfo = "bobs appointment info",
        probationTeam = "the probation team",
        probationTeamEmailAddress = "probationteam@emailaddress.com",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "probationBookingRequestPrisonProbationTeamEmail")

    verify(client).sendEmail(
      "probationBookingRequestPrisonProbationTeamEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "dateOfBirth" to "1 Jan 1970",
        "comments" to "comments for bob",
        "probationTeam" to "the probation team",
        "probationTeamEmailAddress" to "probationteam@emailaddress.com",
        "prison" to "the prison",
        "meetingType" to "Recall report",
        "appointmentInfo" to "bobs appointment info",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should send prison booking request from probation team with no email and return a notification ID`() {
    val result = service.send(
      ProbationBookingRequestPrisonNoProbationTeamEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        dateOfBirth = LocalDate.of(1970, 1, 1),
        date = today,
        comments = "comments for bob",
        meetingType = "Recall report",
        appointmentInfo = "bobs appointment info",
        probationTeam = "the probation team",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "probationBookingRequestPrisonNoProbationTeamEmail")

    verify(client).sendEmail(
      "probationBookingRequestPrisonNoProbationTeamEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "dateOfBirth" to "1 Jan 1970",
        "comments" to "comments for bob",
        "probationTeam" to "the probation team",
        "prison" to "the prison",
        "meetingType" to "Recall report",
        "appointmentInfo" to "bobs appointment info",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }

  @Test
  fun `should throw error for unsupported email type`() {
    val error = service.send(UnsupportedEmail)

    error.isFailure isBool true
    error.exceptionOrNull()?.message isEqualTo "EMAIL: Missing template ID for email type UnsupportedEmail."
  }

  private object UnsupportedEmail : Email("", "", "", LocalDate.now())

  @Test
  fun `should propagate client error when client fails to send email`() {
    val exception = NotificationClientException("Failed to send email.")

    whenever(client.sendEmail(any(), any(), any(), anyOrNull())) doThrow exception

    val result = service.send(
      NewCourtBookingUserEmail(
        address = "address 1",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = LocalDate.now(),
        userName = "username",
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
        courtHearingLink = "https://video.link.com",
      ),
    )

    result.isSuccess isBool false
    result.isFailure isBool true
    result.exceptionOrNull() isEqualTo exception
  }

  @Test
  fun `should send probation user new booking email and return a notification ID`() {
    val result = service.send(
      NewProbationBookingUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        userName = "username",
        comments = "comments for bob",
        appointmentInfo = "bobs appointment info",
        probationTeam = "the probation team",
        prison = "the prison",
        prisonVideoUrl = "prison-video-url",
        probationOfficerName = "probation officer name",
        probationOfficerEmailAddress = "probation.officer@email.address",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "newProbationBookingUser")

    verify(client).sendEmail(
      "newProbationBookingUser",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "comments" to "comments for bob",
        "offenderNo" to "123456",
        "userName" to "username",
        "probationTeam" to "the probation team",
        "prison" to "the prison",
        "appointmentInfo" to "bobs appointment info",
        "frontendDomain" to "http://localhost:3000",
        "prisonVideoUrl" to "prison-video-url",
        "probationOfficerName" to "probation officer name",
        "probationOfficerEmailAddress" to "probation.officer@email.address",
      ),
      null,
    )
  }

  @Test
  fun `should send prison probation new booking email and return a notification ID`() {
    val result = service.send(
      NewProbationBookingPrisonProbationEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        appointmentDate = today,
        comments = "comments for bob",
        appointmentInfo = "bobs appointment info",
        probationTeam = "the probation team",
        prison = "the prison",
        probationEmailAddress = "jim@probation.com",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "newProbationBookingPrisonProbationEmail")

    verify(client).sendEmail(
      "newProbationBookingPrisonProbationEmail",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "comments" to "comments for bob",
        "offenderNo" to "123456",
        "probationTeam" to "the probation team",
        "prison" to "the prison",
        "appointmentInfo" to "bobs appointment info",
        "probationEmailAddress" to "jim@probation.com",
        "frontendDomain" to "http://localhost:3000",
      ),
      null,
    )
  }
}
