package monix

import java.nio.ByteBuffer

import cats.implicits._
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.monix._
import io.circe.generic.auto._
import io.circe.parser._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import scala.concurrent.duration.Duration

case class Message(text: String, author: String)

object MixedStream extends App {

  implicit val sttpBackend = AsyncHttpClientMonixBackend()

  def queryToObservable(keyword: String): Task[Observable[Message]] = {

    val responseTask: Task[Response[Observable[ByteBuffer]]] =
      sttp
        .get(uri"http://localhost:3000?keyword=$keyword")
        .response(asStream[Observable[ByteBuffer]])
        .readTimeout(Duration.Inf)
        .send()

    responseTask.map { response =>
      response.body match {
        case Right(observable) => observable.flatMap { bytes =>
          val s = new String(bytes.array(), "UTF-8")
          Observable(s.split("\n").toList: _*)
            // JSON decode, Circe uses Either rather than throwing exceptions
            .map(line => decode[Message](line).getOrElse(Message("Error parsing JSON","app")))
        }
        case Left(_) => Observable(Message("http error", "app"))
      }
    }
  }

  val streams: Task[List[Observable[Message]]] = List("Akka", "Monix").traverse(queryToObservable)
  streams.map { observables =>
    Observable.merge(observables :_*)
  }.runSyncUnsafe(Duration.Inf).foreach(println)
}
