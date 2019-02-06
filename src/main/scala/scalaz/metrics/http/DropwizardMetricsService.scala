package scalaz.metrics.http

import argonaut.Argonaut.jSingleObject
import argonaut.Json
import org.http4s.argonaut._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{ HttpRoutes, Response }
import scalaz.metrics.DropwizardMetrics
import scalaz.metrics.DropwizardReporters.{ dropwizardReportPrinter, jsonDWReporter }
import scalaz.std.list.listInstance
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

object DropwizardMetricsService {
  def service[A]: DropwizardMetrics => HttpRoutes[Task] =
    (metrics: DropwizardMetrics) =>
      HttpRoutes.of[Task] {
        case GET -> Root / filter =>
          val optFilter = if (filter == "ALL") None else Some(filter)
          val m: Json   = dropwizardReportPrinter.report(metrics, optFilter)(jSingleObject)
          Task(Response[Task](Ok).withEntity(m))
      }
}
