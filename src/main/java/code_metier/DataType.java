package code_metier;

/**
 * The computed data type
 * 
 * @author Rémi BARDON
 */
public enum DataType {

	/**
	 * {@code RAW = TREND + SEASONNALITY + NOISE}
	 */
	RAW,

	/**
	 * {@code TREND = Moving Average of RAW}
	 */
	TREND,

	/**
	 * {@code SEASONNALITY = Seasonal Pattern in RAW}
	 */
	SEASONNALITY,

	/**
	 * {@code NOISE = RAW - TREND - SEASONNALITY}
	 */
	NOISE

}
