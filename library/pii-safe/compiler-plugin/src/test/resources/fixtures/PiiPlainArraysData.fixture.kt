import net.thunderbird.piisafe.annotation.PiiSafe

@PiiSafe.HasPii
data class PiiPlainArraysData(
    @get:PiiSafe.Mask val label: String,
    val array: Array<Any>,
    val bytes: ByteArray,
    val shorts: ShortArray,
    val ints: IntArray,
    val longs: LongArray,
    val floats: FloatArray,
    val doubles: DoubleArray,
)
