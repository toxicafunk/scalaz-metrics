package scalaz.metrics.http

import argonaut.Argonaut.jSingleObject
import argonaut.Json
import org.http4s.argonaut._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Response}
import scalaz.metrics.DropwizardMetrics
import scalaz.metrics.DropwizardReporters.{dropwizardReportPrinter, jsonDWReporter}
import scalaz.std.list.listInstance
import scalaz.zio.{DefaultRuntime, TaskR}
import scalaz.zio.interop.catz._

object DropwizardMetricsService {
  type HttpTask[A] = TaskR[DefaultRuntime, A]
  def service[A]: DropwizardMetrics => HttpRoutes[HttpTask] =
    (metrics: DropwizardMetrics) =>
      HttpRoutes.of[HttpTask] {
        case GET -> Root / filter => {
          val optFilter = if (filter == "ALL") None else Some(filter)
          val m: Json   = dropwizardReportPrinter.report(metrics, optFilter)(jSingleObject)
          TaskR(Response[HttpTask](Ok).withEntity(m))
        }
      }
}
