package scalaz.metrics.http

import java.io.IOException

import com.codahale.metrics.Timer.Context
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{ HttpService, Response }
import scalaz.Scalaz._
import scalaz.metrics.{ Label, Metrics }
import scalaz.zio.{ IO, RTS }
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

import scala.math.Numeric.IntIsIntegral

object TestMetricsService extends RTS {
  println("Serving")

  def performTests(metrics: Metrics[IO[IOException, ?], Context]): IO[IOException, String] =
    for {
      f <- metrics.counter(Label(Array("test", "counter")))
      _ <- f(1)
      _ <- f(2)
      _ <- metrics.gauge(Label(Array("test", "gauge")))(IO.point(5L))
      t <- metrics.timer(Label(Array("test", "timer")))
      l <- IO.traverse(
            List(
              Thread.sleep(1000L),
              Thread.sleep(1400L),
              Thread.sleep(1200L)
            )
          )(a => t.stop(t.apply))
      h <- metrics.histogram(Label(Array("test", "histogram")))
      _ <- IO.traverse(List(h(10), h(25), h(50), h(57), h(19)))(_.void)
      m <- metrics.meter(Label(Array("test", "meter")))
      _ <- IO.traverse(1 to 5)(i => IO.now(m(1)))
    } yield { s"time $l ns" }

  val service = (metrics: Metrics[IO[IOException, ?], Context]) =>
    HttpService[Task] {
      case GET -> Root => {
        val m = performTests(metrics).attempt
          .map(ei => {
            ei.fold(_ => "failure encountered", s => s)
          })

        Response[Task](Ok).withBody(unsafeRun(m))
      }
    }
}
