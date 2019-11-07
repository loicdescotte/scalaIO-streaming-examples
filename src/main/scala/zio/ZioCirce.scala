package zio

import java.nio.ByteBuffer

import sttp.client._
import sttp.client.asynchttpclient.ziostreams.AsyncHttpClientZioStreamsBackend
import io.circe.generic.auto._
import io.circe.parser._
import zio._
import zio.console._
import zio.stream._

import scala.concurrent.duration.Duration

case class Message(text: String, author: String)

object MixedStream extends App {

  val sttpBackend = AsyncHttpClientZioStreamsBackend(this)

  def queryToStream(keyword: String): Task[Stream[Throwable, Message]] = {

    sttpBackend.flatMap { implicit backend =>
      val responseIO =
        basicRequest
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

  }

  def run(args: List[String]) = {
    val streamsIO: Task[List[Stream[Throwable, Message]]] =
      ZIO.foreach(List("ZIO", "FS2"))(queryToStream)
    val mergedIO: Task[Stream[Throwable, Message]] =
      streamsIO.map(streams => streams.reduceLeft(_.merge(_)))
    mergedIO.flatMap(_.foreach(message => putStrLn(message.toString))).fold(_ => 1, _ => 0)
  }

}
