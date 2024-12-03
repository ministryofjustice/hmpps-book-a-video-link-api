package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

enum class JobType(val resultMessage: String) {
  COURT_HEARING_LINK_REMINDER("Court hearing link reminders job triggered"),
}

abstract class JobDefinition(val jobType: JobType, private val block: () -> Unit) {
  fun runJob() {
    block()
  }
}
