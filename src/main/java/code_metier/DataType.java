package code_metier;

/**
 * The computed data type
 * 
 * @author Rémi BARDON
 */
public enum DataType {

	/**
	 * {@link DataType#RAW} = {@link DataType#TREND} + {@link DataType#SEASONNALITY} + {@link DataType#NOISE}
	 * @author Rémi BARDON
	 */
	RAW,

	/**
	 * {@link DataType#TREND} = Moving Average of {@link DataType#RAW}
	 * @author Rémi BARDON
	 */
	TREND,

	/**
	 * {@link DataType#SEASONNALITY} = Seasonal Pattern in {@link DataType#RAW}
	 * @author Rémi BARDON
	 */
	SEASONNALITY,

	/**
	 * {@link DataType#NOISE} = {@link DataType#RAW} - {@link DataType#TREND} - {@link DataType#SEASONNALITY}
	 * @author Rémi BARDON
	 */
	NOISE

}
