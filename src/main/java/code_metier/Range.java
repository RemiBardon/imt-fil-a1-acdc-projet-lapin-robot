package code_metier;

/**
 * 
 * @author Rémi BARDON
 * @param <T> A comparable data type
 */
public final class Range<T extends Comparable<T>> implements Comparable<Range<T>> {

	/**
	 * 
	 * @author Rémi BARDON
	 */
	private T minimum;

	/**
	 * 
	 * @author Rémi BARDON
	 */
	private T maximum;

	/**
	 * 
	 * @param minimum
	 * @param maximum
	 * @author Rémi BARDON
	 */
	public Range(T minimum, T maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
	}

	/**
	 * 
	 * @param value
	 * @return
	 * @author Rémi BARDON
	 */
	public boolean contains(final T value) {
		return this.getMinimum().compareTo(value) <= 0 && this.getMaximum().compareTo(value) >= 0;
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public T getMaximum() {
		return this.maximum;
	}

	/**
	 * 
	 * @param minimum
	 * @author Rémi BARDON
	 */
	public void setMinimum(final T minimum) {
		this.minimum = minimum;
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public T getMinimum() {
		return this.minimum;
	}

	/**
	 * 
	 * @param maximum
	 * @author Rémi BARDON
	 */
	public void setMaximum(final T maximum) {
		this.maximum = maximum;
	}

	@Override
	public String toString() {
		return "(" + this.getMinimum() + "..." + this.getMaximum() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof Range) {
			Range<?> otherRange = (Range<?>) obj;
			return otherRange.getMinimum().equals(this.getMinimum())
					&& otherRange.getMaximum().equals(this.getMaximum());
		}

		return false;
	}

	@Override
	public int compareTo(Range<T> o) {
		return this.getMinimum().compareTo(o.getMinimum());
	}

	@Override
	public int hashCode() {
		return minimum.hashCode() * maximum.hashCode();
	}

}
