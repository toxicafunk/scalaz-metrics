package scalaz.metrics

import com.codahale.metrics.{ Gauge, MetricRegistry }
import scalaz.metrics.Label._
import scalaz.zio.IO
import scalaz.{ Order, Semigroup, Show }

import java.io.IOException

class IOTimer extends Timer[IO[IOException, ?]] {
  override def apply[A](io: IO[IOException, A]): IO[IOException, A] = io
}

object IOTimer {

  def apply[A](io: IO[IOException, A]): IOTimer = {
    val t = new IOTimer()
    t.apply(io)
    t
  }
}

class DropwizardMetrics extends Metrics[IO[IOException, ?]] {

  val registry: MetricRegistry = new MetricRegistry()

  override def counter[L: Show](label: Label[L]): IO[IOException, Long => IO[IOException, Unit]] =
    IO.sync(
      (l: Long) => {
        val lbl = Show[Label[L]].shows(label)
        IO.point(registry.counter(lbl).inc(l))
      }
    )

  override def gauge[A: Semigroup, L: Show](
    label: Label[L]
  )(
    io: IO[IOException, A]
  ): IO[IOException, Unit] = {
    val lbl = Show[Label[L]].shows(label)
    io.map(a => {
        registry.register(lbl, new Gauge[A]() {
          override def getValue: A = a
        })
      })
      .void
  }

  override def timer[L: Show](label: Label[L]): IO[IOException, Timer[IO[IOException, ?]]] = {
    val lbl = Show[Label[L]].shows(label)
    val ctx = IO.point(registry.timer(lbl).time())
    val t   = IOTimer(ctx)
    IO.point(t)
  }

  override def histogram[A: Order, L: Show](
    label: Label[L],
    res: Resevoir[A]
  )(
    implicit
    num: Numeric[A]
  ): IO[IOException, A => IO[IOException, Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.point((a: A) => IO.point(registry.histogram(lbl).update(num.toLong(a))))
  }

  override def meter[L: Show](label: Label[L]): IO[IOException, Double => IO[IOException, Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.point(d => IO.point(registry.meter(lbl).mark(d.toLong)))
  }

}
