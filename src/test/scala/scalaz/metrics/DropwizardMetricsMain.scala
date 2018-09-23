package scalaz.metrics

import java.io.IOException

import scalaz.Scalaz._
import scalaz.zio.IO
import scalaz.zio.RTS

//import scala.concurrent.duration._

object DropwizardMetricsMain extends RTS {

  val dropwizardMetrics = new DropwizardMetrics

  def increaseCounter: IO[IOException, Unit] =
    for {
      f <- dropwizardMetrics.counter(Label(Array("test", "counter")))
      a <- f(1)
      b <- f(2)
    } yield b

  def measureGauge: IO[IOException, Unit] =
    for {
      a <- dropwizardMetrics.gauge(Label(Array("test", "gauge")))(IO.point(5L))
    } yield a

  def main(args: Array[String]): Unit = {
    val a = increaseCounter.attempt
      .map(ei => {
        println(
          dropwizardMetrics.registry
            .getCounters()
            .get("test.counter")
            .getCount()
        )
        ei.isRight
      })

    println(unsafeRun(a))

    val b = measureGauge.attempt
      .map(ei => {
        println(
          dropwizardMetrics.registry
            .getGauges()
            .get("test.gauge")
            .getValue
        )
        ei.isRight
      })

    println(unsafeRun(b))
  }
}
