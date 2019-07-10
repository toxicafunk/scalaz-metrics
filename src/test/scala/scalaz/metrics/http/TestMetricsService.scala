package scalaz.metrics.http

import argonaut.Argonaut._
import org.http4s.argonaut._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{ HttpRoutes, Response }
import scalaz.Scalaz._
import scalaz.metrics.{ Label, Metrics }
import scalaz.zio.interop.catz._
import scalaz.zio.{ZIO, TaskR, Task }
import Server._

import scala.math.Numeric.IntIsIntegral

object TestMetricsService {
  println("Serving")

  val s: Stream[Int]               = Stream.from(1)
  val tester: Option[Int] => Stream[Int] = (_: Option[Int]) => s.takeWhile(_ < 10)

  //def performTests[Ctx](metrics: Metrics[Task[?], Ctx]): Task[String] =
  def performTests[Ctx](metrics: Metrics[Task[?], Ctx]): HttpTask[String] =
    for {
      f  <- metrics.counter(Label(Array("test", "counter"), "_"))
      _  <- f(1)
      _  <- f(2)
      g  <- metrics.gauge(Label(Array("test", "gauge"), "_"))(tester)
      _ <- g(2.some)
      //_ <- g(8.some)
      t  <- metrics.timer(Label(Array("test", "timer"), "_"))
      t1 = t.start
      l <- ZIO.foreachPar(
            List(
              Thread.sleep(1000L),
              Thread.sleep(1400L),
              Thread.sleep(1200L)
            )
          )(_ => t.stop(t1))
      h <- metrics.histogram(Label(Array("test", "histogram"), "_"))
      _ <- ZIO.foreach(List(h(10), h(25), h(50), h(57), h(19)))(_.unit)
      m <- metrics.meter(Label(Array("test", "meter"), "_"))
      _ <- ZIO.foreach(1 to 5)(i => ZIO.succeed(m(i.toDouble)))
    } yield { s"time $l ns" }

  def service[Ctx] =
    (metrics: Metrics[Task[?], Ctx]) =>
      HttpRoutes.of[HttpTask] {
        case GET -> Root =>
          val m = performTests(metrics).fold(t => jSingleObject("error", jString(s"failure encountered $t")), s => jSingleObject("time", jString(s)))
          //TaskR(Response[HttpTask](Ok).withEntity(m))
          m.flatMap(j => TaskR(Response[HttpTask](Ok).withEntity(j)))
    }
}
