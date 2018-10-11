package scalaz.http

import argonaut.Json
import argonaut.Argonaut.jSingleObject
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.argonaut._
import org.http4s.{ HttpService, Response }
import scalaz.metrics.{ DropwizardMetrics, Reporter }
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._
import scalaz.std.list.listInstance
import scalaz.metrics.Reporter.jsonReporter

object MetricsService {
  val service: DropwizardMetrics => HttpService[Task] = (metrics: DropwizardMetrics) =>
    HttpService[Task] {
      case GET -> Root / filter =>
        val optFilter = if (filter == "ALL") None else Some(filter)
        val m: Json   = Reporter.report(metrics.registry, optFilter)(jSingleObject)
        Response[Task](Ok).withBody(m)
    }
}
