package scalaz.metrics.http

import cats.data.Kleisli
import cats.effect
import cats.effect.Timer
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.{ Request, Response }
import scalaz.metrics.{ DropwizardMetrics, _ }
import scalaz.zio.{ TaskR, ZIO }
import scalaz.zio.clock.Clock
import scalaz.zio.duration.Duration
import scalaz.zio.interop.catz._

import scala.concurrent.duration.{ FiniteDuration, NANOSECONDS, TimeUnit }
import scala.util.Properties.envOrNone
import scalaz.zio.DefaultRuntime

object Server {
  val port: Int = envOrNone("HTTP_PORT").fold(9090)(_.toInt)

  type HttpTask[A] = TaskR[DefaultRuntime, A]

  implicit val timer: Timer[HttpTask] = new Timer[HttpTask] {
    val zioClock = Clock.Live.clock

    override def clock: effect.Clock[HttpTask] = new effect.Clock[HttpTask] {
      override def realTime(unit: TimeUnit) = zioClock.nanoTime.map(unit.convert(_, NANOSECONDS))

      override def monotonic(unit: TimeUnit) = zioClock.currentTime(unit)
    }

    override def sleep(duration: FiniteDuration): HttpTask[Unit] = zioClock.sleep(Duration.fromScala(duration))
  }

  val httpApp: DropwizardMetrics => Kleisli[HttpTask, Request[HttpTask], Response[HttpTask]] =
    (metrics: DropwizardMetrics) =>
      Router(
        "/"        -> StaticService.service,
        "/metrics" -> DropwizardMetricsService.service(metrics)
      ).orNotFound

  val builder: DropwizardMetrics => HttpTask[Unit] = (metrics: DropwizardMetrics) =>
    ZIO.runtime[DefaultRuntime].flatMap { implicit rts =>
      BlazeServerBuilder[HttpTask]
        .bindHttp(port)
        .withHttpApp(httpApp(metrics))
        .serve
        .compile
        .drain
    }
}
