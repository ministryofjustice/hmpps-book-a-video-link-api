package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

const val BIRMINGHAM = "BMI"
const val WANDSWORTH = "WWI"
const val RISLEY = "RSI"
const val PENTONVILLE = "PVI"
const val NORWICH = "NWI"

val prisonNames = mapOf(
  BIRMINGHAM to "Birmingham",
  WANDSWORTH to "Wandsworth",
  RISLEY to "Risley",
  PENTONVILLE to "Pentonville (HMP & YOI)",
  NORWICH to "Norwich (HMP & YOI)",
)

val pentonvillePrison = prison(PENTONVILLE)
val wandsworthPrison = prison(WANDSWORTH)
