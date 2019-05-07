package zio

import java.nio.ByteBuffer

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.ziostreams.AsyncHttpClientZioStreamsBackend
import io.circe.generic.auto._
import io.circe.parser._
import scalaz.zio._
import scalaz.zio.console._
import scalaz.zio.stream._

import scala.concurrent.duration.Duration

case class Message(text: String, author: String)

object MixedStream extends App {

  val zioRuntime = new DefaultRuntime {}
  implicit val sttpBackend = AsyncHttpClientZioStreamsBackend(zioRuntime)

  def queryToStream(keyword: String): Task[Stream[Throwable, Message]] = {
    val responseIO: Task[Response[Stream[Throwable, ByteBuffer]]] =
      sttp
        .post(uri"http://localhost:3000?keyword=$keyword")
        .response(asStream[Stream[Throwable, ByteBuffer]])
        .readTimeout(Duration.Inf)
        .send()

    responseIO.map { response =>
      response.body match {
        case Right(stream) =>
          stream.flatMap { bytes =>
            val s = new String(bytes.array(), "UTF-8")
            Stream.fromIterable(s.split("\n"))
              // JSON decode, Circe uses Either rather than throwing exceptions
              .map(line => decode[Message](line).getOrElse(Message("Error parsing JSON", "app")))
          }
        case Left(_) => Stream(Message("http error", "app"))
      }

    }

  }

  def run(args: List[String]) = {
    val streamsIO: Task[List[Stream[Throwable, Message]]] =
      ZIO.foreach(List("ZIO", "FS2"))(queryToStream)
    val mergedIO: Task[Stream[Throwable, Message]] =
      streamsIO.map(streams => streams.reduceLeft(_.merge(_)))
    mergedIO.flatMap(_.foreach(message => putStrLn(message.toString))).fold(_ => 1, _ => 0)
  }

}
