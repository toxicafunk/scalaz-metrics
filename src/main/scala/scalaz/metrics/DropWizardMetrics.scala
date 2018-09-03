package scalaz.metrics

import scalaz.zio.IO
import com.codahale.metrics.Counter
import com.codahale.metrics.MetricRegistry

trait DropwizardMetrics[C[_], E, L] extends Metrics[C, IO[E, ?], L] {

  val registry: MetricRegistry = new MetricRegistry()

  override def counter(label: Label[L]): IO[E, Long => IO[E, Unit]] = {
    IO.syncException(registry.counter(label.toMetricName))
  }
}
 
