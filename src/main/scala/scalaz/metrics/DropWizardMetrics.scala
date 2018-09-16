package scalaz.metrics

import com.codahale.metrics.{ Gauge, MetricRegistry }
import com.codahale.metrics.Timer.Context
import scalaz.metrics.Label._
import scalaz.zio.IO
import scalaz.{ Semigroup, Show }

import java.io.IOException

class DropwizardMetrics extends Metrics[IO[IOException, ?], Context] {

  val registry: MetricRegistry = new MetricRegistry()

  type MetriczIO[A] = IO[IOException, A]

  override def counter[L: Show](label: Label[L]): MetriczIO[Long => IO[IOException, Unit]] =
    IO.sync(
      (l: Long) => {
        val lbl = Show[Label[L]].shows(label)
        IO.point(registry.counter(lbl).inc(l))
      }
    )

  override def gauge[A: Semigroup, L: Show](
    label: Label[L]
  )(
    io: MetriczIO[A]
  ): MetriczIO[Unit] = {
    val lbl = Show[Label[L]].shows(label)
    io.map(a => {
        registry.register(lbl, new Gauge[A]() {
          override def getValue: A = a
        })
      })
      .void
  }

  class IOTimer(val ctx: Context) extends Timer[MetriczIO[?], Context] {
    override val a: Context                = ctx
    override def apply: MetriczIO[Context] = IO.point(a)
    override def stop(io: MetriczIO[Context]): MetriczIO[Long] =
      io.map(c => c.stop())
  }

  override def timer[L: Show](label: Label[L]): IO[IOException, Timer[MetriczIO[?], Context]] = {
    val lbl = Show[Label[L]].shows(label)
    val iot = IO.now(registry.timer(lbl))
    val r   = iot.map(t => new IOTimer(t.time()))
    r
  }

  override def histogram[A: Numeric, L: Show](
    label: Label[L],
    res: Reservoir[A]
  )(
    implicit
    num: Numeric[A]
  ): MetriczIO[A => MetriczIO[Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.sync((a: A) => IO.sync(registry.histogram(lbl).update(num.toLong(a))))
  }

  override def meter[L: Show](label: Label[L]): MetriczIO[Double => MetriczIO[Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.point(d => IO.now(registry.meter(lbl)).map(m => m.mark(d.toLong)))
  }
}
