package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

/**
 * Conversion is not guaranteed, only use when you know (or at least expect) all types to be the same type.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> List<*>.asListOfType() = this as List<T>

fun <T> MutableCollection<T>.addIf(predicate: () -> Boolean, value: T) {
  if (predicate()) {
    this.add(value)
  }
}
