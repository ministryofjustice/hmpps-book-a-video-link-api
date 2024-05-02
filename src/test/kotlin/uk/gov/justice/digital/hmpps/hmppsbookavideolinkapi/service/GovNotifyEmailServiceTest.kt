package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailTemplates
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health.isEqualTo
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

class GovNotifyEmailServiceTest {
  private val client: NotificationClient = mock()

  private val emailTemplates = EmailTemplates(
    appointmentCancelledCourt = "template 1",
    appointmentCancelledPrison = "template 2",
    appointmentCancelledPrisonNoCourt = "template 3",
    offenderReleasedCourt = "template 4",
    offenderReleasedPrison = "template 5",
    offenderReleasedPrisonNoCourt = "template 6",
    offenderTransferredCourt = "template 7",
    offenderTransferredPrison = "template 8",
    offenderTransferredPrisonNoCourt = "template 9",
  )

  private val service = GovNotifyEmailService(client, emailTemplates)

  @Test
  fun `should send appointment cancelled court email`() {
    service.send(AppointmentCancelledCourtEmail("address 1", mapOf("a" to "b")))

    verify(client).sendEmail("template 1", "address 1", mapOf("a" to "b"), null)
  }

  @Test
  fun `should send appointment cancelled prison email`() {
    service.send(AppointmentCancelledPrisonEmail("address 2", mapOf("b" to "c")))

    verify(client).sendEmail("template 2", "address 2", mapOf("b" to "c"), null)
  }

  @Test
  fun `should send appointment cancelled prison no court email`() {
    service.send(AppointmentCancelledPrisonNoCourtEmail("address 3", mapOf("c" to "d")))

    verify(client).sendEmail("template 3", "address 3", mapOf("c" to "d"), null)
  }

  @Test
  fun `should send offender released court email`() {
    service.send(OffenderReleasedCourtEmail("address 4", mapOf("d" to "e")))

    verify(client).sendEmail("template 4", "address 4", mapOf("d" to "e"), null)
  }

  @Test
  fun `should send offender released prison email`() {
    service.send(OffenderReleasedPrisonEmail("address 5", mapOf("e" to "f")))

    verify(client).sendEmail("template 5", "address 5", mapOf("e" to "f"), null)
  }

  @Test
  fun `should send offender released prison no court email`() {
    service.send(OffenderReleasedPrisonNoCourtEmail("address 6", mapOf("f" to "g")))

    verify(client).sendEmail("template 6", "address 6", mapOf("f" to "g"), null)
  }

  @Test
  fun `should send offender transferred court email`() {
    service.send(OffenderTransferredCourtEmail("address 7", mapOf("g" to "h")))

    verify(client).sendEmail("template 7", "address 7", mapOf("g" to "h"), null)
  }

  @Test
  fun `should send offender transferred prison email`() {
    service.send(OffenderTransferredPrisonEmail("address 8", mapOf("h" to "i")))

    verify(client).sendEmail("template 8", "address 8", mapOf("h" to "i"), null)
  }

  @Test
  fun `should send offender transferred prison no court email`() {
    service.send(OffenderTransferredPrisonNoCourtEmail("address 9", mapOf("i" to "j")))

    verify(client).sendEmail("template 9", "address 9", mapOf("i" to "j"), null)
  }

  @Test
  fun `should throw error for unsupported email type`() {
    val error = assertThrows<RuntimeException> { service.send(UnsupportedEmail) }

    error.message isEqualTo "Unsupported email type UnsupportedEmail."
  }

  private object UnsupportedEmail : Email() {
    override val address: String get() = ""
    override val personalisation: Map<String, String?> get() = mapOf()
  }

  @Test
  fun `should propagate client error when client fails to send email`() {
    whenever(client.sendEmail(any(), any(), any(), anyOrNull())) doThrow NotificationClientException("Failed to send email.")

    val error = assertThrows<NotificationClientException> { service.send(OffenderTransferredPrisonNoCourtEmail("address 9", mapOf("i" to "j"))) }

    error.message isEqualTo "Failed to send email."
  }
}
