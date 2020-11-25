package code_metier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 
 * @author Rémi BARDON
 */
public class ExperimentDataStore {

	/**
	 * 
	 * @author Rémi BARDON
	 */
	private List<DataPoint> dataPoints;
	/**
	 * 
	 * @author Rémi BARDON
	 */
	private Map<Tag, Range<Float>> phases;

	/**
	 * 
	 * @author Rémi BARDON
	 */
	public ExperimentDataStore() {
		this.dataPoints = new ArrayList<DataPoint>();
		this.phases = new LinkedHashMap<Tag, Range<Float>>();
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public List<DataPoint> getDataPoints() {
		return this.dataPoints;
	}

	/**
	 * 
	 * @param optionalTag An {@link Optional} {@link Tag} to filter results
	 * @return <ul>
	 *     <li>An empty {@link List} if the given {@link Tag} doesn't exist</li>
	 *     <li>Otherwise, the {@link DataPoint}s corresponding to given {@link Tag}</li>
	 * </ul>
	 * @author Rémi BARDON
	 */
	public List<DataPoint> getDataPoints(final Optional<Tag> optionalTag) {
		if (optionalTag.isEmpty()) {
			return this.getDataPoints();
		}
		final Tag tag = optionalTag.get();

		final Map<Tag, Range<Float>> phases = this.getPhases();
		if (!phases.containsKey(tag)) {
			return new ArrayList<DataPoint>();
		}

		final Range<Float> range = phases.get(tag);
		final List<DataPoint> points = this.getDataPoints();
		final List<DataPoint> result = new ArrayList<DataPoint>();

		for (DataPoint point : points) {
			if (range.contains(point.getTimestamp())) {
				result.add(point);
			}
		}

		return result;
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public List<Tag> getTags() {
		return new ArrayList<Tag>(this.phases.keySet());
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public Map<Tag, Range<Float>> getPhases() {
		return this.phases;
	}

}
