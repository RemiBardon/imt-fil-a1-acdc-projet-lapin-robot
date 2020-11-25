package code_metier;

/**
 * A tag present in the data file, representing a period during the experiment
 * @author Rémi BARDON
 */
public final class Tag {

	/**
	 * The preparation phase
	 * @author Rémi BARDON
	 */
	public static final Tag PREPARATION = new Tag("preparation");

	/**
	 * The string value of the {@link Tag}
	 * @author Rémi BARDON
	 */
	private String value;

	/**
	 * A simple constructor
	 * @param value The string value of the {@link Tag}
	 * @author Rémi BARDON
	 */
	Tag(String value) {
		this.value = value;
	}

	/**
	 * A string representation of the {@link Tag}
	 * @return The string value of the {@link Tag}
	 * @author Rémi BARDON
	 */
	@Override
	public String toString() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof Tag) {
			Tag otherTag = (Tag) obj;
			return otherTag.toString().equals(this.toString());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
