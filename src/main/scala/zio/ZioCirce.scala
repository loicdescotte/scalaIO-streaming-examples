package zio

import java.nio.ByteBuffer

import io.circe.generic.auto._
import io.circe.parser._
import sttp.client._
import sttp.client.asynchttpclient.ziostreams.AsyncHttpClientZioStreamsBackend
import zio.console._
import zio.stream.{Stream, _}

import scala.concurrent.duration.Duration

case class Message(text: String, author: String)

object MixedStream extends App {

  val sttpBackend = AsyncHttpClientZioStreamsBackend(this)

  def queryToStream(keyword: String): Task[ZStream[Any, Throwable, Message]] = {

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
            delimitByLine(stream).map{ line =>
              decode[Message](line).getOrElse(Message("Error parsing JSON", "app"))
            }
          case Left(_) => Stream(Message("http error", "app"))
        }
      }
    }

  }

  def delimitByLine(inStream: Stream[Throwable, ByteBuffer]): Stream[Throwable, String] = {
    inStream
      .map(bytes => new String(bytes.array(), "UTF-8"))
      // or ZSink.splitDelimiter("\n")
      .transduce(ZSink.splitLines)
      .flatMap(chunk => Stream.fromChunk(chunk))
  }

  def run(args: List[String]) = {
    val streamsIO: Task[List[Stream[Throwable, Message]]] =
      ZIO.foreach(List("ZIO", "FS2"))(queryToStream)
    val mergedIO: Task[Stream[Throwable, Message]] =
      streamsIO.map(streams => streams.reduceLeft(_.merge(_)))
    mergedIO.flatMap(_.foreach(message => putStrLn(message.toString))).fold(_ => 1, _ => 0)
  }

}
