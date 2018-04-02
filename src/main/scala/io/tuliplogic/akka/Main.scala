package io.tuliplogic.akka

import java.util.UUID

import akka.actor.ActorLogging
import akka.persistence.PersistentActor
import io.tuliplogic.akka.approvalmodel.{ApprovalRequest, Command, Event}
import io.tuliplogic.akka.approvalmodel.ApprovalStatus.Pending

/**
  *
  * akka-persistence-cassandra-poc - 30/03/2018
  * Created with ♥ in Amsterdam
  */
object Main {



}


trait EventSourcing[State, Command, Event] {
  def applyCommand(cmd: Command)(os: Option[State]): Either[String, Event]
  def applyEvent(evt: Event)(os: Option[State]): Either[String, State]
}

object approvalmodel {

  sealed trait ApprovalStatus

  object ApprovalStatus {
    object Pending extends ApprovalStatus
    object Approved extends ApprovalStatus
    object Rejected extends ApprovalStatus
    object Canceled extends ApprovalStatus
  }

  case class ApproverName(name: String)
  case class ApprovalRequestId(id: String)
  object ApprovalRequestId {
    def create = ApprovalRequestId(UUID.randomUUID.toString)
  }

  case class Approver(name: ApproverName, approvalStatus: ApprovalStatus = Pending)
  case class ApprovalRequest(id: ApprovalRequestId, approvers: Map[ApproverName, Approver] = Map()) {
  }

  sealed trait Command
  object Command {
    case class CreateRequest(id: ApprovalRequestId) extends Command
    case class AddApprover(approver: Approver) extends Command

    case class Approve(name: ApproverName) extends Command
    case class Reject(name: ApproverName) extends Command
    case class Cancel(name: ApproverName) extends Command
  }

  sealed trait Event
  object Event {
    case class RequestCreated(id: ApprovalRequestId) extends Event
    case class ApproverAdditionSucceeded(id: ApprovalRequestId, name: ApproverName) extends Event
    case class ApproverAdditionFailed(id: ApprovalRequestId, name: ApproverName) extends Event

    case class ApprovalSucceeded(id: ApprovalRequestId, name: ApproverName) extends Event
    case class ApprovalFailed(id: ApprovalRequestId, name: ApproverName) extends Event
    case class RejectionSucceeded(id: ApprovalRequestId, name: ApproverName) extends Event
    case class RejectionFailed(id: ApprovalRequestId, name: ApproverName) extends Event
    case class CancellationSucceeded(id: ApprovalRequestId, name: ApproverName) extends Event
    case class CancellationFailed(id: ApprovalRequestId, name: ApproverName) extends Event

  }

  object ApprovalRequest extends EventSourcing[ApprovalRequest, Command, Event] {
    def create(approvalRequestId: ApprovalRequestId) = ApprovalRequest(approvalRequestId, Map())

    def approver(name: ApproverName)(request: ApprovalRequest): Either[String, Approver] = request.approvers.get(name).toRight("Approver does not exist")
    def contains(name: ApproverName)(request: ApprovalRequest): Boolean = approver(name)(request).isRight
    def status(name: ApproverName)(request: ApprovalRequest): Either[String, ApprovalStatus] = approver(name)(request).map(_.approvalStatus)

    def canAddApprover(approver: Approver, request: ApprovalRequest): Boolean = !contains(approver.name)(request)
    def addApprover(approver: Approver)(request: ApprovalRequest): Either[String, ApprovalRequest] =
      if (!canAddApprover(approver, request)) Left("Approver already in request") else Right(request.copy(approvers = request.approvers + (approver.name -> approver)))

    //we allow update only of pending requests. This logic could be more complicated
    private def update(approverName: ApproverName, request: ApprovalRequest, toStatus: ApprovalStatus): Either[String, ApprovalRequest] = for {
      approver <- approver(approverName)(request)
      updated <- if (approver.approvalStatus == Pending)
        request.copy(approvers =
          request.approvers.updated(approver.name, approver.copy(approvalStatus =
            toStatus)))
    } yield updated

