package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment as PrisonAppointmentEntity

fun PrisonAppointmentEntity.toModel() = PrisonAppointment(
  prisonAppointmentId = prisonAppointmentId,
  prisonCode = prisonCode(),
  prisonerNumber = prisonerNumber,
  appointmentType = appointmentType,
  comments = comments,
  prisonLocKey = prisonLocKey,
  appointmentDate = appointmentDate,
  startTime = startTime,
  endTime = endTime,
)

fun List<PrisonAppointmentEntity>.toModel() = map { it.toModel() }
