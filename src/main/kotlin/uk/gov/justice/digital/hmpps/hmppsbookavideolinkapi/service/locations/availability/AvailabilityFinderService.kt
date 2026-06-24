package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AppointmentSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Interval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailabilityResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingOption
import java.time.LocalTime
import java.util.TreeMap
import java.util.UUID

@Service
class AvailabilityFinderService(
  private val availabilityOptionsGenerator: AvailabilityOptionsGenerator,

  @Value("\${video-link-booking.options.max-alternatives}")
  private val maxAlternatives: Int = 3,
) {

  fun getOptions(request: AvailabilityRequest, appointments: List<AppointmentSlot>, locations: List<Location>): AvailabilityResponse {
    // Create a Map of <PrisonLocKey, Timeline> from the existing appointments - so we can check and find gaps
    val timelinesByLocationKey = timelinesByLocationKey(appointments, locations)
    val timelinesByLocationId = timelinesByLocationId(appointments, locations)

    // Convert the requested times into a booking option object
    val preferredOption = BookingOption.from(request)

    var alternatives: Sequence<BookingOption>

    if (request.mainAppointment.dpsLocationId != null) {
      // If the preferred option (requested times) is available, return ok with no alternatives
      if (optionIsBookableById(preferredOption, timelinesByLocationId)) {
        return AvailabilityResponse(availabilityOk = true, alternatives = emptyList())
      }

      // Find upto the maximum number of alternative booking times for these rooms
      alternatives = availabilityOptionsGenerator
        .getOptionsInPreferredOrder(preferredOption)
        .filter { optionIsBookableById(it, timelinesByLocationId) }
        .take(maxAlternatives)
        .sortedBy { it.main.interval.start }
    } else {
      // If the preferred option (requested times) is available, return ok with no alternatives
      if (optionIsBookableByKey(preferredOption, timelinesByLocationKey)) {
        return AvailabilityResponse(availabilityOk = true, alternatives = emptyList())
      }

      // Find upto the maximum number of alternative booking times for these rooms
      alternatives = availabilityOptionsGenerator
        .getOptionsInPreferredOrder(preferredOption)
        .filter { optionIsBookableByKey(it, timelinesByLocationKey) }
        .take(maxAlternatives)
        .sortedBy { it.main.interval.start }
    }

    // Return the alternatives
    return AvailabilityResponse(availabilityOk = false, alternatives = alternatives.toList())
  }

  companion object {

    @Deprecated(message = "Use timelinesByLocationId instead", replaceWith = ReplaceWith("timelinesByLocationId(slots, locations)"))
    fun optionIsBookableByKey(option: BookingOption, timelinesByLocationId: Map<String, Timeline>) = option
      .toLocationsAndIntervals()
      .all { timelinesByLocationId[it.prisonLocKey]?.isFreeForInterval(it.interval) ?: true }

    fun optionIsBookableById(option: BookingOption, timelinesByLocationId: Map<UUID, Timeline>) = option
      .toLocationsAndIntervals()
      .all { timelinesByLocationId[it.dpsLocationId]?.isFreeForInterval(it.interval) ?: true }

    @Deprecated(message = "Use timelinesByLocationId instead", replaceWith = ReplaceWith("timelinesByLocationId(slots, locations)"))
    fun timelinesByLocationKey(slots: List<AppointmentSlot>, locations: List<Location>): Map<String, Timeline> = slots
      .groupBy { a -> locations.single { it.id == a.prisonLocationId }.key }
      .mapValues { (_, appointments) -> appointments.flatMap(Companion::toEvents) }
      .mapValues { Timeline(it.value) }

    fun timelinesByLocationId(slots: List<AppointmentSlot>, locations: List<Location>): Map<UUID, Timeline> = slots
      .groupBy { a -> locations.single { it.id == a.prisonLocationId }.id }
      .mapValues { (_, appointments) -> appointments.flatMap(Companion::toEvents) }
      .mapValues { Timeline(it.value) }

    private fun toEvents(slot: AppointmentSlot) = listOf(StartEvent(slot.startTime), EndEvent(slot.endTime))
  }
}

/**
 * Utility classes for constructing a timeline of existing appointment start/end times, within which to
 * check and find available slots.
 */

sealed class Event(val time: LocalTime)

class StartEvent(time: LocalTime) : Event(time)

class EndEvent(time: LocalTime) : Event(time)

/**
 * The Timeline class is constructed with the list of video appointment start/end times occurring today.
 * Its init block it will process all events passed in the constructor, and identify the free periods for each
 * of the rooms requested.
 */

class Timeline(events: List<Event>) {
  private val emptyPeriods = TreeMap<LocalTime, LocalTime>()

  init {
    var currentBookings = 0

    var previousEventTime: LocalTime = LocalTime.MIN

    var freePeriodStarted: LocalTime? = LocalTime.MIN

    fun updateStateForPreviousEventTime() {
      when (currentBookings) {
        0 -> {
          // The room became, or continued to be, unoccupied
          if (freePeriodStarted == null) {
            // The room became unoccupied
            freePeriodStarted = previousEventTime
          }
        }
        else -> {
          // The room became, or continued to be occupied
          if (freePeriodStarted != null) {
            // The room became occupied at previousEventTime.
            emptyPeriods[freePeriodStarted!!] = previousEventTime
            freePeriodStarted = null
          }
        }
      }
    }

    events
      .plus(StartEvent(LocalTime.MAX))
      .sortedBy { it.time }
      .forEach {
        if (it.time > previousEventTime) {
          updateStateForPreviousEventTime()
        }

        currentBookings += when (it) {
          is StartEvent -> 1
          is EndEvent -> -1
        }

        previousEventTime = it.time
      }

    // Handle the end of day
    updateStateForPreviousEventTime()
  }

  /**
   * A List of pairs of start and end times representing the free periods.  Ordered by start time ascending.
   * Used for testing only.
   */
  fun emptyPeriods() = emptyPeriods.navigableKeySet().map {
    Pair(it, emptyPeriods[it])
  }

  /**
   * Answer the question 'is this Timeline free of appointments during the specified interval'.
   */
  fun isFreeForInterval(interval: Interval): Boolean {
    if (interval.end.isBefore(interval.start)) {
      throw IllegalArgumentException("start must precede end")
    }
    val freePeriod = emptyPeriods.floorEntry(interval.start)
    return interval.end.isBefore(freePeriod.value) || interval.end == freePeriod.value
  }
}
