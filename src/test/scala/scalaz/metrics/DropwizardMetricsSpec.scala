package scalaz.metrics
import java.io.IOException

import scalaz.Scalaz._
import scalaz.zio.{ App, IO }

import scala.math.Numeric.IntIsIntegral

object DropwizardMetricsSpec extends App {

  val dropwizardMetrics = new DropwizardMetrics

  val tester: () => Long = () => System.nanoTime()

  def performTests: IO[IOException, Unit] =
    for {
      f <- dropwizardMetrics.counter(Label(Array("test", "counter")))
      _ <- f(1)
      _ <- f(2)
      _ <- dropwizardMetrics.gauge(Label(Array("test", "gauge")))(IO.sync(tester))
      t <- dropwizardMetrics.timer(Label(Array("test", "timer")))
      l <- IO.traverse(
            List(
              Thread.sleep(1000L),
              Thread.sleep(1400L),
              Thread.sleep(1200L)
            )
          )(a => t.stop(t.apply))
      h <- dropwizardMetrics.histogram(Label(Array("test", "histogram")))
      _ <- IO.traverse(List(h(10), h(25), h(50), h(57), h(19)))(_.void)
      m <- dropwizardMetrics.meter(Label(Array("test", "meter")))
      _ <- IO.traverse(1 to 5)(i => IO.now(m(1)))
    } yield { println(s"time $l ns"); () }

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    performTests.attempt
      .map(ei => {
        printMetrics()
        ei.fold(_ => 1, _ => 0)
      })
      .map(ExitStatus.ExitNow(_))

  def printMetrics(): Unit = {
    println(
      dropwizardMetrics.registry
        .getCounters()
        .get("test.counter")
        .getCount
    )
    println(
      dropwizardMetrics.registry
        .getGauges()
        .get("test.gauge")
        .getValue
    )
    println(
      dropwizardMetrics.registry
        .getTimers()
        .get("test.timer")
        .getCount
    )
    println(
      dropwizardMetrics.registry
        .getTimers()
        .get("test.timer")
        .getMeanRate
    )
    println(
      dropwizardMetrics.registry
        .getHistograms()
        .get("test.histogram")
        .getSnapshot
        .get75thPercentile
    )
    println(
      dropwizardMetrics.registry
        .getHistograms()
        .get("test.histogram")
        .getSnapshot
        .get99thPercentile
    )
    println(
      dropwizardMetrics.registry
        .getHistograms()
        .get("test.histogram")
        .getSnapshot
        .getMean
    )
    println(
      dropwizardMetrics.registry
        .getMeters()
        .get("test.meter")
        .getMeanRate
    )
    println(
      dropwizardMetrics.registry
        .getMeters()
        .get("test.meter")
        .getCount
    )
  }
}
