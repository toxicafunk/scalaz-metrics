package scalaz.metrics
import java.math.BigInteger
import java.util.Date

/**
 *ALLOWED_CLASSNAMES_LIST = {
 *"java.lang.Void",
 *"java.lang.Boolean",
 *"java.lang.Character",
 *"java.lang.Byte",
 *"java.lang.Short",
 *"java.lang.Integer",
 *"java.lang.Long",
 *"java.lang.Float",
 *"java.lang.Double",
 *"java.lang.String",
 *"java.math.BigDecimal",
 *"java.math.BigInteger",
 *"java.util.Date",
 *"javax.management.ObjectName",
 *CompositeData.class.getName(),
 *TabularData.class.getName() }
 **/
trait Measurable                      extends Any
class VoidZ(val z: Unit)              extends AnyVal with Measurable
class BooleanZ(val z: Boolean)        extends AnyVal with Measurable
class CharacterZ(val z: Character)    extends AnyVal with Measurable
class ByteZ(val z: Byte)              extends AnyVal with Measurable
class ShortZ(val z: Short)            extends AnyVal with Measurable
class IntegerZ(val z: Integer)        extends AnyVal with Measurable
class LongZ(val z: Long)              extends AnyVal with Measurable
class FloatZ(val z: Float)            extends AnyVal with Measurable
class DoubleZ(val z: Double)          extends AnyVal with Measurable
class StringZ(val z: String)          extends AnyVal with Measurable
class BigDecimalZ(val z: BigDecimal)  extends AnyVal with Measurable
class BigIntegerzZ(val z: BigInteger) extends AnyVal with Measurable
class DateZ(val z: Date)              extends AnyVal with Measurable
