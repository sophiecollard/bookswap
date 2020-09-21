package com.github.sophiecollard.bookswap.domain.transaction

import java.time.LocalDateTime

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait RequestStatus extends EnumEntry {

  import RequestStatus._

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

  case object Pending                                    extends RequestStatus
  final case class Accepted(acceptedOn: LocalDateTime)   extends RequestStatus
  final case class OnWaitingList(addedOn: LocalDateTime) extends RequestStatus
  final case class Rejected(rejectedOn: LocalDateTime)   extends RequestStatus
  final case class Fulfilled(fulfilledOn: LocalDateTime) extends RequestStatus
  final case class Cancelled(cancelledOn: LocalDateTime) extends RequestStatus

  override val values: immutable.IndexedSeq[RequestStatus] = findValues

  def pending: RequestStatus = Pending
  def accepted(acceptedOn: LocalDateTime): RequestStatus = Accepted(acceptedOn)
  def onWaitingList(addedOn: LocalDateTime): RequestStatus = OnWaitingList(addedOn)
  def rejected(rejectedOn: LocalDateTime): RequestStatus = Rejected(rejectedOn)
  def fulfilled(fulfilledOn: LocalDateTime): RequestStatus = Fulfilled(fulfilledOn)
  def cancelled(cancelledOn: LocalDateTime): RequestStatus = Cancelled(cancelledOn)

}
