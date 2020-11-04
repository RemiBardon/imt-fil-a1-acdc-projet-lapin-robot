package scanner;

import java.util.Date;

/**
 * A data point (recorded value associated and timestamp)
 * @author Rémi BARDON
 */
public final class DataPoint {

	/**
	 * The time of recording the data
	 */
	public Date timestamp;
	
	/**
	 * The recorded value
	 */
	public float value;
	
	/**
	 * A simple constructor
	 * @param timestamp The time of recording the data
	 * @param value The recorded value
	 */
    DataPoint(Date timestamp, float value) {
    	this.timestamp = timestamp;
    	this.value = value;
    }

}
