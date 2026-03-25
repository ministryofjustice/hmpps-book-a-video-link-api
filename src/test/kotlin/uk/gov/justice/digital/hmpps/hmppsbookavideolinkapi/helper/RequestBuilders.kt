package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import java.time.LocalDate
import java.time.LocalTime

class CreateCourtBookingRequestBuilder {
  private lateinit var courtCode: String
  private lateinit var prisoner: Prisoner
  private lateinit var mainLocation: Location
  private lateinit var main: Appointment
  private var hearingType: CourtHearingType? = null
  private var pre: Location? = null
  private var post: Location? = null
  private var cvpLinkDetails: CvpLinkDetails? = null
  private var guestPin: String? = null
  private var notesForStaff: String? = null
  private var notesForPrisoners: String? = null

  companion object {
    fun builder(init: CreateCourtBookingRequestBuilder.() -> Unit) = CreateCourtBookingRequestBuilder().also { it.init() }
  }

  fun court(courtCode: String) = apply { this.courtCode = courtCode }
  fun hearingType(type: CourtHearingType) = apply { this.hearingType = type }
  fun prisoner(prisoner: Prisoner) = apply { this.prisoner = prisoner }
  fun pre(location: Location) = apply { pre = location }
  fun post(location: Location) = apply { post = location }
  fun cvpLinkDetails(cvpLinkDetails: CvpLinkDetails) = apply { this.cvpLinkDetails = cvpLinkDetails }
  fun pin(pin: String) = apply { this.guestPin = pin }
  fun staffNotes(notes: String) = apply { this.notesForStaff = notes }
  fun prisonerNotes(notes: String) = apply { this.notesForPrisoners = notes }

  fun main(location: Location, date: LocalDate, startTime: LocalTime, endTime: LocalTime) = apply {
    mainLocation = location
    main = Appointment(
      type = AppointmentType.VLB_COURT_MAIN,
      locationKey = location.key,
      date = date,
      startTime = startTime,
      endTime = endTime,
    )
  }

  fun build() = run {
    require(pre == null || pre!!.prisonId == mainLocation.prisonId)
    require(post == null || post!!.prisonId == mainLocation.prisonId)
    require(prisoner.prison == mainLocation.prisonId)

    CreateVideoBookingRequest(
      courtCode = courtCode,
      bookingType = BookingType.COURT,
      prisoners = listOf(
        PrisonerDetails(
          prisonCode = prisoner.prison,
          prisonerNumber = prisoner.number,
          listOfNotNull(
            pre?.let {
              Appointment(
                type = AppointmentType.VLB_COURT_PRE,
                locationKey = it.key,
                date = main.date,
                startTime = main.startTime!!.minusMinutes(15),
                endTime = main.startTime,
              )
            },
            main,
            post?.let {
              Appointment(
                type = AppointmentType.VLB_COURT_POST,
                locationKey = it.key,
                date = main.date,
                startTime = main.endTime!!,
                endTime = main.endTime!!.plusMinutes(15),
              )
            },
          ),
        ),
      ),
      courtHearingType = hearingType ?: CourtHearingType.TRIBUNAL,
      videoLinkUrl = cvpLinkDetails?.videoUrl,
      hmctsNumber = cvpLinkDetails?.hmctsNumber,
      guestPin = guestPin,
      notesForStaff = notesForStaff,
      notesForPrisoners = notesForPrisoners,
    )
  }
}

class AmendCourtBookingRequestBuilder {
  private lateinit var prisoner: Prisoner
  private lateinit var mainLocation: Location
  private lateinit var main: Appointment
  private var hearingType: CourtHearingType? = null
  private var pre: Location? = null
  private var post: Location? = null
  private var cvpLinkDetails: CvpLinkDetails? = null
  private var guestPin: String? = null
  private var notesForStaff: String? = null
  private var notesForPrisoners: String? = null

  companion object {
    fun builder(init: AmendCourtBookingRequestBuilder.() -> Unit) = AmendCourtBookingRequestBuilder().also { it.init() }
  }

  fun hearingType(type: CourtHearingType) = apply { this.hearingType = type }
  fun prisoner(prisoner: Prisoner) = apply { this.prisoner = prisoner }
  fun pre(location: Location) = apply { pre = location }
  fun post(location: Location) = apply { post = location }
  fun cvpLinkDetails(cvpLinkDetails: CvpLinkDetails) = apply { this.cvpLinkDetails = cvpLinkDetails }
  fun pin(pin: String) = apply { this.guestPin = pin }
  fun staffNotes(notes: String) = apply { this.notesForStaff = notes }
  fun prisonerNotes(notes: String) = apply { this.notesForPrisoners = notes }

  fun main(location: Location, date: LocalDate, startTime: LocalTime, endTime: LocalTime) = apply {
    mainLocation = location
    main = Appointment(
      type = AppointmentType.VLB_COURT_MAIN,
      locationKey = location.key,
      date = date,
      startTime = startTime,
      endTime = endTime,
    )
  }

