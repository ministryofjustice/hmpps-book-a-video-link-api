package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

enum class JobType(val resultMessage: String = "") {
  COURT_HEARING_LINK_REMINDER("Court hearing link reminders sent"),
}

abstract class JobDefinition(val jobType: JobType, val block: () -> Unit)
