package code_metier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ExperimentDataStore {

	private List<DataPoint> dataPoints;
	private Map<Tag, Range<Float>> phases;
	
	ExperimentDataStore() {
		this.dataPoints = new ArrayList<DataPoint>();
		this.phases = new LinkedHashMap<Tag, Range<Float>>();
	}
	
	List<DataPoint> getDataPoints() {
		return this.dataPoints;
	}
	
	Map<Tag, Range<Float>> getPhases() {
		return this.phases;
	}

}
