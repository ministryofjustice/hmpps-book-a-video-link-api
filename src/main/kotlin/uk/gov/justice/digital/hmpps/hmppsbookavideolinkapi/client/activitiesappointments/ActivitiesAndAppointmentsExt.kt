package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import java.time.LocalTime

fun AppointmentSearchResult.isTheSameAppointmentType(appointmentType: SupportedAppointmentTypes.Type) = category.code == appointmentType.code

fun AppointmentSearchResult.isTheSameTime(appointment: PrisonAppointment) = appointment.startTime == LocalTime.parse(startTime) && appointment.endTime == LocalTime.parse(endTime)

fun AppointmentSearchResult.isTheSameTime(bha: BookingHistoryAppointment) = bha.startTime == LocalTime.parse(startTime) && bha.endTime == LocalTime.parse(endTime)
