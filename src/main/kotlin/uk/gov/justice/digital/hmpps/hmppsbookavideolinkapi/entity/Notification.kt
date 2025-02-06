package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "notification")
class Notification(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val notificationId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "video_booking_id")
  val videoBooking: VideoBooking? = null,

  val templateName: String,

  val email: String,

  val reason: String,

  val govNotifyNotificationId: UUID,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Notification

    return notificationId == other.notificationId
  }

  override fun hashCode(): Int = notificationId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(notificationId = $notificationId)"
}
