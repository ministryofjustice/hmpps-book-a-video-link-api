package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

enum class JobType(val resultMessage: String) {
  COURT_HEARING_LINK_REMINDER("Court hearing link reminders job triggered"),
  PROBATION_OFFICER_DETAILS_REMINDER("Probation officer details reminder job triggered"),
}

abstract class JobDefinition(val jobType: JobType, private val block: () -> Unit) {
  fun runJob() {
    block()
  }
}
