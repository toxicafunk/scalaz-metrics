package scalaz.metrics

/**
  ALLOWED_CLASSNAMES_LIST = {
  "java.lang.Void",
  "java.lang.Boolean",
  "java.lang.Character",
  "java.lang.Byte",
  "java.lang.Short",
  "java.lang.Integer",
  "java.lang.Long",
  "java.lang.Float",
  "java.lang.Double",
  "java.lang.String",
  "java.math.BigDecimal",
  "java.math.BigInteger",
  "java.util.Date",
  "javax.management.ObjectName",
  CompositeData.class.getName(),
  TabularData.class.getName() }
 **/
sealed trait Measurable
final case class VoidZM() extends Measurable
