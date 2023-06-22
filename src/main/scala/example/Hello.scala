package example

import zio._

import zio.http.ChannelEvent.{
  ExceptionCaught,
  Read,
  UserEvent,
  UserEventTriggered
}
import zio.http._
import zio.stream._
import java.nio.file.Paths

object WebSocketAdvanced extends ZIOAppDefault {

  SubscriptionRef
  val socketApp: SocketApp[Any] =
    Handler.webSocket { channel =>
      channel.receiveAll {
        case Read(WebSocketFrame.Text("end")) =>
          channel.shutdown

        // Send a "bar" if the server sends a "foo"
        case Read(WebSocketFrame.Text("foo")) =>
          channel.send(Read(WebSocketFrame.text("bar")))

        // Send a "foo" if the server sends a "bar"
        case Read(WebSocketFrame.Text("bar")) =>
          channel.send(Read(WebSocketFrame.text("foo")))

        // Echo the same message 10 times if it's not "foo" or "bar"
        case Read(WebSocketFrame.Text(text)) =>
          ZIO.logInfo(s"got text $text") *> channel
            .send(Read(WebSocketFrame.text(s"echo $text")))
            .repeatN(10)
            .catchSomeCause { case cause =>
              ZIO.logErrorCause(s"failed sending", cause)
            } *> ZIO.logInfo("echo ten times")

        // Send a "greeting" message to the server once the connection is established
        case UserEventTriggered(UserEvent.HandshakeComplete) =>
          channel.send(Read(WebSocketFrame.text("Greetings!")))

        // Log when the channel is getting closed
        case Read(WebSocketFrame.Close(status, reason)) =>
          Console.printLine(
            "Closing channel with status: " + status + " and reason: " + reason
          )

        // Print the exception if it's not a normal close
        case ExceptionCaught(cause) =>
          Console.printLine(s"Channel error!: ${cause.getMessage}")

        case _ =>
          ZIO.logInfo("unknown message") *> ZIO.unit
      }
    }

  val files: Http[Any, Response, Request, Response] = Http
    .collectHandler[Request] { case Method.GET -> Root / "index.html" =>
      Http
        .fromFile(Paths.get("src/main/resources/index.html").toFile())
        .toHandler(Handler.notFound)
    }
    .mapError(_ => Response.status(Status.InternalServerError))

  val app: Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.GET -> Root / "subscriptions" =>
      socketApp.toResponse
    }

  override val run = Server.serve(app ++ files).provide(Server.default)
}
