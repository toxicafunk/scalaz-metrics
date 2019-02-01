package scalaz.metrics

import java.io.IOException
import java.util
import java.util.{HashSet, Set}

import scalaz.std.string.stringInstance
import scalaz.Scalaz._
import scalaz.zio.{IO, RTS}
import testz.{Harness, PureHarness, assert}
import scalaz.metrics.PrometheusMetrics.DoubleSemigroup

object PrometheusTests extends RTS {

  val prometheusMetrics = new PrometheusMetrics

  val testCounter: IO[IOException, Unit] = for {
    f <- prometheusMetrics.counter(Label(Array("test", "counter"), ""))
    a <- f(1)
    b <- f(2)
  } yield b

  val testGauge: (Option[Double] => Double) => IO[IOException, Unit] = (f: Option[Double] => Double) =>
    for {
      g <- prometheusMetrics.gauge[Double, Double, String](Label(Array("test", "gauge"), ""))(f)
      a <- g(5.0.some)
      b <- g((-3.0).some)
    } yield b

  /*val testTimer: IO[IOException, List[Long]] = for {
    t <- prometheusMetrics.timer(Label(Array("test", "timer")))
    l <- IO.traverse(
          List(
            Thread.sleep(1000L),
            Thread.sleep(1400L),
            Thread.sleep(1200L)
          )
        )(a => t.stop(t.apply))
  } yield l*/

  val testHistogram: IO[IOException, Unit] = {
    import scala.math.Numeric.IntIsIntegral
    for {
      h <- prometheusMetrics.histogram(Label(Array("test", "hist"), ""))
      _ <- IO.foreach(List(h(10), h(25), h(50), h(57), h(19)))(_.void)
    } yield ()
  }

  val testHistogramTimer: IO[IOException, Unit] = {
    import scala.math.Numeric.IntIsIntegral
    for {
      h <- prometheusMetrics.histogramTimer(Label(Array("test", "tid"), ""))
      _ <- IO.foreach(List(h(), h(), h(), h(), h()))(io => {
        Thread.sleep(500)
        io.void
      })
    } yield ()
  }

  /*val testMeter: IO[IOException, Unit] = for {
    m <- prometheusMetrics.meter(Label(Array("test", "meter")))
    _ <- IO.traverse(1 to 5)(i => IO.now(m(1)))
  } yield ()*/

  def tests[T](harness: Harness[T]): T = {
    import harness._
    section(
      test("counter increases by `inc` amount") { () =>
        unsafeRun(testCounter)
        val set: util.Set[String] = new util.HashSet[String]()
        set.add("testcounter")
        val counter = prometheusMetrics.registry
          .filteredMetricFamilySamples(set).nextElement().samples.get(0).value
        assert(counter == 3.0)
      },
      test("gauge returns latest value") { () =>
        val tester: Option[Double] => Double = (op: Option[Double]) => op.get
        unsafeRun(testGauge(tester))
        val set: Set[String] = new HashSet[String]()
        set.add("testgauge")
        val a1 = prometheusMetrics.registry
          .filteredMetricFamilySamples(set).nextElement().samples.get(0).value

        assert(a1 == 2.0)
      },
      /*test("Timer called 3 times") { () =>
        unsafeRun(testTimer)
        val counter = prometheusMetrics.registry
          .getTimers()
          .get("test.timer")
          .getCount

        assert(counter == 3)
      },
      test("Timer mean rate within bounds") { () =>
        unsafeRun(testTimer)
        val meanRate = prometheusMetrics.registry
          .getTimers()
          .get("test.timer")
          .getMeanRate

        assert(meanRate > 0.78 && meanRate < 0.84)
      },*/
      test("Histogram sum is 161 and count is 5") { () =>
        unsafeRun(testHistogram)
        val set: Set[String] = new HashSet[String]()
        set.add("testhist_count")
        set.add("testhist_sum")
        val count = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(0).value
        assert(count == 5.0)
        val sum = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(1).value
        assert(sum == 161.0)
      },
      test("Histogram timer") { () =>
        unsafeRun(testHistogramTimer)
        val set: Set[String] = new HashSet[String]()
        set.add("testtid_count")
        set.add("testtid_sum")
        val count = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(0).value
        assert(count == 5.0)
        val sum = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(1).value
        assert(sum > 2.5)
      },
      /*test("Meter invoked 5 times") { () =>
        unsafeRun(testMeter)
        val counter = prometheusMetrics.registry
          .getMeters()
          .get("test.meter")
          .getCount

        assert(counter == 5)
      },
      test("Meter mean rate within bounds") { () =>
        unsafeRun(testMeter)
        val meanRate = prometheusMetrics.registry
          .getMeters()
          .get("test.meter")
          .getMeanRate

        assert(meanRate > 720 && meanRate < 9000)
      }*/
    )
  }

  val harness: Harness[PureHarness.Uses[Unit]] =
    PureHarness.makeFromPrinter((result, name) => {
      println(s"${name.reverse.mkString("[\"", "\"->\"", "\"]:")} $result")
    })

  def main(args: Array[String]): Unit =
    tests(harness)((), Nil).print()
}
