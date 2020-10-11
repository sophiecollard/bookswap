package com.github.sophiecollard.bookswap.domain.transaction

import java.time.LocalDateTime

import doobie.implicits.javatime._
import doobie.util.{Read, Write}
import enumeratum.{Enum, EnumEntry}
import io.circe.syntax._
import io.circe.{Encoder, Json}

import scala.collection.immutable

sealed abstract class RequestStatus(val value: String) extends EnumEntry {

  import RequestStatus._

  def maybeTimestamp: Option[LocalDateTime] = this match {
    case Pending                => None
    case Accepted(acceptedOn)   => Some(acceptedOn)
    case OnWaitingList(addedOn) => Some(addedOn)
    case Rejected(rejectedOn)   => Some(rejectedOn)
    case Fulfilled(fulfilledOn) => Some(fulfilledOn)
    case Cancelled(cancelledOn) => Some(cancelledOn)
  }

  def isPending: Boolean = this match {
    case Pending => true
    case _       => false
  }

  def isAccepted: Boolean = this match {
    case Accepted(_) => true
    case _           => false
  }

  def isOnWaitingList: Boolean = this match {
    case OnWaitingList(_) => true
    case _                => false
  }

  def isRejected: Boolean = this match {
    case Rejected(_) => true
    case _           => false
  }

  def isFulfilled: Boolean = this match {
    case Fulfilled(_) => true
    case _            => false
  }

  def isCancelled: Boolean = this match {
    case Cancelled(_) => true
    case _            => false
  }

}

object RequestStatus extends Enum[RequestStatus] {

  case object Pending                                    extends RequestStatus(PendingStatusValue)
  final case class Accepted(acceptedOn: LocalDateTime)   extends RequestStatus(AcceptedStatusValue)
  final case class OnWaitingList(addedOn: LocalDateTime) extends RequestStatus(OnWaitingListStatusValue)
  final case class Rejected(rejectedOn: LocalDateTime)   extends RequestStatus(RejectedStatusValue)
  final case class Fulfilled(fulfilledOn: LocalDateTime) extends RequestStatus(FulfilledStatusValue)
  final case class Cancelled(cancelledOn: LocalDateTime) extends RequestStatus(CancelledStatusValue)

  override val values: immutable.IndexedSeq[RequestStatus] = findValues

  val PendingStatusValue: String = "pending"
  val AcceptedStatusValue: String = "accepted"
  val OnWaitingListStatusValue: String = "on_waiting_list"
  val RejectedStatusValue: String = "rejected"
  val FulfilledStatusValue: String = "fulfilled"
  val CancelledStatusValue: String = "cancelled"

  def pending: RequestStatus = Pending
  def accepted(acceptedOn: LocalDateTime): RequestStatus = Accepted(acceptedOn)
  def onWaitingList(addedOn: LocalDateTime): RequestStatus = OnWaitingList(addedOn)
  def rejected(rejectedOn: LocalDateTime): RequestStatus = Rejected(rejectedOn)
  def fulfilled(fulfilledOn: LocalDateTime): RequestStatus = Fulfilled(fulfilledOn)
  def cancelled(cancelledOn: LocalDateTime): RequestStatus = Cancelled(cancelledOn)

  implicit val read: Read[RequestStatus] =
    Read[(String, Option[LocalDateTime])].map {
      case (PendingStatusValue, None)                => Pending
      case (AcceptedStatusValue, Some(acceptedOn))   => Accepted(acceptedOn)
      case (OnWaitingListStatusValue, Some(addedOn)) => OnWaitingList(addedOn)
      case (RejectedStatusValue, Some(rejectedOn))   => Rejected(rejectedOn)
      case (FulfilledStatusValue, Some(fulfilledOn)) => Fulfilled(fulfilledOn)
      case (CancelledStatusValue, Some(cancelledOn)) => Cancelled(cancelledOn)
      case other => throw new RuntimeException(s"Invalid RequestStatus: $other")
    }

  implicit val write: Write[RequestStatus] =
    Write[(String, Option[LocalDateTime])].contramap { status =>
      (status.value, status.maybeTimestamp)
    }

  implicit val encoder: Encoder[RequestStatus] =
    Encoder.instance { status =>
      Json.obj(
        "status" := status.value,
        "timestamp" := status.maybeTimestamp.map(_.asJson).getOrElse(Json.Null)
      )
    }

}
