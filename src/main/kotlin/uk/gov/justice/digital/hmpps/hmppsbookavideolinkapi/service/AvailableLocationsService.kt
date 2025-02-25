package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailableLocationsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import java.time.LocalDateTime
import java.time.LocalTime

private const val FIFTEEN_MINUTES = 15L

@Service
@Transactional(readOnly = true)
class AvailableLocationsService(
  private val locationsService: LocationsService,
  private val bookedLocationsService: BookedLocationsService,
  private val prisonRegime: PrisonRegime,
  private val timeSource: TimeSource,
  private val locationAttributesService: LocationAttributesAvailableService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * This provides a snapshot (point in time) of available rooms at time of calling. Note this does not guarantee the
   * room can be booked, by the time the user attempts to save the booking the room could already have been taken by
   * another user of the service.
   */
  fun findAvailableLocations(request: AvailableLocationsRequest): AvailableLocationsResponse {
    val (startOfDay, endOfDay) = getStartAndEndOfDay(request)
    val prisonVideoLinkLocations = getDecoratedLocationsAt(request.prisonCode!!)
    val bookedLocations = bookedLocationsService.findBooked(BookedLookup(request.prisonCode, request.date!!, prisonVideoLinkLocations, request.vlbIdToExclude))
    val meetingDuration = request.bookingDuration!!.toLong()

    val availableLocations = availableLocations {
      prisonVideoLinkLocations.forEach { location ->
        // These time adjustments do not allow for PRE and POST meeting times.
        var meetingStartTime = startOfDay
        var meetingEndTime = meetingStartTime.plusMinutes(meetingDuration)

        while (meetingStartTime.isBefore(endOfDay)) {
          if (!bookedLocations.isBooked(location, meetingStartTime, meetingEndTime) &&
            request.fallsWithinSlotTime(meetingStartTime) &&
            location.allowsByAnyRuleOrSchedule(request, meetingStartTime)
          ) {
            add(
              AvailableLocation(
                name = location.description ?: location.key,
                startTime = meetingStartTime,
                endTime = meetingEndTime,
                dpsLocationId = location.dpsLocationId,
                dpsLocationKey = location.key,
                usage = location.extraAttributes?.locationUsage?.let { LocationUsage.valueOf(it.name) },
                timeSlot = slot(meetingStartTime),
              ),
            )
          }

          meetingStartTime = meetingStartTime.plusMinutes(FIFTEEN_MINUTES)
          meetingEndTime = meetingEndTime.plusMinutes(FIFTEEN_MINUTES)
        }
      }
    }

    return AvailableLocationsResponse(
      availableLocations.build().also { log.info("AVAILABLE LOCATIONS: found ${it.size} available locations matching request $request") },
    )
  }

  private fun availableLocations(init: AvailableLocationsBuilder.() -> Unit): AvailableLocationsBuilder {
    val availableLocations = AvailableLocationsBuilder()
    availableLocations.init()
    return availableLocations
  }

  private class AvailableLocationsBuilder {
    private val probation = mutableSetOf<AvailableLocation>()
    private val court = mutableSetOf<AvailableLocation>()
    private val shared = mutableSetOf<AvailableLocation>()

    fun add(availableLocation: AvailableLocation) {
      // TODO handle probation code, court code, shared and scheduled types specifically
      when (availableLocation.usage) {
        LocationUsage.PROBATION -> probation.add(availableLocation)
        LocationUsage.COURT -> court.add(availableLocation)
        else -> shared.add(availableLocation)
      }
    }

    fun build() = run {
      if (probation.isNotEmpty() && court.isNotEmpty()) {
        throw IllegalStateException("Cannot mix probation and court only locations")
      }

      val sharedCopy = mutableListOf<AvailableLocation>().also { it.addAll(shared) }
      probation.forEach { probation -> sharedCopy.removeIf { shared -> shared.startTime == probation.startTime } }
      court.forEach { court -> sharedCopy.removeIf { shared -> shared.startTime == court.startTime } }

      val result =
        (probation + court + sharedCopy)
          .sortedWith(compareBy({ it.startTime }, { it.name }))
          .distinctBy { it.startTime }
          .toList()

      result
    }
  }

  private fun AvailableLocationsRequest.fallsWithinSlotTime(time: LocalTime) = timeSlots == null || timeSlots.any { slot -> slot.isTimeInSlot(time) }

  private fun getStartAndEndOfDay(request: AvailableLocationsRequest): Pair<LocalTime, LocalTime> {
    val regimeStartOfDay = prisonRegime.startOfDay(request.prisonCode!!)
    val regimeEndOfDay = prisonRegime.endOfDay(request.prisonCode)
    val now = timeSource.now()

    // Start looking for meeting 15 mins from now if looking for slots today when the time has passed the regime start time.
    if (request.isForToday() && regimeStartOfDay.isBefore(now.toLocalTime())) {
      return when (now.minute) {
        0 -> LocalTime.of(now.hour, 15)
        in 1..15 -> LocalTime.of(now.hour, 30)
        in 16..30 -> LocalTime.of(now.hour, 45)
        else -> LocalTime.of(now.hour + 1, 0)
      } to regimeEndOfDay
    }

    return regimeStartOfDay to regimeEndOfDay
  }

  private fun AvailableLocationsRequest.isForToday() = this.date!! == timeSource.today()

  private fun getDecoratedLocationsAt(prisonCode: String) = locationsService.getDecoratedVideoLocations(prisonCode = prisonCode, enabledOnly = true)

  private fun Location.allowsByAnyRuleOrSchedule(request: AvailableLocationsRequest, time: LocalTime): Boolean {
    if (extraAttributes != null) {
      return when (request.bookingType!!) {
        BookingType.COURT -> locationAttributesService.isLocationAvailableFor(
          LocationAvailableRequest.court(
            extraAttributes.attributeId,
            request.courtCode!!,
            request.date!!.atTime(time),
          ),
        )
        BookingType.PROBATION -> locationAttributesService.isLocationAvailableFor(
          LocationAvailableRequest.probation(
            extraAttributes.attributeId,
            request.probationTeamCode!!,
            request.date!!.atTime(time),
          ),
        )
      }
    }

    return true
  }
}

