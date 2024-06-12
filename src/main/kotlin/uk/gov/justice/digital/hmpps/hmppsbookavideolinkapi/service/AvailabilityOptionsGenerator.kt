package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingOption
import java.time.Duration
import java.time.LocalTime

@Component
class AvailabilityOptionsGenerator(
  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
  @Value("\${video-link-booking.options.day-start}")
  val dayStart: LocalTime,

  @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
  @Value("\${video-link-booking.options.day-end}")
  val dayEnd: LocalTime,

  @Value("\${video-link-booking.options.step}")
  val step: Duration,
) {
  fun getOptionsInPreferredOrder(preferredOption: BookingOption): Sequence<BookingOption> =
    durations(step)
      .map { dayStart.plus(it) }
      .map { preferredOption.copyStartingAt(it) }
      .takeWhile { it.endsOnOrBefore(dayEnd) }
      .sortedBy { Duration.between(preferredOption.main.interval.start, it.main.interval.start).abs() }

  companion object {
    fun durations(delta: Duration) = generateSequence(Duration.ZERO) { it.plus(delta) }
  }
}
