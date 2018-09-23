package scalaz.http

import com.codahale.metrics.MetricRegistry
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{HttpService, Response}
import scalaz.metrics.Reporter
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

object MetricsService {

  val service = (metrics: MetricRegistry) =>
    HttpService[Task] {
      case GET -> Root / filter => {
        val optFilter = if (filter.isEmpty) None else Some(filter)
        val m = Reporter.report(metrics, optFilter)
        Response[Task](Ok).withBody(m.mkString(":"))
      }
  }
}