@Service
@Transactional(readOnly = true)
class LocationAttributesAvailableService(
  private val locationAttributeRepository: LocationAttributeRepository,
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
) {
  fun isLocationAvailableFor(request: LocationAvailableRequest): Boolean {
    val attribute = locationAttributeRepository.findById(request.attributeId)
      .orElseThrow { EntityNotFoundException("Location attribute ${request.attributeId} not found") }

    return when (request.type) {
      LocationAvailableRequest.Type.COURT -> {
        attribute.isAvailableFor(
          courtRepository.findByCode(request.code) ?: throw EntityNotFoundException("Court code ${request.code} not found"),
          request.onDateTime,
        )
      }
      LocationAvailableRequest.Type.PROBATION -> {
        attribute.isAvailableFor(
          probationTeamRepository.findByCode(request.code) ?: throw EntityNotFoundException("Probation team ${request.code} not found"),
          request.onDateTime,
        )
      }
    }
  }
}

class LocationAvailableRequest private constructor(
  val attributeId: Long,
  val type: Type,
  val code: String,
  val onDateTime: LocalDateTime,
) {
  enum class Type {
    COURT,
    PROBATION,
  }

  companion object {
    fun court(attributeId: Long, courtCode: String, onDateTime: LocalDateTime) = LocationAvailableRequest(attributeId, Type.COURT, courtCode, onDateTime)

    fun probation(attributeId: Long, probationTeamCode: String, onDateTime: LocalDateTime) = LocationAvailableRequest(attributeId, Type.PROBATION, probationTeamCode, onDateTime)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LocationAvailableRequest

    if (attributeId != other.attributeId) return false
    if (type != other.type) return false
    if (code != other.code) return false
    if (onDateTime != other.onDateTime) return false

    return true
  }

  override fun hashCode() = attributeId.hashCode()
}
