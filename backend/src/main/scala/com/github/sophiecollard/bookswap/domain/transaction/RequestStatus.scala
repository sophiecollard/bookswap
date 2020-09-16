package com.github.sophiecollard.bookswap.domain.transaction

import java.time.LocalDateTime

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait RequestStatus extends EnumEntry

object RequestStatus extends Enum[RequestStatus] {

  case object Pending                                    extends RequestStatus
  final case class Accepted(acceptedOn: LocalDateTime)   extends RequestStatus
  final case class Rejected(rejectedOn: LocalDateTime)   extends RequestStatus
  final case class OnWaitingList(addedOn: LocalDateTime) extends RequestStatus

  override val values: immutable.IndexedSeq[RequestStatus] = findValues

  def pending: RequestStatus = Pending
  def accepted(acceptedOn: LocalDateTime): RequestStatus = Accepted(acceptedOn)
  def rejected(rejectedOn: LocalDateTime): RequestStatus = Rejected(rejectedOn)
  def onWaitingList(addedOn: LocalDateTime): RequestStatus = OnWaitingList(addedOn)

}
