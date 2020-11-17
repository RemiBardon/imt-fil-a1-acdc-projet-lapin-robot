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

	public T getMinimum() {
		return this.minimum;
	}

	public T getMaximum() {
		return this.maximum;
	}

	public String toString() {
		return "(" + this.getMinimum() + "," + this.getMaximum() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof Range) {
			Range<?> otherRange = (Range<?>)obj;
			return otherRange.getMinimum().equals(this.getMinimum())
				&& otherRange.getMaximum().equals(this.getMaximum());
		}

		return false;
	}

	@Override
	public int hashCode() {
	    return minimum.hashCode() * maximum.hashCode();
	}

	public int compareTo(Range<T> o) {
		return this.getMinimum().compareTo(o.getMinimum());
	}

}
