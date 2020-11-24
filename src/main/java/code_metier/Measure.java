package code_metier;

/**
 * A tag present in the data file, representing a period during the experiment
 * 
 * @author Rémi BARDON
 */
public final class Measure {

	/**
	 * The name of the {@link Measure}
	 */
	private String name;

	/**
	 * A simple constructor
	 * 
	 * @param value The name of the {@link Measure}
	 */
	Measure(String name) {
		this.name = name;
	}

	/**
	 * A getter for {@code Measure.name}
	 * 
	 * @return The name of the {@link Measure}
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * A string representation of the {@link Measure}
	 * 
	 * @return The string value of the {@link Measure}
	 */
	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof Measure) {
			Measure otherMeasure = (Measure) obj;
			return otherMeasure.getName().equals(this.getName());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

}