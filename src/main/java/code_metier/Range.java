package code_metier;

/**
 * 
 * @author Rémi BARDON
 * @param <T> A comparable data type
 */
public final class Range<T extends Comparable<T>> implements Comparable<Range<T>> {

	private T minimum;

	private T maximum;

	public Range(T minimum, T maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
	}

	@Override
	public int compareTo(Range<T> o) {
		return this.getMinimum().compareTo(o.getMinimum());
	}

	public boolean contains(final T value) {
		return this.getMinimum().compareTo(value) <= 0 && this.getMaximum().compareTo(value) >= 0;
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

	public T getMaximum() {
		return this.maximum;
	}

	public T getMinimum() {
		return this.minimum;
	}

	@Override
	public int hashCode() {
		return minimum.hashCode() * maximum.hashCode();
	}

	public void setMaximum(final T maximum) {
		this.maximum = maximum;
	}

	public void setMinimum(final T minimum) {
		this.minimum = minimum;
	}

	@Override
	public String toString() {
		return "(" + this.getMinimum() + "..." + this.getMaximum() + ")";
	}

}
