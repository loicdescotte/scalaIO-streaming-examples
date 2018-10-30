package fs2

import java.nio.ByteBuffer

import cats.effect.IO
import cats.implicits._
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import io.circe.fs2._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class Message(text: String, author: String)

object MixedStream extends App {
  
  implicit val contextShift = IO.contextShift(global)
  implicit val timer = IO.timer(global)
  implicit val sttpBackend = AsyncHttpClientFs2Backend[cats.effect.IO]()

  def queryToStream(keyword: String): IO[Stream[IO,Message]] = {
    val responseIO: IO[Response[Stream[IO,ByteBuffer]]] =
      sttp
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

  val streamsIO: IO[List[Stream[IO,Message]]] = List("Akka", "FS2").traverse(queryToStream)
  val mergedIO = streamsIO.map(streams => streams.reduceLeft(_.merge(_)))
  val printStreamIO: IO[Stream[IO, Unit]] = mergedIO.map(merged => merged.map(println))
  val printIO: IO[Unit] = printStreamIO.flatMap(_.compile.drain)
  printIO.unsafeRunSync()
}
