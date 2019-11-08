package fs2

import java.nio.ByteBuffer

import _root_.io.circe.fs2._
import _root_.io.circe.generic.auto._
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.fs2.AsyncHttpClientFs2Backend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class Message(text: String, author: String)

object MixedStream extends App {

  type Fs2SttpBackend = SttpBackend[IO, Stream[IO, ByteBuffer], WebSocketHandler]

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)
  val sttpBackend = AsyncHttpClientFs2Backend[cats.effect.IO]()

  def queryToStream(keyword: String)(implicit sttpBackend: Fs2SttpBackend): IO[Stream[IO, Message]] = {
    val responseIO =
      basicRequest
        .post(uri"http://localhost:3000?keyword=$keyword")
        .response(asStream[Stream[IO, ByteBuffer]])
        .readTimeout(Duration.Inf)
        .send()

    responseIO.map { response =>
      response.body match {
        case Right(stream) => stream.flatMap { bytes =>
          val s = new String(bytes.array(), "UTF-8")
          Stream(s).through(stringStreamParser[IO]).through(decoder[IO, Message])
        }
        case Left(_) => Stream(Message("http error", "app"))
      }
    }
  }

  sttpBackend.flatMap { implicit backend =>
    val streamsIO: IO[List[Stream[IO, Message]]] = List("Akka", "FS2").traverse(queryToStream)
    val mergedIO = streamsIO.map(streams => streams.reduceLeft(_.merge(_)))
    val printStreamIO: IO[Stream[IO, Unit]] = mergedIO.map(merged => merged.map(println))
    printStreamIO.flatMap(_.compile.drain)
  }.unsafeRunSync()

}
