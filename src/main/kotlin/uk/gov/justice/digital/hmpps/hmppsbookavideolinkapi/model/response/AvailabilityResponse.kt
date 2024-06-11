package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.LocationAndInterval
import java.time.Duration
import java.time.LocalTime

@Schema(description = "Availability check response")
data class AvailabilityResponse(
  @Schema(description = "Set to true when all the locations and times for this booking are available, or false when not.", example = "true")
  val availabilityOk: Boolean = true,

  @Schema(description = "A list of alternative times and locations for the requested appointment on this booking, or empty list")
  val alternatives: List<BookingOption> = emptyList(),
)

@Schema(description = "Video link booking option")
data class BookingOption(
  @Schema(description = "The pre appointment location and time", required = false)
  val pre: LocationAndInterval? = null,

  @Schema(description = "The main appointment location and time", required = true)
  val main: LocationAndInterval,

  @Schema(description = "The post appointment location and time", required = false)
  val post: LocationAndInterval? = null,
) {

  fun copyStartingAt(startTime: LocalTime): BookingOption {
    val offset = Duration.between(earliestStartTime(), startTime)
    return BookingOption(
      pre = this.pre?.shift(offset),
      main = this.main.shift(offset),
      post = this.post?.shift(offset),
    )
  }

  fun toLocationsAndIntervals() = listOfNotNull(pre, main, post)

  fun endsOnOrBefore(endTime: LocalTime): Boolean = latestEndTime().isBefore(endTime) || latestEndTime() == endTime

  private fun earliestStartTime(): LocalTime = pre?.interval?.start ?: main.interval.start

  private fun latestEndTime(): LocalTime = post?.interval?.end ?: main.interval.end

  companion object {
    // Converts the availability check request into a booking option
    fun from(request: AvailabilityRequest) =
      BookingOption(
        pre = request.preAppointment?.let {
          LocationAndInterval(prisonLocKey = it.prisonLocKey, interval = it.interval)
        },
        main = request.mainAppointment.let {
          LocationAndInterval(prisonLocKey = it.prisonLocKey, interval = it.interval)
        },
        post = request.postAppointment?.let {
          LocationAndInterval(prisonLocKey = it.prisonLocKey, interval = it.interval)
        },
      )
  }
}
