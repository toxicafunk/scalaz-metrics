package scalaz.metrics

import java.util

import scalaz.Scalaz._
import scalaz.metrics.PrometheusMetrics.DoubleSemigroup
import scalaz.std.string.stringInstance
import scalaz.zio.{ DefaultRuntime, IO, Task }
import testz.{ assert, Harness, PureHarness, Result }

object PrometheusTests extends DefaultRuntime {

  val prometheusMetrics = new PrometheusMetrics

  val testCounter: Task[Unit] = for {
    f <- prometheusMetrics.counter(Label(Array("test", "counter"), ""))
    _ <- f(1)
    b <- f(2)
  } yield b

  val testGauge: (Option[Double] => Double) => Task[Unit] = (f: Option[Double] => Double) =>
    for {
      g <- prometheusMetrics.gauge[Double, Double, String](Label(Array("test", "gauge"), ""))(f)
      _ <- g(5.0.some)
      b <- g((-3.0).some)
    } yield b

  val testTimer: Task[List[Double]] = for {
    t  <- prometheusMetrics.timer(Label(Array("test", "timer"), ""))
    t1 = t.start
    l <- IO.foreach(
          List(
            Thread.sleep(1000L),
            Thread.sleep(1400L),
            Thread.sleep(1200L)
          )
        )(_ => t.stop(t1))
  } yield l

  val testHistogram: Task[Unit] = {
    import scala.math.Numeric.IntIsIntegral
    for {
      h <- prometheusMetrics.histogram(Label(Array("test", "hist"), ""))
      _ <- IO.foreach(List(h(10), h(25), h(50), h(57), h(19)))(_.unit)
    } yield ()
  }

  val testHistogramTimer: Task[Unit] = {
    import scala.math.Numeric.IntIsIntegral
    for {
      h <- prometheusMetrics.histogramTimer(Label(Array("test", "tid"), ""))
      _ <- IO.foreach(List(h(), h(), h(), h(), h()))(io => {
            Thread.sleep(500)
            io.unit
          })
    } yield ()
  }

  val testMeter: Task[Unit] = for {
    m <- prometheusMetrics.meter(Label(Array("test", "meter"), ""))
    _ <- IO.foreach(1 to 5)(_ => IO.succeed(m(2)))
  } yield ()

  def tests[T](harness: Harness[T]): T = {
    import harness._
    section(
      test("counter increases by `inc` amount") { () =>
        unsafeRun(testCounter)
        val set: util.Set[String] = new util.HashSet[String]()
        set.add("testcounter")
        val counter = prometheusMetrics.registry
          .filteredMetricFamilySamples(set)
          .nextElement()
          .samples
          .get(0)
          .value
        assert(counter == 3.0)
      },
      test("gauge returns latest value") { () =>
        val tester: Option[Double] => Double = (op: Option[Double]) => op.getOrElse(0.0)
        unsafeRun(testGauge(tester))
        val set: util.Set[String] = new util.HashSet[String]()
        set.add("testgauge")
        val a1 = prometheusMetrics.registry
          .filteredMetricFamilySamples(set)
          .nextElement()
          .samples
          .get(0)
          .value

        assert(a1 == 2.0)
      },
      test("Timer called 3 times") { () =>
        unsafeRun(testTimer)
        val set: util.Set[String] = new util.HashSet[String]()
        set.add("testtimer_count")
        set.add("testtimer_sum")
        val count = prometheusMetrics.registry
          .filteredMetricFamilySamples(set)
          .nextElement()
          .samples
          .get(0)
          .value
        val sum = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(1).value

        Result.combine(assert(count == 3.0), assert(sum >= 3.6))
      },
      test("Histogram sum is 161 and count is 5") { () =>
        unsafeRun(testHistogram)
        val set: util.Set[String] = new util.HashSet[String]()
        set.add("testhist_count")
        set.add("testhist_sum")

        val count = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(0).value
        val sum   = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(1).value
        Result.combine(assert(count == 5.0), assert(sum == 161.0))
      },
      test("Histogram timer") { () =>
        unsafeRun(testHistogramTimer)
        val set: util.Set[String] = new util.HashSet[String]()
        set.add("testtid_count")
        set.add("testtid_sum")
        val count = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(0).value

        val sum = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(1).value
        Result.combine(assert(count == 5.0), assert(sum > 2.5))
      },
      test("Meter invoked 5 times") { () =>
        unsafeRun(testMeter)
        val set: util.Set[String] = new util.HashSet[String]()
        set.add("testmeter_count")
        set.add("testmeter_sum")
        val count = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(0).value
        val sum   = prometheusMetrics.registry.filteredMetricFamilySamples(set).nextElement().samples.get(1).value
        Result.combine(assert(count == 5.0), assert(sum >= 10.0))
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
