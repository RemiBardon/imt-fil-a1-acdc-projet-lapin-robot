package code_metier;

/**
 * A tag present in the data file, representing a period during the experiment
 * @author Rémi BARDON
 */
public final class Tag {

	/**
	 * The string value of the `Tag`
	 */
	private String value;

	/**
	 * A simple constructor
	 * @param value The string value of the `Tag`
	 */
	Tag(String value) {
		this.value = value;
	}

	/**
	 * The preparation phase
	 * @return A pre-defined `Tag`
	 */
	public static Tag preparation() {
		return new Tag("preparation");
	}

	/**
	 * The euthanasia phase
	 * @return A pre-defined `Tag`
	 */
	public static Tag euthanasia() {
		return new Tag("euthanasia");
	}

	/**
	 * A string representation of the `Tag`
	 * @return The string value of the `Tag`
	 */
	public String toString() {
		return this.value;
	}

}
