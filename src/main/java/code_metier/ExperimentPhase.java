package code_metier;

/**
 * A phase of the experiment
 * @author Rémi BARDON
 */
final class ExperimentPhase {

	Tag tag;
	Float start;
	Float end;

	ExperimentPhase(Tag tag, Float start, Float end) {
		this.tag = tag;
		this.start = start;
		this.end = end;
	}

}
