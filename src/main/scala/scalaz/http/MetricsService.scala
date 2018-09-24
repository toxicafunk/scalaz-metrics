package scalaz.http

import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{ HttpService, Response }
import scalaz.metrics.{ DropwizardMetrics, Reporter }
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

object MetricsService {
  val service = (metrics: DropwizardMetrics) =>
    HttpService[Task] {
      case GET -> Root / filter => {
        val optFilter = if (filter == "ALL") None else Some(filter)
        val m         = Reporter.report(metrics.registry, optFilter)
        println(s"Map: $m")
        Response[Task](Ok).withBody(m)
      }
    }
}
