package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment as PrisonAppointmentEntity

fun PrisonAppointmentEntity.toModel(locations: Set<Location>) = PrisonAppointment(
  prisonAppointmentId = prisonAppointmentId,
  prisonCode = prisonCode(),
  prisonerNumber = prisonerNumber,
  appointmentType = appointmentType,
  comments = comments,
  prisonLocKey = locations.find { it.dpsLocationId == prisonLocationId }?.key ?: throw IllegalArgumentException("Prison location with id $prisonLocationId not found in supplied set of locations"),
  appointmentDate = appointmentDate,
  startTime = startTime,
  endTime = endTime,
  notesForPrisoners = notesForPrisoners,
  notesForStaff = notesForStaff,
)

fun List<PrisonAppointmentEntity>.toModel(locations: Set<Location>) = map { it.toModel(locations) }
