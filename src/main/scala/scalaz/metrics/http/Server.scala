package scalaz.metrics.http

import cats.data.Kleisli
import org.http4s.server.blaze._
import org.http4s.{ Request, Response }
import scalaz.metrics.Metrics
import scalaz.zio.{ Task, TaskR, ZIO }
import scalaz.zio.clock.Clock
import scalaz.zio.interop.catz._
import scalaz.zio.scheduler.Scheduler
import scala.util.Properties.envOrNone

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  type HttpEnvironment = Clock with Scheduler
  type HttpTask[A]     = TaskR[HttpEnvironment, A]

  type KleisliApp = Kleisli[HttpTask, Request[HttpTask], Response[HttpTask]]

  type HttpApp[Ctx] = Metrics[Task[?], Ctx] => KleisliApp

  /*def httpApp[M: Metrics[Task[?], Ctx], Ctx]: HttpApp[Ctx] =
    (metrics: M) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> dropwizardMetricsService.service(metrics)
      ).orNotFound*/

  def builder[Ctx]: KleisliApp => HttpTask[Unit] =
    (app: Kleisli[HttpTask, Request[HttpTask], Response[HttpTask]]) =>
      ZIO
        .runtime[HttpEnvironment]
        .flatMap { implicit rts =>
          BlazeServerBuilder[HttpTask]
            .bindHttp(port)
            .withHttpApp(app)
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
