package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class SafeJobRunner {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun runJob(jobDefinition: JobDefinition): JobType {
    runSafe(jobDefinition)
    return jobDefinition.jobType
  }

  private fun runSafe(jobDefinition: JobDefinition) {
    log.info("JOB: Running job ${jobDefinition.jobType}")

    val elapsed = measureTimeMillis {
      runCatching { jobDefinition.block() }
        .onSuccess { log.info("JOB: Succeeded in running job ${jobDefinition.jobType}") }
        .onFailure { exception -> log.info("JOB: Failed to run job ${jobDefinition.jobType}", exception) }
    }

    log.info("JOB: Time taken for job ${jobDefinition.jobType}: ${elapsed}ms")
  }
}
