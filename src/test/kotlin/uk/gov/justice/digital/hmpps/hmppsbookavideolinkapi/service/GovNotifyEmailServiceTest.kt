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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health.isEqualTo
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import uk.gov.service.notify.SendEmailResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class GovNotifyEmailServiceTest {
  private val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
  private val notificationId = UUID.randomUUID()
  private val sendEmailResponse: SendEmailResponse = mock { on { notificationId } doReturn notificationId }
  private val client: NotificationClient =
    mock { on { sendEmail(any(), any(), any(), anyOrNull()) } doReturn sendEmailResponse }
  private val emailTemplates = EmailTemplates(
    courtNewBooking = "template 1",
  )

  private val service = GovNotifyEmailService(client, emailTemplates)

  @Test
  fun `should send booking by court email and return a notification ID`() {
    val result = service.send(
      CourtNewBookingEmail(
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

    result.getOrThrow() isEqualTo notificationId

    verify(client).sendEmail(
      "template 1",
      "address 1",
      mapOf(
        "date" to today,
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
      CourtNewBookingEmail(
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
