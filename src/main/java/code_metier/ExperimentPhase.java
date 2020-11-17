package code_metier;

/**
 * A phase of the experiment
 * @author Rémi BARDON
 */
public final class ExperimentPhase implements Cloneable {

	private Tag tag;
	private float start;
	private float end;

	ExperimentPhase(Tag tag, float start, float end) {
		this.tag = tag;
		this.start = start;
		this.end = end;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

	public float getStart() {
		return start;
	}

	public void setStart(float start) {
		this.start = start;
	}

	public float getEnd() {
		return end;
	}

	public void setEnd(float end) {
		this.end = end;
	}

	public String toString() {
		return this.getTag() + " (" + this.getStart() + "," + this.getEnd() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof ExperimentPhase) {
			ExperimentPhase otherPhase = (ExperimentPhase)obj;
			return otherPhase.getTag().equals(this.getTag())
				&& otherPhase.getStart() == this.getStart()
				&& otherPhase.getEnd() == this.getEnd();
		}

		return false;
	}

	@Override
	public int hashCode() {
	    return tag.hashCode() * ((Float)start).hashCode() * ((Float)end).hashCode();
	}

	@Override
	public ExperimentPhase clone() throws CloneNotSupportedException {
		return (ExperimentPhase) super.clone();
	}

}
