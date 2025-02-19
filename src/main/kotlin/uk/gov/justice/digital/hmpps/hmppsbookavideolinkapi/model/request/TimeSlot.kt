package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import java.time.LocalTime

enum class TimeSlot(private val predicate: (LocalTime) -> Boolean) {
  AM({ t -> t.hour < 12 }),
  PM({ t -> t.hour in 12..16 }),
  ED({ t -> t.hour >= 17 }),
  ;

  fun isTimeInSlot(time: LocalTime) = predicate.invoke(time)
}