  fun build() = run {
    require(pre == null || pre!!.prisonId == mainLocation.prisonId)
    require(post == null || post!!.prisonId == mainLocation.prisonId)
    require(prisoner.prison == mainLocation.prisonId)

    AmendVideoBookingRequest(
      bookingType = BookingType.COURT,
      prisoners = listOf(
        PrisonerDetails(
          prisonCode = prisoner.prison,
          prisonerNumber = prisoner.number,
          listOfNotNull(
            pre?.let {
              Appointment(
                type = AppointmentType.VLB_COURT_PRE,
                locationKey = it.key,
                date = main.date,
                startTime = main.startTime!!.minusMinutes(15),
                endTime = main.startTime,
              )
            },
            main,
            post?.let {
              Appointment(
                type = AppointmentType.VLB_COURT_POST,
                locationKey = it.key,
                date = main.date,
                startTime = main.endTime!!,
                endTime = main.endTime!!.plusMinutes(15),
              )
            },
          ),
        ),
      ),
      courtHearingType = hearingType ?: CourtHearingType.TRIBUNAL,
      videoLinkUrl = cvpLinkDetails?.videoUrl,
      hmctsNumber = cvpLinkDetails?.hmctsNumber,
      guestPin = guestPin,
      notesForStaff = notesForStaff,
      notesForPrisoners = notesForPrisoners,
    )
  }
}

class CreateProbationBookingRequestBuilder {
  private lateinit var probationTeamCode: String
  private lateinit var prisoner: Prisoner
  private lateinit var mainLocation: Location
  private lateinit var main: Appointment
  private var probationMeetingType: ProbationMeetingType? = null
  private var notesForStaff: String? = null
  private var notesForPrisoners: String? = null

  companion object {
    fun builder(init: CreateProbationBookingRequestBuilder.() -> Unit) = CreateProbationBookingRequestBuilder().also { it.init() }
  }

  fun probationTeam(probationTeamCode: String) = apply { this.probationTeamCode = probationTeamCode }
  fun meetingType(type: ProbationMeetingType) = apply { this.probationMeetingType = type }
  fun prisoner(prisoner: Prisoner) = apply { this.prisoner = prisoner }
  fun staffNotes(notes: String) = apply { this.notesForStaff = notes }
  fun prisonerNotes(notes: String) = apply { this.notesForPrisoners = notes }

  fun main(location: Location, date: LocalDate, startTime: LocalTime, endTime: LocalTime) = apply {
    mainLocation = location
    main = Appointment(
      type = AppointmentType.VLB_PROBATION,
      locationKey = location.key,
      date = date,
      startTime = startTime,
      endTime = endTime,
    )
  }

  fun build() = run {
    require(prisoner.prison == mainLocation.prisonId)

    CreateVideoBookingRequest(
      probationTeamCode = probationTeamCode,
      bookingType = BookingType.PROBATION,
      prisoners = listOf(
        PrisonerDetails(
          prisonCode = prisoner.prison,
          prisonerNumber = prisoner.number,
          listOfNotNull(main),
        ),
      ),
      probationMeetingType = probationMeetingType ?: ProbationMeetingType.RR,
      notesForStaff = notesForStaff,
      notesForPrisoners = notesForPrisoners,
    )
  }
}

class AmendProbationBookingRequestBuilder {
  private lateinit var prisoner: Prisoner
  private lateinit var mainLocation: Location
  private lateinit var main: Appointment
  private var probationMeetingType: ProbationMeetingType? = null
  private var notesForStaff: String? = null
  private var notesForPrisoners: String? = null

  companion object {
    fun builder(init: AmendProbationBookingRequestBuilder.() -> Unit) = AmendProbationBookingRequestBuilder().also { it.init() }
  }

  fun meetingType(type: ProbationMeetingType) = apply { this.probationMeetingType = type }
  fun prisoner(prisoner: Prisoner) = apply { this.prisoner = prisoner }
  fun staffNotes(notes: String) = apply { this.notesForStaff = notes }
  fun prisonerNotes(notes: String) = apply { this.notesForPrisoners = notes }

  fun main(location: Location, date: LocalDate, startTime: LocalTime, endTime: LocalTime) = apply {
    mainLocation = location
    main = Appointment(
      type = AppointmentType.VLB_PROBATION,
      locationKey = location.key,
      date = date,
      startTime = startTime,
      endTime = endTime,
    )
  }

  fun build() = run {
    require(prisoner.prison == mainLocation.prisonId)

    AmendVideoBookingRequest(
      bookingType = BookingType.PROBATION,
      prisoners = listOf(
        PrisonerDetails(
          prisonCode = prisoner.prison,
          prisonerNumber = prisoner.number,
          listOfNotNull(main),
        ),
      ),
      probationMeetingType = probationMeetingType ?: ProbationMeetingType.RR,
      notesForStaff = notesForStaff,
      notesForPrisoners = notesForPrisoners,
    )
  }
}
