package scalaz.metrics

import com.codahale.metrics.{Gauge, MetricRegistry}
import scalaz.metrics.Label._
import scalaz.zio.IO
import scalaz.{Order, Semigroup, Show}

class IOTimer extends Timer[IO[Nothing, ?]] {
  override def apply[A](io: IO[Nothing, A]): IO[Nothing, A] = io
}

object IOTimer {
  def apply[A](io: IO[Nothing, A]): IOTimer = {
    val t = new IOTimer()
    t.apply(io)
    t
  }
}

class DropwizardMetrics[C[_]] extends Metrics[C, IO[Nothing, ?]] {

  val registry: MetricRegistry = new MetricRegistry()

  override def counter[L: Show](label: Label[L]): IO[Nothing, Long => IO[Nothing, Unit]] =
    IO.sync(
      (l: Long) => {
        val lbl = Show[Label[L]].shows(label)
        IO.point(registry.counter(lbl).inc(l))
      }
    )

  override def gauge[A: Semigroup: C, L: Show](label: Label[L])(io: IO[Nothing, A]): IO[Nothing, Unit] = {
    val lbl = Show[Label[L]].shows(label)
    io.map(a => {
      registry.register(lbl, new Gauge[A]() {
        override def getValue: A = a
      })
    }).toUnit
  }

  override def timer[L: Show](label: Label[L]): IO[Nothing, Timer[IO[Nothing, ?]]] = {
    val lbl = Show[Label[L]].shows(label)
    val ctx = IO.point(registry.timer(lbl).time())
    val t = IOTimer(ctx)
    IO.point(t)
  }

  override def histogram[A: Order: C, L: Show](label: Label[L], res: Resevoir[A])
                                              (implicit num: Numeric[A]):
    IO[Nothing, A => IO[Nothing, Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.point((a: A) => IO.point(registry.histogram(lbl).update(num.toLong(a))))
  }


  override def meter[L: Show](label: Label[L]):
    IO[Nothing, Double => IO[Nothing, Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.point(d => IO.point(registry.meter(lbl).mark(d.toLong)))
  }

}
