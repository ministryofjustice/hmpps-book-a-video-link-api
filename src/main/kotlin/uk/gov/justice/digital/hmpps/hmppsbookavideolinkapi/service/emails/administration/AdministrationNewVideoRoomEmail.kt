package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.administration

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.AdministrationEmail

class AdministrationNewVideoRoomEmail(address: String, newPrisonVideoRooms: Map<String, Collection<String>>) : AdministrationEmail(address) {
  init {
    addPersonalisation(
      "prisonsAndRooms",
      buildString {
        newPrisonVideoRooms.forEach { entry ->
          appendLine(entry.key)
          entry.value.forEach { appendLine(it) }
          appendLine()
        }
      },
    )
  }
}
