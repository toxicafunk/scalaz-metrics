package scalaz.metrics.http

import argonaut.Argonaut.jSingleObject
import argonaut.Json
import org.http4s.argonaut._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{ HttpRoutes, Response }
import scalaz.metrics.Metrics
import scalaz.std.list.listInstance
import scalaz.zio.{ Task, TaskR }
import scalaz.zio.interop.catz._
import scalaz.metrics.http.Server._

trait MetricsService[M <: Metrics[Task[?], Ctx], Ctx] {
  def service: M => HttpRoutes[HttpTask]
}

object MetricsService {

  import com.codahale.metrics.Timer.Context
  import scalaz.metrics.DropwizardMetrics
  import scalaz.metrics.DropwizardReporters.{ dropwizardReportPrinter, jsonDWReporter }
  implicit val dropwizardMetricsService = new MetricsService[DropwizardMetrics, Context] {

    def service: DropwizardMetrics => HttpRoutes[HttpTask] =
      (metrics: DropwizardMetrics) =>
        HttpRoutes.of[HttpTask] {
          case GET -> Root / filter => {
            val optFilter = if (filter == "ALL") None else Some(filter)
            val m: Json   = dropwizardReportPrinter.report(metrics, optFilter)(jSingleObject)
            TaskR(Response[HttpTask](Ok).withEntity(m))
          }
        }
  }

  import io.prometheus.client.Summary.Timer
  import scalaz.metrics.PrometheusMetrics
  import scalaz.metrics.PrometheusReporters.{ jsonPrometheusReporter, prometheusReportPrinter }
  implicit val prometheusMetricsService = new MetricsService[PrometheusMetrics, Timer] {

    def service: PrometheusMetrics => HttpRoutes[HttpTask] =
      (metrics: PrometheusMetrics) =>
        HttpRoutes.of[HttpTask] {
          case GET -> Root / filter =>
            val optFilter = if (filter == "ALL") None else Some(filter)
            val m: Json   = prometheusReportPrinter.report(metrics, optFilter)(jSingleObject)
            TaskR(Response[HttpTask](Ok).withEntity(m))
        }
  }

}
