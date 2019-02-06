package scalaz.metrics.http

import argonaut.Argonaut.jSingleObject
import argonaut.Json
import org.http4s.argonaut._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{ HttpRoutes, Response }
import scalaz.metrics.PrometheusMetrics
import scalaz.metrics.PrometheusReporters.{ jsonPrometheusReporter, prometheusReportPrinter }
import scalaz.std.list.listInstance
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

object PrometheusMetricsService {
  def service[A]: PrometheusMetrics => HttpRoutes[Task] =
    (metrics: PrometheusMetrics) =>
      HttpRoutes.of[Task] {
        case GET -> Root / filter =>
          val optFilter = if (filter == "ALL") None else Some(filter)
          val m: Json   = prometheusReportPrinter.report(metrics, optFilter)(jSingleObject)
          Task(Response[Task](Ok).withEntity(m))
      }
}
