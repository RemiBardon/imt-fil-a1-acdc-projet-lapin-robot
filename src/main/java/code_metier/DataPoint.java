package code_metier;

/**
 * A data point (recorded value associated and timestamp)
 * @author Rémi BARDON
 */
public final class DataPoint {

	/**
	 * The time of recording the data
	 */
	private Float timestamp;
	
	/**
	 * The recorded value
	 */
	private Float value;
	
	/**
	 * A simple constructor
	 * @param timestamp The time of recording the data
	 * @param value The recorded value
	 */
    public DataPoint(Float timestamp, Float value) {
    	this.timestamp = timestamp;
    	this.value = value;
    }
    
    /**
     * The time of recording the data
     * @return The number of milliseconds from the start of the experiment
     */
    public Float getTimestamp() {
    	return this.timestamp;
    }
    
    /**
     * The recorded value
     * @return The recorded floating-point value
     */
    public Float getValue() {
    	return this.value;
    }

}
