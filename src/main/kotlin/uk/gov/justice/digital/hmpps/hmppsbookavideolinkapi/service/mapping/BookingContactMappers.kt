package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact as BookingContactEntity

fun BookingContactEntity.toModel() = BookingContact(
  videoBookingId = videoBookingId,
  contactType = ContactType.valueOf(contactType),
  name = name,
  position = position,
  email = email,
  telephone = telephone,
)

fun List<BookingContactEntity>.toModel() = map { it.toModel() }
