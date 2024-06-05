package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import java.util.UUID

class GovNotifyEmailServiceTest {
  private val today = LocalDate.now()
  private val notificationId = UUID.randomUUID()
  private val sendEmailResponse: SendEmailResponse = mock { on { notificationId } doReturn notificationId }
  private val client: NotificationClient =
    mock { on { sendEmail(any(), any(), any(), anyOrNull()) } doReturn sendEmailResponse }
  private val emailTemplates = EmailTemplates(
    newCourtBookingOwner = "template 1",
    newCourtBookingPrisonCourtEmail = "template 2",
    newCourtBookingPrisonNoCourtEmail = "template 3",
  )

  private val service = GovNotifyEmailService(client, emailTemplates)

  @Test
  fun `should send court new booking email and return a notification ID`() {
    val result = service.send(
      NewCourtBookingEmail(
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
      NewCourtBookingEmail(
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
  fun `should throw error for unsupported email type`() {
    val error = assertThrows<RuntimeException> { service.send(UnsupportedEmail) }

    error.message isEqualTo "Unsupported email type UnsupportedEmail."
  }

  private object UnsupportedEmail : Email("", "", "", "", LocalDate.now())

  @Test
  fun `should propagate client error when client fails to send email`() {
    val exception = NotificationClientException("Failed to send email.")

    whenever(client.sendEmail(any(), any(), any(), anyOrNull())) doThrow exception

    val result = service.send(
      NewCourtBookingEmail(
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
