package akka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Framing, Sink, Source}
import akka.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.ahc._

import scala.concurrent._
import scala.concurrent.duration._

  case class Message(text: String, author: String)

object Message {
  implicit val messageReader = Json.reads[Message]
}

object MixedStream extends App {

  implicit val system = ActorSystem("MixedStream")
  implicit val materializer = ActorMaterializer()
  val wsClient = StandaloneAhcWSClient()

  def queryToSource(keyword: String) = {

    val request = wsClient
      .url(s"http://localhost:3000?keyword=$keyword")

    Source.fromFuture(request.stream()).flatMapConcat(_.bodyAsSource)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 100, allowTruncation = true))
      .map(byteString => Json.parse(byteString.utf8String).as[Message])
  }

  val keywordSources = Source(List("Akka", "FS2")) // can mix n streams
  val done: Future[Done] = keywordSources.flatMapMerge(10, queryToSource)
    .runWith(Sink.foreach(println))
  Await.result(done, Duration.Inf)

}



