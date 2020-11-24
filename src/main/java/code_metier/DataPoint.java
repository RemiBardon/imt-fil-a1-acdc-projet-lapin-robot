package code_metier;

/**
 * A data point (recorded value associated and timestamp)
 * 
 * @author Rémi BARDON
 */
public final class DataPoint {

	/**
	 * The time of recording the data
	 */
	private float timestamp;

	/**
	 * The recorded value
	 */
	private Float value;

	/**
	 * A simple constructor
	 * 
	 * @param timestamp The time of recording the data
	 * @param value     The recorded value
	 */
	DataPoint(float timestamp, Float value) {
		this.timestamp = timestamp;
		this.value = value;
	}

	/**
	 * The time of recording the data
	 * 
	 * @return The number of milliseconds from the start of the experiment
	 */
	public float getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets the time of recording the data
	 * 
	 * @param timestamp The new timestamp value
	 */
	public void setTimestamp(float timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * The recorded value
	 * 
	 * @return The recorded floating-point value
	 */
	public Float getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue() + " at " + this.getTimestamp();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof DataPoint) {
			DataPoint otherPoint = (DataPoint) obj;
			return otherPoint.getTimestamp() == this.getTimestamp() && otherPoint.getValue().equals(this.getValue());
		}

		return false;
	}

}
