package scalaz.metrics
import argonaut.Argonaut._
import argonaut.Json
import io.prometheus.client.Summary
import scalaz.{ Foldable, Monoid }
import java.util

import scala.collection.JavaConverters._

object PrometheusReporters {

  implicit val prometheusReportPrinter: ReportPrinter[Summary.Timer, PrometheusMetrics] =
    new ReportPrinter[Summary.Timer, PrometheusMetrics] {
      override def report[F[_], A](metrics: PrometheusMetrics, filter: Option[String])(
        cons: (String, A) => A
      )(implicit M: Monoid[A], L: Foldable[F], R: Reporter[Summary.Timer, PrometheusMetrics, F, A]): A = {

        import scalaz.syntax.semigroup._

        val fs = List(
          ("counters", R.extractCounters),
          ("gauges", R.extractGauges),
          ("timers", R.extractTimers),
          ("histograms", R.extractHistograms),
          ("meters", R.extractMeters)
        )

        fs.foldLeft(M.zero)((acc0, f) => {
          val m = f._2(metrics)(filter)
          acc0 |+| L.foldMap(m)(a => cons(f._1, a))
        })
      }
    }

  val metricFamily2Json: (PrometheusMetrics, util.Set[String]) => List[Json] =
    (metrics: PrometheusMetrics, filter: util.Set[String]) => {
      val filteredSamples = metrics.registry.filteredMetricFamilySamples(filter)
      filteredSamples.asScala
        .map(familySamples => {
          val json = familySamples.samples.asScala
            .map(sample => jObjectAssocList(List(sample.name -> jNumberOrNull(sample.value))))
            .toList
          println(s"json: $json")
          jSingleObject(familySamples.name, jArray(json))
        })
        .toList
    }

  implicit val jsonPrometheusReporter: Reporter[Summary.Timer, PrometheusMetrics, List, Json] =
    new Reporter[Summary.Timer, PrometheusMetrics, List, Json] {
      override val extractCounters: PrometheusMetrics => Filter => List[Json] =
        (metrics: PrometheusMetrics) =>
          (filter: Filter) => {
            val set: util.Set[String] = new util.HashSet[String]()
            set.add(filter.getOrElse("test_counter"))
            metricFamily2Json(metrics, set)
          }

      override val extractGauges: PrometheusMetrics => Filter => List[Json] =
        (metrics: PrometheusMetrics) =>
          (filter: Filter) => {
            val set: util.Set[String] = new util.HashSet[String]()
            set.add(filter.getOrElse("test_gauge"))
            metricFamily2Json(metrics, set)
          }

      override val extractTimers: PrometheusMetrics => Filter => List[Json] =
        (metrics: PrometheusMetrics) =>
          (filter: Filter) => {
            val set: util.Set[String] = new util.HashSet[String]()
            set.add(filter.getOrElse("test_timer"))
            set.add(filter.getOrElse("test_timer_count"))
            set.add(filter.getOrElse("test_timer_sum"))
            metricFamily2Json(metrics, set)
          }

      override val extractHistograms: PrometheusMetrics => Filter => List[Json] =
        (metrics: PrometheusMetrics) =>
          (filter: Filter) => {
            val set: util.Set[String] = new util.HashSet[String]()
            set.add(filter.getOrElse("test_histogram"))
            set.add(filter.getOrElse("test_histogram_count"))
            set.add(filter.getOrElse("test_histogram_sum"))
            set.add(filter.getOrElse("test_histogram_bucket"))
            metricFamily2Json(metrics, set)
          }

      override val extractMeters: PrometheusMetrics => Filter => List[Json] =
        (metrics: PrometheusMetrics) =>
          (filter: Filter) => {
            val set: util.Set[String] = new util.HashSet[String]()
            set.add(filter.getOrElse("test_meter"))
            set.add(filter.getOrElse("test_meter_count"))
            set.add(filter.getOrElse("test_meter_sum"))
            metricFamily2Json(metrics, set)
          }
    }

}
