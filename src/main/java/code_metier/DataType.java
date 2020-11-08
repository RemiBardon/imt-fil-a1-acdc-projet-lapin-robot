package code_metier;

/**
 * The computed data type
 * @author Rémi BARDON
 */
public enum DataType {

	/**
	 * RAW = TREND + SEASONNALITY + NOISE
	 */
	RAW,

	/**
	 * TREND = Moving Average of RAW
	 */
	TREND,

	/**
	 * SEASONNALITY = Seasonal Pattern in RAW
	 */
	SEASONNALITY,

	/**
	 * NOISE = RAW - TREND - SEASONNALITY
	 */
	NOISE

}
