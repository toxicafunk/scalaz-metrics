package scalaz.metrics

import com.codahale.metrics.MetricRegistry.MetricSupplier
import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{Reservoir => DWReservoir, _}
import scalaz.metrics.Label._
import scalaz.metrics.Reservoir._
import scalaz.zio.{IO, Task, UIO, ZIO}
import scalaz.{Semigroup, Show}

class DropwizardMetrics extends Metrics[Task[?], Context] {

  val registry: MetricRegistry = new MetricRegistry()

  override def counter[L: Show](label: Label[L]): Task[Long => UIO[Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.effect(
      (l: Long) => {
        IO.succeedLazy(registry.counter(lbl).inc(l))
      }
    )
  }

  override def gauge[A, B: Semigroup, L: Show](
    label: Label[L]
  )(f: Option[A] => B): Task[Option[A] => UIO[Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.effect(
      (op: Option[A]) =>
        IO.succeedLazy({
          registry.register(lbl, new Gauge[B]() {
            override def getValue: B = f(op)
          })
          ()
        })
    )
  }

  class IOTimer(val ctx: Context) extends Timer[Task[?], Context] {
    override val a: Context                = ctx
    override def start: Task[Context] = IO.succeed(a)
    override def stop(io: Task[Context]): Task[Double] =
      io.map(c => c.stop().toDouble)
  }

  override def timer[L: Show](label: Label[L]): ZIO[Any, Nothing, IOTimer] = {
    val lbl = Show[Label[L]].shows(label)
    val iot = IO.succeed(registry.timer(lbl))
    val r   = iot.map(t => new IOTimer(t.time()))
    r
  }

  override def histogram[A: Numeric, L: Show](
    label: Label[L],
    res: Reservoir[A]
  )(
    implicit
    num: Numeric[A]
  ): Task[A => Task[Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    val reservoir: DWReservoir = res match {
      case Uniform(config @ _)               => new UniformReservoir
      case ExponentiallyDecaying(config @ _) => new ExponentiallyDecayingReservoir
      case Bounded(window, unit)             => new SlidingTimeWindowReservoir(window, unit)
    }
    val supplier = new MetricSupplier[Histogram] {
      override def newMetric(): Histogram = new Histogram(reservoir)
    }

    IO.effect((a: A) => IO.effect(registry.histogram(lbl, supplier).update(num.toLong(a))))
  }

  override def meter[L: Show](label: Label[L]): Task[Double => Task[Unit]] = {
    val lbl = Show[Label[L]].shows(label)
    IO.effect(d => IO.succeed(registry.meter(lbl)).map(m => m.mark(d.toLong)))
  }
}

object DropwizardMetrics {
  def makeFilter(filter: Option[String]): MetricFilter = filter match {
    case Some(s) =>
      s.charAt(0) match {
        case '+' => MetricFilter.startsWith(s.substring(1))
        case '-' => MetricFilter.endsWith(s.substring(1))
        case _   => MetricFilter.contains(s)
      }
    case _ => MetricFilter.ALL
  }
}
