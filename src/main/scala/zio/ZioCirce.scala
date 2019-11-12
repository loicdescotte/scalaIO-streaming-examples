package zio

import java.nio.ByteBuffer

import io.circe.generic.auto._
import io.circe.parser._
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.ziostreams.AsyncHttpClientZioStreamsBackend
import sttp.client.testing.SttpBackendStub
import zio.console._
import zio.stream.{Stream, _}

import scala.concurrent.duration.Duration

case class Message(text: String, author: String)

object MixedStream extends App {

  type ZioSttpBackend = SttpBackend[Task, Stream[Throwable, ByteBuffer], Nothing]

  trait MixedStreamModule extends Console {
    def sttpBackend[R]: RIO[R, ZioSttpBackend]
  }

  // live module
  object MixedStreamModuleLive extends MixedStreamModule with Console.Live {
    def sttpBackend[R]: RIO[R, ZioSttpBackend] = ZIO.runtime.flatMap((r: Runtime[R]) => AsyncHttpClientZioStreamsBackend[R](r))
  }

  def queryToStream(keyword: String): RIO[MixedStreamModule, Stream[Throwable, Message]] = {

    ZIO.accessM[MixedStreamModule](_.sttpBackend).flatMap { implicit backend =>

      val responseIO =
        basicRequest
          .post(uri"http://localhost:3000?keyword=$keyword")
          .response(asStream[Stream[Throwable, ByteBuffer]])
          .readTimeout(Duration.Inf)
          .send()

      responseIO.map { response =>
        response.body match {
          case Right(stream) =>
            delimitByLine(stream).map { line =>
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

  val program: ZIO[MixedStreamModule, Throwable, Unit] = {
    val streamsIO = ZIO.foreach(List("ZIO", "FS2"))(queryToStream)
    val mergedIO = streamsIO.map(streams => streams.reduceLeft(_.merge(_)))
    mergedIO.flatMap(_.foreach(message => putStrLn(message.toString)))
  }

  def run(args: List[String]) = program.provide(MixedStreamModuleLive).fold(_ => 1, _ => 0)


  /* For test you can define a test module object with a stubbed sttp backend
  Example :
  object MixedStreamModuleTest extends MixedStreamModule with Console.Live {
    def sttpBackend[R]: RIO[R, ZioSttpBackend] = {
      ZIO.runtime
        .flatMap((r: Runtime[R]) => AsyncHttpClientZioStreamsBackend[R](r))
        .map { backend =>
          SttpBackendStub(backend)
          //(see https://sttp.readthedocs.io/en/latest/testing.html)
          //.whenRequestMatches(...)
        }
    }
  }*/

}
