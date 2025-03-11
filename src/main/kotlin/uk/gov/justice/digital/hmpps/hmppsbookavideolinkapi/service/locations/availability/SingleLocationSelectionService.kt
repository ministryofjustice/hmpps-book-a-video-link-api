package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailableLocationsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage as ModelLocationUsage

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
    val prisonVideoLinkLocations = getVideoLinkLocationsAt(request.prisonCode!!)
    val bookedLocations = bookedLocationsService.findBooked(BookedLookup(request.prisonCode, request.date!!, prisonVideoLinkLocations, request.vlbIdToExclude))
    val meetingDuration = request.bookingDuration!!.toLong()

    val availableLocationsBuilder = availableLocations {
      prisonVideoLinkLocations.forEach { location ->
        // These time adjustments do not allow for PRE and POST meeting times.
        var meetingStartTime = startOfDay
        var meetingEndTime = meetingStartTime.plusMinutes(meetingDuration)

        while (meetingStartTime.isBefore(endOfDay)) {
          if (!bookedLocations.isBooked(location, meetingStartTime, meetingEndTime) && request.fallsWithinSlotTime(meetingStartTime)) {
            val availabilityStatus = location.allowsByAnyRuleOrSchedule(request, meetingStartTime)
            add(availabilityStatus, location, meetingStartTime, meetingEndTime)
          }

          meetingStartTime = meetingStartTime.plusMinutes(FIFTEEN_MINUTES)
          meetingEndTime = meetingEndTime.plusMinutes(FIFTEEN_MINUTES)
        }
      }
    }

    return AvailableLocationsResponse(
      availableLocationsBuilder.build().also { log.info("AVAILABLE LOCATIONS: found ${it.size} available locations matching request $request") },
    )
  }

  private fun availableLocations(init: AvailableLocationsBuilder.() -> Unit) = AvailableLocationsBuilder().also { it.init() }

  private class AvailableLocationsBuilder {
    private val dedicatedProbationTeamLocations = mutableSetOf<AvailableLocation>()
    private val anyProbationTeamLocations = mutableSetOf<AvailableLocation>()
    private val dedicatedCourtLocations = mutableSetOf<AvailableLocation>()
    private val anyCourtLocations = mutableSetOf<AvailableLocation>()
    private val sharedLocations = mutableSetOf<AvailableLocation>()

    fun add(availabilityStatus: AvailabilityStatus, location: Location, startTime: LocalTime, endTime: LocalTime) {
      AvailableLocation(
        name = location.description ?: location.key,
        startTime = startTime,
        endTime = endTime,
        dpsLocationId = location.dpsLocationId,
        dpsLocationKey = location.key,
        usage = location.extraAttributes?.locationUsage?.let { ModelLocationUsage.valueOf(it.name) },
        timeSlot = slot(startTime),
      ).let { availableLocation ->
        when (availabilityStatus) {
          AvailabilityStatus.PROBATION_TEAM -> dedicatedProbationTeamLocations.add(availableLocation)
          AvailabilityStatus.PROBATION_ANY -> anyProbationTeamLocations.add(availableLocation)
          AvailabilityStatus.COURT_ROOM -> dedicatedCourtLocations.add(availableLocation)
          AvailabilityStatus.COURT_ANY -> anyCourtLocations.add(availableLocation)
          AvailabilityStatus.SHARED -> sharedLocations.add(availableLocation)
          AvailabilityStatus.NONE -> { }
        }
      }
    }

    fun build() = run {
      val probation = buildList<AvailableLocation> {
        addAll(dedicatedProbationTeamLocations)
        addAll(anyProbationTeamLocations.filter { any -> this.none { it.startTime == any.startTime } })
      }

      // This is not complete for courts (regardless of schedules), it is more complicated than this with pre and post meetings!!!!
      val court = buildList<AvailableLocation> {
        addAll(dedicatedCourtLocations)
        addAll(anyCourtLocations.filter { any -> this.none { it.startTime == any.startTime } })
      }

      if (probation.isNotEmpty() && court.isNotEmpty()) {
        throw IllegalStateException("Cannot mix probation and court locations")
      }

      val availableLocations = buildList<AvailableLocation> {
        addAll(probation)
        addAll(court)
        addAll(sharedLocations.filter { shared -> this.none { it.startTime == shared.startTime } })
      }

      availableLocations
        .sortedWith(compareBy({ it.startTime }, { it.name }))
        .distinctBy { it.startTime }
        .toList()
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

  private fun getVideoLinkLocationsAt(prisonCode: String) = locationsService.getVideoLinkLocationsAtPrison(prisonCode = prisonCode, enabledOnly = true)

  private fun Location.allowsByAnyRuleOrSchedule(request: AvailableLocationsRequest, time: LocalTime): AvailabilityStatus {
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

    return AvailabilityStatus.SHARED
  }
}

@Service
@Transactional(readOnly = true)
class LocationAttributesAvailableService(
  private val locationAttributeRepository: LocationAttributeRepository,
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
) {
  fun isLocationAvailableFor(request: LocationAvailableRequest): AvailabilityStatus {
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
