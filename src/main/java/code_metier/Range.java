package code_metier;

/**
 * 
 * @author Rémi BARDON
 *
 * @param <T> A data type
 */
public final class Range<T extends Comparable<T>> implements Comparable<Range<T>> {
	
	private T minimum;
	
	private T maximum;
	
	public Range(T minimum, T maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
	}
	
	/**
	 * Gets the minimum value in this `Range`
	 * @return The minimum value in this `Range`, not null
	 */
	public T getMinimum() {
		return this.minimum;
	}
	
	/**
	 * Gets the maximum value in this `Range`
	 * @return The maximum value in this `Range`, not null
	 */
	public T getMaximum() {
		return this.maximum;
	}

	public int compareTo(Range<T> o) {
		return this.getMinimum().compareTo(o.getMinimum());
	}
	
}
