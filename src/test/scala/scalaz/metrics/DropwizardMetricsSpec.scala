package scalaz.metrics
import java.io.IOException

import scalaz.Scalaz._
import scalaz.zio.{App, IO}

object DropwizardMetricsSpec extends App {

  val dropwizardMetrics = new DropwizardMetrics

  val f = IO.point(println("Hola"))

  def increaseCounter: IO[IOException, Unit] = for {
    f <- dropwizardMetrics.counter(Label(Array("test", "counter")))
    a <- f(1)
    b <- f(2)
  } yield b

  def measureGauge: IO[IOException, Unit] = for {
    a <- dropwizardMetrics.gauge(Label(Array("test", "gauge")))(IO.point(5L))
  } yield a

  def run(args: List[String]): IO[Nothing, ExitStatus] = {
    increaseCounter.attempt
      .map(ei => {
        println(
          dropwizardMetrics.registry
            .getCounters()
            .get("test.counter")
            .getCount()
        )
        ei.fold(_ => 1, _ => 0)
      })

    measureGauge.attempt
      .map(ei => {
        println(
          dropwizardMetrics.registry
            .getGauges()
            .get("test.gauge")
            .getValue
        )
        ei.fold(_ => 1, _ => 0)
      })
      .map(ExitStatus.ExitNow(_))
  }
}
