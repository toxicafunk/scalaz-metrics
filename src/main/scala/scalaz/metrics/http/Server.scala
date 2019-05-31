package scalaz.metrics.http

import cats.data.Kleisli
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.{ Request, Response }
import scalaz.metrics.DropwizardMetrics
import scalaz.zio.{ TaskR, ZIO }
import scalaz.zio.clock.Clock
import scalaz.zio.interop.catz._
import scalaz.zio.scheduler.Scheduler
import scala.util.Properties.envOrNone

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  type HttpEnvironment = Clock with Scheduler
  type HttpTask[A]     = TaskR[HttpEnvironment, A]

  type HttpApp = DropwizardMetrics => Kleisli[HttpTask, Request[HttpTask], Response[HttpTask]]

  val httpApp: HttpApp =
    (metrics: DropwizardMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> DropwizardMetricsService.service(metrics)
      ).orNotFound

  val builder: (HttpApp, DropwizardMetrics) => HttpTask[Unit] = (app: HttpApp, metrics: DropwizardMetrics) =>
    ZIO
      .runtime[HttpEnvironment]
      .flatMap { implicit rts =>
        BlazeServerBuilder[HttpTask]
          .bindHttp(port)
          .withHttpApp(app(metrics))
          .serve
          .compile
          .drain
      }
      .provideSome[HttpEnvironment] { base =>
        new Clock with Scheduler {
          override val scheduler: Scheduler.Service[Any] = base.scheduler
          override val clock: Clock.Service[Any]         = base.clock
        }
      }
}
