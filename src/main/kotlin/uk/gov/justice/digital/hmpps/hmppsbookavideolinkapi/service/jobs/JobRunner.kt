package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryService
import kotlin.system.measureTimeMillis

@Component
class JobRunner(private val telemetryService: TelemetryService) {

  fun runJob(jobDefinition: JobDefinition): JobType {
    run(jobDefinition)
    return jobDefinition.jobType
  }

  private fun run(jobDefinition: JobDefinition) {
    var timeElapsed = 0L

    runCatching { timeElapsed = measureTimeMillis { jobDefinition.block() } }
      .onSuccess { telemetryService.track(JobSuccessTelemetryEvent(jobType = jobDefinition.jobType, timeElapsed = timeElapsed)) }
      .onFailure { exception -> telemetryService.track(JobFailureTelemetryEvent(jobType = jobDefinition.jobType, message = exception.message, timeElapsed = timeElapsed)) }
  }
}

class JobFailureTelemetryEvent(
  private val jobType: JobType,
  private val message: String? = null,
  private val timeElapsed: Long,
) : TelemetryEvent(TelemetryEventType.JOB_FAILURE) {
  override fun properties(): Map<String, String> =
    mapOf(
      "jobType" to jobType.name,
      "message" to "".plus(message),
      "timeElapsed" to timeElapsed.toString().plus("ms")
    )
}

class JobSuccessTelemetryEvent(
  private val jobType: JobType,
  private val timeElapsed: Long
) : TelemetryEvent(TelemetryEventType.JOB_SUCCESS) {
  override fun properties(): Map<String, String> = mapOf(
    "jobType" to jobType.name,
    "timeElapsed" to timeElapsed.toString().plus("ms")
  )
}
