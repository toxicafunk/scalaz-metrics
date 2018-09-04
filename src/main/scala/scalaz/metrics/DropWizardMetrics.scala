package scalaz.metrics

import scalaz.zio.IO
import scalaz.{Semigroup, Show}
import com.codahale.metrics.{DerivativeGauge, Gauge, MetricRegistry}
import scalaz.metrics.Label._

trait DropwizardMetrics[C[_]] extends Metrics[C, IO[Nothing, ?]] {

  val registry: MetricRegistry = new MetricRegistry()

  override def counter[L: Show](label: Label[L]): IO[Nothing, Long => IO[Nothing, Unit]] =
    IO.sync(
      (l: Long) => {
        val lbl = Show[Label[L]].shows(label)
        IO.point(registry.counter(lbl).inc(l))
      }
    )

  def gauge[A: Semigroup: C, L: Show](label: Label[L])(io: IO[Nothing, A]): IO[Nothing, Unit] = {
    val lbl = Show[Label[L]].shows(label)
    IO.point(registry.register(lbl, new Gauge[A] {
      override def getValue: A = {
        IO.absolve(io.attempt) match {
          case a: A => a
        }
      }
    }))
  }

  /*def timer[L: Show](label: Label[L]): IO[Nothing, Timer[IO]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.point(registry.timer(lbl).time())
  }*/
}
