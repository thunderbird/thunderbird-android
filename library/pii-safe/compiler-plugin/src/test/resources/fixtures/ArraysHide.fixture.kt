import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class ArraysHide(
    @get:PiiSafe.Hide
    val list: List<String>,
    @get:PiiSafe.Hide
    val map: Map<String, String>,
    @get:PiiSafe.Hide
    val array: Array<Any>,
    @get:PiiSafe.Hide
    val byteArray: ByteArray,
    @get:PiiSafe.Hide
    val shortArray: ShortArray,
    @get:PiiSafe.Hide
    val intArray: IntArray,
    @get:PiiSafe.Hide
    val longArray: LongArray,
    @get:PiiSafe.Hide
    val floatArray: FloatArray,
    @get:PiiSafe.Hide
    val doubleArray: DoubleArray,
)