    def approve(approverName: ApproverName)(request: ApprovalRequest): Either[String, ApprovalRequest] = update(approverName, request, ApprovalStatus.Approved)
    def cancel(approverName: ApproverName)(request: ApprovalRequest): Either[String, ApprovalRequest] = update(approverName, request, ApprovalStatus.Canceled)
    def reject(approverName: ApproverName)(request: ApprovalRequest): Either[String, ApprovalRequest] = update(approverName, request, ApprovalStatus.Rejected)

    override def applyCommand(cmd: Command)(optRequest: Option[ApprovalRequest]): Either[String, Event]= (cmd, optRequest) match {
      case (Command.CreateRequest(id), None) => Right(Event.RequestCreated(id))

      case (Command.AddApprover(approver), Some(approvalRequest)) =>
        if(canAddApprover(approver, approvalRequest)) Right(Event.ApproverAdditionSucceeded(approvalRequest.id, approver.name))
        else Right(Event.ApproverAdditionFailed(approvalRequest.id, approver.name))

      case (Command.Approve(approverName), Some(approvalRequest)) => if (approve(approverName)(approvalRequest).isRight)
        Right(Event.ApprovalSucceeded(approvalRequest.id, approverName))
        else Right(Event.ApprovalFailed(approvalRequest.id, approverName))

      case (Command.Reject(approverName), Some(approvalRequest)) => if (reject(approverName)(approvalRequest).isRight)
        Right(Event.RejectionSucceeded(approvalRequest.id, approverName))
        else Right(Event.RejectionFailed(approvalRequest.id, approverName))

      case (Command.Cancel(approverName), Some(approvalRequest)) => if (approve(approverName)(approvalRequest).isRight)
        Right(Event.CancellationSucceeded(approvalRequest.id, approverName))
        else Right(Event.CancellationFailed(approvalRequest.id, approverName))

      case _ => Left(s"Could not apply command $cmd to approval request $optRequest")
    }

    override def applyEvent(evt: Event)(optRequest: Option[ApprovalRequest]): Either[String, ApprovalRequest] = (evt, optRequest) match {
      case (Event.RequestCreated(requestId), None) => Right(ApprovalRequest(requestId))

      case (Event.ApprovalSucceeded(_, approverName), Some(approvalRequest)) => ApprovalRequest.approve(approverName)(approvalRequest)
      case (Event.ApprovalFailed(_, _), Some(approvalRequest)) => Right(approvalRequest) // failed is absorbed and won't change the state

      case (Event.RejectionSucceeded(_, approverName), Some(approvalRequest)) => ApprovalRequest.reject(approverName)(approvalRequest)
      case (Event.RejectionFailed(_, _), Some(approvalRequest)) => Right(approvalRequest) // failed is absorbed and won't change the state

      case (Event.CancellationSucceeded(_, approverName), Some(approvalRequest)) => ApprovalRequest.cancel(approverName)(approvalRequest)
      case (Event.CancellationFailed(_, _), Some(approvalRequest)) => Right(approvalRequest) // failed is absorbed and won't change the stat
    }
  }
}

object ApprovalRequestsRepository {
  def create() = {


  }
}

class ApprovalRequestActor extends PersistentActor with ActorLogging {

  override def receiveRecover: Receive = handleRecover(None)

  override def receiveCommand: Receive = handleCommand(None)

  def handleCommand(request: Option[ApprovalRequest]): Receive = {
    case c: Command => ApprovalRequest.applyCommand(c)(request) match {
      case Left(error) => log.error(s"Error applying command $error")
      case Right(event) =>
        ApprovalRequest.applyEvent(event)(request) match {
          case Right(updatedRequest) =>
            persist(event)
            context.become(handleCommand(Some(updatedRequest)))
          case _ => ()

        }
    }
  }

  def handleRecover(request: Option[ApprovalRequest]): Receive = {
    case e: Event =>
      ApprovalRequest.applyEvent(e)(request) match {
        case Right(updatedRequest) =>
          context.become(handleRecover(Some(updatedRequest)))
        case _ => ()
      }
  }

  override def persistenceId: String = s"approval-request-${self.path.name}"
}