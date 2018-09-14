package scalaz.metrics
import java.io.IOException

import scalaz.Scalaz._
import scalaz.zio.{ App, IO }

object DropwizardMetricsSpec extends App {

  val dropwizardMetrics = new DropwizardMetrics

  val f = IO.point(println("Hola"))

  def performTests: IO[IOException, Unit] =
    for {
      f <- dropwizardMetrics.counter(Label(Array("test", "counter")))
      _ <- f(1)
      _ <- f(2)
      _ <- dropwizardMetrics.gauge(Label(Array("test", "gauge")))(IO.point(5L))
    } yield ()

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    performTests.attempt
      .map(ei => {
        printMetrics
        ei.fold(_ => 1, _ => 0)
      })
      .map(ExitStatus.ExitNow(_))

  def printMetrics() = {
    println(
      dropwizardMetrics.registry
        .getCounters()
        .get("test.counter")
        .getCount()
    )
    println(
      dropwizardMetrics.registry
        .getGauges()
        .get("test.gauge")
        .getValue()
    )
  }
}
