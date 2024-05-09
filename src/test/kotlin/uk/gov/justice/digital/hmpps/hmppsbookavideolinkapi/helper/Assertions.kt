package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import org.assertj.core.api.Assertions.assertThat

internal inline infix fun <reified T> Collection<T>.containsExactlyInAnyOrder(value: Collection<T>) {
  assertThat(this).containsExactlyInAnyOrder(*value.toTypedArray())
}

internal inline infix fun <reified K, V> Map<K, V>.containsEntriesExactlyInAnyOrder(value: Map<K, V>) {
  assertThat(this).containsExactlyInAnyOrderEntriesOf(value)
}

internal infix fun Boolean.isBool(value: Boolean) {
  assertThat(this).isEqualTo(value)
}

internal infix fun <T> T.isEqualTo(value: T) {
  assertThat(this).isEqualTo(value)
}
