package io.gatling.tcp

import akka.actor.ActorRef
import io.gatling.core.util.TimeHelper._
import org.jboss.netty.channel.{ ChannelStateEvent, MessageEvent, ChannelHandlerContext, SimpleChannelUpstreamHandler }

class MessageListener(tx: TcpTx, actor: ActorRef) extends SimpleChannelUpstreamHandler {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    actor ! OnTextMessage(e.getMessage.asInstanceOf[String], nowMillis)
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
    actor ! OnDisconnect(nowMillis)
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
//    actor ! OnConnect(tx, ctx.getChannel, nowMillis)
  }
}
