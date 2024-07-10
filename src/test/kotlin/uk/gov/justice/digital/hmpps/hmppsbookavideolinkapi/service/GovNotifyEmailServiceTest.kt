package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

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
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import uk.gov.service.notify.SendEmailResponse
import java.time.LocalDate
import java.util.*

class GovNotifyEmailServiceTest {
  private val today = LocalDate.now()
  private val notificationId = UUID.randomUUID()
  private val sendEmailResponse: SendEmailResponse = mock { on { notificationId } doReturn notificationId }
  private val client: NotificationClient =
    mock { on { sendEmail(any(), any(), any(), anyOrNull()) } doReturn sendEmailResponse }
  private val emailTemplates = EmailTemplates(
    newCourtBookingUser = "template 1",
    newCourtBookingPrisonCourtEmail = "template 2",
    newCourtBookingPrisonNoCourtEmail = "template 3",
    amendedCourtBookingUser = "template 4",
    amendedCourtBookingPrisonCourtEmail = "template 5",
    amendedCourtBookingPrisonNoCourtEmail = "template 6",
    cancelledCourtBookingUser = "template 7",
    cancelledCourtBookingPrisonCourtEmail = "template 8",
    cancelledCourtBookingPrisonNoCourtEmail = "template 9",
    courtBookingRequestUser = "template 10",
    courtBookingRequestPrisonCourtEmail = "template 11",
    courtBookingRequestPrisonNoCourtEmail = "template 12",
    probationBookingRequestUser = "template 13",
    probationBookingRequestPrisonProbationTeamEmail = "template 14",
    probationBookingRequestPrisonNoProbationTeamEmail = "template 15",
    transferCourtBookingCourt = "template 16",
    transferCourtBookingPrisonCourtEmail = "template 17",
    transferCourtBookingPrisonNoCourtEmail = "template 18",
    releaseCourtBookingCourt = "template 19",
    releaseCourtBookingPrisonCourtEmail = "template 20",
    releaseCourtBookingPrisonNoCourtEmail = "template 21",
  )

  private val service = GovNotifyEmailService(client, emailTemplates)

  @Test
  fun `should send court new booking email and return a notification ID`() {
    val result = service.send(
      NewCourtBookingUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        date = today,
        userName = "username",
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 1")

    verify(client).sendEmail(
      "template 1",
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
      ),
      null,
    )
  }

  @Test
  fun `should send court new booking email with no pre and post appoint or no comments, and return a notification ID`() {
    val result = service.send(
      NewCourtBookingUserEmail(
        address = "recipient@emailaddress.com",
        prisonerFirstName = "builder",
        prisonerLastName = "bob",
        prisonerNumber = "123456",
        date = today,
        userName = "username",
        comments = null,
        preAppointmentInfo = null,
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = null,
        court = "the court",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 1")

    verify(client).sendEmail(
      "template 1",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        courtEmailAddress = "court@emailaddress.com",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 2")

    verify(client).sendEmail(
      "template 2",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 3")

    verify(client).sendEmail(
      "template 3",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 4")

    verify(client).sendEmail(
      "template 4",
      "recipient@emailaddress.com",
      mapOf(
        "date" to today.toMediumFormatStyle(),
        "prisonerName" to "builder bob",
        "offenderNo" to "123456",
        "comments" to "comments for bob",
        "userName" to "username",
        "court" to "the court",
        "preAppointmentInfo" to "bobs pre-appointment info",
        "mainAppointmentInfo" to "bobs main appointment info",
        "postAppointmentInfo" to "bob post appointment info",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        courtEmailAddress = "court@emailaddress.com",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 5")

    verify(client).sendEmail(
      "template 5",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 6")

    verify(client).sendEmail(
      "template 6",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 7")

    verify(client).sendEmail(
      "template 7",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        courtEmailAddress = "court@emailaddress.com",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 8")

    verify(client).sendEmail(
      "template 8",
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
        date = today,
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 9")

    verify(client).sendEmail(
      "template 9",
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
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 10")

    verify(client).sendEmail(
      "template 10",
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
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 11")

    verify(client).sendEmail(
      "template 11",
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
      ),
    )

    result.getOrThrow() isEqualTo Pair(notificationId, "template 12")

    verify(client).sendEmail(
      "template 12",
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

    result.getOrThrow() isEqualTo Pair(notificationId, "template 13")

    verify(client).sendEmail(
      "template 13",
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

    result.getOrThrow() isEqualTo Pair(notificationId, "template 14")

    verify(client).sendEmail(
      "template 14",
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

    result.getOrThrow() isEqualTo Pair(notificationId, "template 15")

    verify(client).sendEmail(
      "template 15",
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
        date = LocalDate.now(),
        userName = "username",
        comments = "comments for bob",
        preAppointmentInfo = "bobs pre-appointment info",
        mainAppointmentInfo = "bobs main appointment info",
        postAppointmentInfo = "bob post appointment info",
        court = "the court",
        prison = "the prison",
      ),
    )

    result.isSuccess isBool false
    result.isFailure isBool true
    result.exceptionOrNull() isEqualTo exception
  }
}
