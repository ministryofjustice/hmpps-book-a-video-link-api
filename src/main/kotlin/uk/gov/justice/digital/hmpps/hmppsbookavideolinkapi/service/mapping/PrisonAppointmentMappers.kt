package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment as PrisonAppointmentEntity

fun PrisonAppointmentEntity.toModel(locations: Set<Location>) = run {
  val location = locations.find { it.dpsLocationId == prisonLocationId } ?: throw IllegalArgumentException("Prison location with id $prisonLocationId not found in supplied set of locations")

  PrisonAppointment(
    prisonAppointmentId = prisonAppointmentId,
    prisonCode = prisonCode(),
    prisonerNumber = prisonerNumber,
    appointmentType = appointmentType,
    prisonLocKey = location.key,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    notesForPrisoners = notesForPrisoners,
    notesForStaff = notesForStaff,
    dpsLocationId = this.prisonLocationId,
    dpsLocationDescription = location.description ?: location.key,
  )
}

fun List<PrisonAppointmentEntity>.toModel(locations: Set<Location>) = map { it.toModel(locations) }
