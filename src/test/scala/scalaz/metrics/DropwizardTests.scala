package scalaz.metrics

import java.io.IOException

import scalaz.Scalaz._
import scalaz.zio.{ IO, RTS }
import testz.{ assert, Harness, PureHarness }

object DropwizardTests extends RTS {

  val dropwizardMetrics = new DropwizardMetrics

  val testCounter: IO[IOException, Unit] = for {
    f <- dropwizardMetrics.counter(Label(Array("test", "counter")))
    a <- f(1)
    b <- f(2)
  } yield b

  val testGauge: (Option[Unit] => Long) => IO[IOException, Unit] = (f: Option[Unit] => Long) =>
    for {
      a <- dropwizardMetrics.gauge[Unit, Long, String](Label(Array("test", "gauge")))(f)
      r <- a(None)
    } yield r

  val testTimer: IO[IOException, List[Double]] = for {
    t  <- dropwizardMetrics.timer(Label(Array("test", "timer")))
    t1 = t.start
    l <- IO.foreach(
          List(
            Thread.sleep(1000L),
            Thread.sleep(1400L),
            Thread.sleep(1200L)
          )
        )(a => t.stop(t1))
  } yield l

  val testHistogram: IO[IOException, Unit] = {
    import scala.math.Numeric.IntIsIntegral
    for {
      h <- dropwizardMetrics.histogram(Label(Array("test", "histogram")))
      _ <- IO.foreach(List(h(10), h(25), h(50), h(57), h(19)))(_.void)
    } yield ()
  }

  val testMeter: IO[IOException, Unit] = for {
    m <- dropwizardMetrics.meter(Label(Array("test", "meter")))
    _ <- IO.foreach(1 to 5)(i => IO.succeed(m(1)))
  } yield ()

  def tests[T](harness: Harness[T]): T = {
    import harness._
    section(
      test("counter increases by `inc` amount") { () =>
        unsafeRun(testCounter)
        val counter = dropwizardMetrics.registry
          .getCounters()
          .get("test.counter")
          .getCount

        assert(counter == 3)
      },
      test("gauge returns latest value") { () =>
        val tester: Option[Unit] => Long = (op: Option[Unit]) => System.nanoTime()
        unsafeRun(testGauge(tester))
        val a1 = dropwizardMetrics.registry
          .getGauges()
          .get("test.gauge")
          .getValue
          .asInstanceOf[Long]

        val a2 = dropwizardMetrics.registry
          .getGauges()
          .get("test.gauge")
          .getValue
          .asInstanceOf[Long]

        assert(a1 < a2)
      },
      test("Timer called 3 times") { () =>
        unsafeRun(testTimer)
        val counter = dropwizardMetrics.registry
          .getTimers()
          .get("test.timer")
          .getCount

        assert(counter == 3)
      },
      test("Timer mean rate for 6 calls within bounds") { () =>
        unsafeRun(testTimer)
        val meanRate = dropwizardMetrics.registry
          .getTimers()
          .get("test.timer")
          .getMeanRate

        assert(meanRate > 0.78 && meanRate < 0.84)
      },
      test("Histogram 75th percentile is 50.0") { () =>
        unsafeRun(testHistogram)
        val perc75th = dropwizardMetrics.registry
          .getHistograms()
          .get("test.histogram")
          .getSnapshot
          .get75thPercentile

        assert(perc75th == 50.0)
      },
      test("Histogram 99.9th percentile is 57.0") { () =>
        unsafeRun(testHistogram)
        val perc99th = dropwizardMetrics.registry
          .getHistograms()
          .get("test.histogram")
          .getSnapshot
          .get999thPercentile

        assert(perc99th == 57.0)
      },
      test("Meter invoked 5 times") { () =>
        unsafeRun(testMeter)
        val counter = dropwizardMetrics.registry
          .getMeters()
          .get("test.meter")
          .getCount

        assert(counter == 5)
      },
      test("Meter mean rate within bounds") { () =>
        unsafeRun(testMeter)
        val meanRate = dropwizardMetrics.registry
          .getMeters()
          .get("test.meter")
          .getMeanRate

        assert(meanRate > 720 && meanRate < 9000)
      }
    )
  }

  val harness: Harness[PureHarness.Uses[Unit]] =
    PureHarness.makeFromPrinter((result, name) => {
      println(s"${name.reverse.mkString("[\"", "\"->\"", "\"]:")} $result")
    })

  def main(args: Array[String]): Unit =
    tests(harness)((), Nil).print()
}
