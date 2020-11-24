package code_metier;

/**
 * A tag present in the data file, representing a period during the experiment
 * 
 * @author Rémi BARDON
 */
public final class Tag {

	/**
	 * The preparation phase
	 */
	public static final Tag PREPARATION = new Tag("preparation");

	/**
	 * The string value of the {@link Tag}
	 */
	private String value;

	/**
	 * A simple constructor
	 * 
	 * @param value The string value of the {@link Tag}
	 */
	Tag(String value) {
		this.value = value;
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

	/**
	 * A string representation of the {@link Tag}
	 * 
	 * @return The string value of the {@link Tag}
	 */
	@Override
	public String toString() {
		return this.value;
	}

}
