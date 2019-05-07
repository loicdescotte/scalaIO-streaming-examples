Scala HTTP streaming examples with STTP and FS2, ZIO, Monix, Akka Streams.

To run an example, run the node server : 

```
cd scalaIO-streaming-examples/node 
node stream.js
```

Then run a scala example like this, to consume several http responses in streaming and mix them in a single stream : 

```scala
//for example with zio
sbt "runMain zio.MixedStream"
```

This examples were originally made for this presentation at ScalaIO 2018 :  https://slides.com/loicd/deck
