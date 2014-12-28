package io.gatling.tcp

import akka.actor.ActorRef
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.CheckResult
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper._
import io.gatling.tcp.check.TcpCheck
import org.jboss.netty.channel.Channel

class TcpActor extends BaseActor with DataWriterClient {

  override def receive: Receive = initialState

  val initialState: Receive = {
    case OnConnect(tx, channel, time) =>
      val newSession = tx.session.set("tcpActor", self)
      val newTx = tx.copy(session = newSession)
      context.become(connectedState(channel, newTx))
      tx.next ! newSession
      logRequest(tx.session, "connect", OK, nowMillis, nowMillis)
    case _ => context.stop(self)
  }

  def connectedState(channel: Channel, tx: TcpTx): Receive = {
      def succeedPendingCheck(checkResult: CheckResult) = {
        tx.check match {
          case Some(check) =>
            // expected count met, let's stop the check
            logRequest(tx.session, tx.requestName, OK, tx.start, nowMillis, None)
            val newUpdates = if (checkResult.hasUpdate) {
              checkResult.update.getOrElse(Session.Identity) :: tx.updates
            } else {
              tx.updates
            }
            // apply updates and release blocked flow
            val newSession = tx.session.update(newUpdates)

            tx.next ! newSession
            val newTx = tx.copy(session = newSession, updates = Nil, check = None)
            context.become(connectedState(channel, newTx))
          case _ =>
        }
      }
    {
      case Send(requestName, message, next, session, check) => {
        logger.debug(s"Sending message check on channel '$channel': $message")

        val now = nowMillis

        check match {
          case Some(c) =>
            // do this immediately instead of self sending a Listen message so that other messages don't get a chance to be handled before
            setCheck(tx, channel, requestName, c, next, session)
          case None => next ! session
        }

        message match {
          case TextTcpMessage(text) => channel.write(text)
          case _                    => logger.warn("Only text messages supported")
        }

        logRequest(session, requestName, OK, now, now)
      }
      case OnTextMessage(message, time) => {
        logger.debug(s"Received text message on  :$message")

        implicit val cache = scala.collection.mutable.Map.empty[Any, Any]

        tx.check.foreach { check =>

          check.check(message, tx.session) match {
            case io.gatling.core.validation.Success(result) =>

              succeedPendingCheck(result)
            case s => failPendingCheck(tx, s"check failed $s")

          }
        }
      }
      case CheckTimeout(check) =>
        tx.check match {
          case Some(`check`) =>

            val newTx = failPendingCheck(tx, "Check failed: Timeout")
            context.become(connectedState(channel, newTx))

            // release blocked session
            newTx.next ! newTx.applyUpdates(newTx.session).session
          case _ =>
        }

      // ignore outdated timeout
      case Disconnect(requestName, next, session) => {

        logger.debug(s"Disconnect channel for session: $session")
        channel.close()
        val newSession: Session = session.remove("channel")
        //
        next ! newSession
        logRequest(session, requestName, OK, nowMillis, nowMillis)
      }
      case OnDisconnect(time) =>
        context.become(disconnectedState(tx))

    }
  }

  def disconnectedState(tx: TcpTx): Receive = {
    case a: AnyRef => logger.error(a.toString)
  }

  private def logRequest(session: Session, requestName: String, status: Status, started: Long, ended: Long, errorMessage: Option[String] = None): Unit = {
    writeRequestData(
      session,
      requestName,
      started,
      ended,
      ended,
      ended,
      status,
      errorMessage)
  }

  def setCheck(tx: TcpTx, channel: Channel, requestName: String, check: TcpCheck, next: ActorRef, session: Session): Unit = {

    logger.debug(s"setCheck timeout=${check.timeout}")

    // schedule timeout
    scheduler.scheduleOnce(check.timeout) {
      self ! CheckTimeout(check)
    }

    val newTx = tx
      .applyUpdates(session)
      .copy(start = nowMillis, check = Some(check), next = next, requestName = requestName + "Check")
    context.become(connectedState(channel, newTx))

    //    if (!check.blocking)
    //      next ! newTx.session
  }
  def failPendingCheck(tx: TcpTx, message: String): TcpTx = {
    tx.check match {
      case Some(c) =>
        logRequest(tx.session, tx.requestName, KO, tx.start, nowMillis, Some(message))
        tx.copy(updates = Session.MarkAsFailedUpdate :: tx.updates, check = None)

      case _ => tx
    }
  }
}
