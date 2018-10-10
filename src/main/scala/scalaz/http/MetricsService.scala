package scalaz.http

import argonaut.Json
import argonaut.Argonaut.jArray
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.argonaut._
import org.http4s.headers.`Content-Type`
import org.http4s.{HttpService, MediaType, Response}
import scalaz.metrics.{DropwizardMetrics, Reporter}
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._
import scalaz.Scalaz.listInstance

object MetricsService {
  val service = (metrics: DropwizardMetrics) =>
    HttpService[Task] {
      case GET -> Root / filter => {
        val optFilter     = if (filter == "ALL") None else Some(filter)
        val m: List[Json] = Reporter.report(metrics.registry, optFilter)
        Response[Task](Ok)
          .withContentType(`Content-Type`(MediaType.`application/json`))
          .withBody(jArray(m))
      }
  }
}
