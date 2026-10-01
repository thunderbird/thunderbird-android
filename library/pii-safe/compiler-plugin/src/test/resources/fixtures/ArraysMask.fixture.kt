import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class ArraysMask(
    @get:PiiSafe.Mask
    val list: List<String>,
    @get:PiiSafe.Mask
    val map: Map<String, String>,
    @get:PiiSafe.Mask
    val array: Array<Any>,
    @get:PiiSafe.Mask
    val byteArray: ByteArray,
    @get:PiiSafe.Mask
    val shortArray: ShortArray,
    @get:PiiSafe.Mask
    val intArray: IntArray,
    @get:PiiSafe.Mask
    val longArray: LongArray,
    @get:PiiSafe.Mask
    val floatArray: FloatArray,
    @get:PiiSafe.Mask
    val doubleArray: DoubleArray,
)
