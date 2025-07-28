package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.administration.AdministrationEmailService

class NewVideoRoomsJobTest {
  private val administrationEmailService: AdministrationEmailService = mock()
  private val job = NewPrisonVideoRoomsJob(administrationEmailService)

  @Test
  fun `should call the admin service when run`() {
    job.runJob()

    verify(administrationEmailService).sendEmailsForNewPrisonVideoRoom()
  }
}
