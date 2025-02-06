package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService

class TestJob(block: () -> Unit = {}) :
  JobDefinition(
    jobType = JobType.COURT_HEARING_LINK_REMINDER,
    block = block,
  )

class JobRunnerTest {

  private val telemetryService: TelemetryService = mock()
  private val runner = JobRunner(telemetryService)

  @Test
  fun `runs job without error`() {
    val telemetryCaptor = argumentCaptor<JobSuccessStandardTelemetryEvent>()
    runner.runJob(TestJob())
    verify(telemetryService).track(telemetryCaptor.capture())
    with(telemetryCaptor.firstValue) {
      properties().get("jobType") isEqualTo JobType.COURT_HEARING_LINK_REMINDER.name
    }
  }

  @Test
  fun `runs job with error`() {
    val telemetryCaptor = argumentCaptor<JobFailureStandardTelemetryEvent>()
    assertThrows<Exception> { runner.runJob(TestJob { throw Exception("Test error") }) }
    verify(telemetryService).track(telemetryCaptor.capture())
    with(telemetryCaptor.firstValue) {
      properties().get("jobType") isEqualTo JobType.COURT_HEARING_LINK_REMINDER.name
    }
  }
}
