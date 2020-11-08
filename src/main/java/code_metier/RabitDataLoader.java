package code_metier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.servicenow.ds.stats.stl.SeasonalTrendLoess;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

/**
 * 
 * @author Rémi BARDON
 */
public final class RabitDataLoader {

	private final static char DELIMITER = '\t';
	private String headingComment;
	private final ArrayList<ExperimentPhase> phases;
	private final HashMap<Tag, ExperimentPhase> phasesMap;
	private HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> points;
	private final HashMap<Range<Float>, HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>> omittedPoints;

	public RabitDataLoader() {
		this.headingComment = "";
		this.phases = new ArrayList<ExperimentPhase>();
		this.phasesMap = new HashMap<Tag, ExperimentPhase>();
		this.points = emptyPoints();
		this.omittedPoints = new HashMap<Range<Float>, HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>>();
	}

	public final void reset() {
		this.headingComment = "";
		this.phases.clear();
		this.phasesMap.clear();
		this.points = emptyPoints();
		this.omittedPoints.clear();
	}

	private static final HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> emptyPoints() {
		HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> empty = new HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>();

		// Add empty `HashMap`s for every `DataType`
		for (DataType type: DataType.values()) {
			empty.put(type, emptyMesures());
		}

		return empty;
	}

	private static final HashMap<Mesure, ArrayList<DataPoint>> emptyMesures() {
		HashMap<Mesure, ArrayList<DataPoint>> empty = new HashMap<Mesure, ArrayList<DataPoint>>();

		// Add empty `ArrayList`s for every `Mesure`
		for (Mesure mesure: Mesure.values()) {
			empty.put(mesure, new ArrayList<DataPoint>());
		}

		return empty;
	}

	public final void load(File file) throws IOException, CsvValidationException, NumberFormatException {
		this.reset();

		CSVReader csvReader = null;

		try {
			// Create CSV Reader
			FileReader fileReader = new FileReader(file);
			CSVParser parser = new CSVParserBuilder().withSeparator(DELIMITER).build();
			csvReader = new CSVReaderBuilder(fileReader).withCSVParser(parser).build();

			// Get reference to raw points
			HashMap<Mesure, ArrayList<DataPoint>> raw = this.points.get(DataType.RAW);

			// Initialize phases with preparation phase
			Tag preparationTag = Tag.preparation();
			ExperimentPhase actualPhase = new ExperimentPhase(preparationTag, 0.0f, 0.0f);
			this.phases.add(actualPhase);
			this.phasesMap.put(preparationTag, actualPhase);

			// Read CSV and parse data
			String[] split = null;
			Float lastTimestamp = 0.0f;
			boolean isHeader = true;
			while((split = csvReader.readNext()) != null) {
				// `isHeader` is `true` until we meet a line starting with `"0\t"` for the first time
				isHeader = isHeader && split[0] != "0\t";

				if (isHeader) {
					// If we're still in file header

					// Read comment
					headingComment += String.join(" ", split);
				} else {
					// If we're not in header anymore

					// Parse timestamp
					Float timestamp = Float.parseFloat(split[0]);

					// Parse measures data and store it in raw points
					raw.get(Mesure.PRESSION_ARTERIELLE)		.add(new DataPoint(timestamp, Float.parseFloat(split[1])));
					raw.get(Mesure.SPIROMETRIE)				.add(new DataPoint(timestamp, Float.parseFloat(split[2])));
					raw.get(Mesure.PA_MOYENNE)				.add(new DataPoint(timestamp, Float.parseFloat(split[3])));
					raw.get(Mesure.FREQUENCE_CARDIAQUE)		.add(new DataPoint(timestamp, Float.parseFloat(split[4])));
					raw.get(Mesure.FREQUENCE_RESPIRATOIRE)	.add(new DataPoint(timestamp, Float.parseFloat(split[5])));

					if (!split[6].isEmpty()) {
						// End last phase at last timestamp
						actualPhase.end = lastTimestamp;

						// Get tag value, and remove `"#* "` prefix
						Tag newTag = new Tag(split[6].replaceFirst("^#* ", ""));

						// Create a new phase starting and ending at actual timestamp
						ExperimentPhase newPhase = new ExperimentPhase(newTag, timestamp, timestamp);
						this.phases.add(newPhase);
						this.phasesMap.put(newTag, newPhase);

						// Update actual phase
						actualPhase = newPhase;
					}

					// Update last timestamp for next read
					lastTimestamp = timestamp;
				}
			}

			// Decompose time series: extract seasonnality, trend and noise
			for (Map.Entry<Mesure, ArrayList<DataPoint>> entry: raw.entrySet()) {
				double[] values = dataPointsValues(entry.getValue());

				SeasonalTrendLoess.Builder builder = new SeasonalTrendLoess.Builder();
				SeasonalTrendLoess smoother = builder
					.setPeriodLength(1000)	// Period of 1s
					.setPeriodic() 			// <=> .setSeasonalWidth(1)
					.setRobust() 			// Expecting outliers
					.buildSmoother(values);

				SeasonalTrendLoess.Decomposition stl = smoother.decompose();

				// Get stl decomposed values
				double[] stlSeasonal 	= stl.getSeasonal();
				double[] stlTrend 		= stl.getTrend();
				double[] stlResidual 	= stl.getResidual();

				// Get references to points
				ArrayList<DataPoint> seasonnality 	= this.points.get(DataType.SEASONNALITY).get(entry.getKey());
				ArrayList<DataPoint> trend 			= this.points.get(DataType.TREND)		.get(entry.getKey());
				ArrayList<DataPoint> noise 			= this.points.get(DataType.NOISE)		.get(entry.getKey());

				int valueCount = entry.getValue().size();
				Iterator<DataPoint> iterator = entry.getValue().iterator();

				for (int i = 0; i < valueCount; i++) {
					float timestamp = iterator.next().getTimestamp();
					seasonnality.add(new DataPoint(timestamp, (float) stlSeasonal[i]));
					trend		.add(new DataPoint(timestamp, (float) stlTrend[i]));
					noise		.add(new DataPoint(timestamp, (float) stlResidual[i]));
				}
			}
		} finally {
			csvReader.close();
		}
	}

	private static double[] convertDoublesToPrimitiveDoubles(List<Double> doubles) {
		double[] result = new double[doubles.size()];
		Iterator<Double> iterator = doubles.iterator();

		for (int i = 0; i < result.length; i++) {
			result[i] = iterator.next().doubleValue();
		}

		return result;
	}

	private static List<Double> convertDataPointsToDoubles(List<DataPoint> points) {
		ArrayList<Double> result = new ArrayList<Double>();

		for (DataPoint point: points) {
			result.add(point.getValue().doubleValue());
		}

		return result;
	}

	private static double[] dataPointsValues(List<DataPoint> points) {
		return convertDoublesToPrimitiveDoubles(convertDataPointsToDoubles(points));
	}

	public final ArrayList<Tag> getTags() {
		ArrayList<Tag> tags = new ArrayList<Tag>();

		for (ExperimentPhase phase: this.phases) {
			tags.add(phase.tag);
		}

		return tags;
	}

	public final Optional<DataPoint> getPoint(DataType type, Mesure mesure, Float timestamp) {
		ArrayList<DataPoint> filtered = this.points.get(type).get(mesure);

		for (DataPoint point: filtered) {
			if (point.getTimestamp() == timestamp) {
				return Optional.of(point);
			}
		}

		return Optional.empty();
	}

	public final HashMap<Mesure, Optional<DataPoint>> getAllPoints(DataType type, ArrayList<Mesure> mesures, Float timestamp) {
		HashMap<Mesure, Optional<DataPoint>> result = new HashMap<Mesure, Optional<DataPoint>>();

		for (Mesure mesure: mesures) {
			result.put(mesure, this.getPoint(type, mesure, timestamp));
		}

		return result;
	}

	public final ArrayList<DataPoint> getPoints(DataType type, Mesure mesure, Optional<Tag> tag) {
		ArrayList<DataPoint> filtered = this.points.get(type).get(mesure);

		if (tag.isEmpty()) {
			return filtered;
		}

		ExperimentPhase phase = this.phasesMap.get(tag.get());

		ArrayList<DataPoint> result = new ArrayList<DataPoint>();

		for (DataPoint point: filtered) {
			Float timestamp = point.getTimestamp();
			if (timestamp >= phase.start && timestamp <= phase.end) {
				result.add(point);
			}
		}

		return result;
	}

	public final HashMap<Mesure, ArrayList<DataPoint>> getAllPoints(DataType type, ArrayList<Mesure> mesures, Optional<Tag> tag) {
		HashMap<Mesure, ArrayList<DataPoint>> result = new HashMap<Mesure, ArrayList<DataPoint>>();

		for (Mesure mesure: mesures) {
			result.put(mesure, this.getPoints(type, mesure, tag));
		}

		return result;
	}

	public final HashMap<DataType, Optional<DataPoint>> getPoints(Mesure mesure, Float timestamp) {
		HashMap<DataType, Optional<DataPoint>> result = new HashMap<DataType, Optional<DataPoint>>();

		for (Map.Entry<DataType, HashMap<Mesure, ArrayList<DataPoint>>> entry: this.points.entrySet()) {
			DataType type = entry.getKey();
			result.put(type, this.getPoint(type, mesure, timestamp));
		}

		return result;
	}

	public final HashMap<Mesure, HashMap<DataType, Optional<DataPoint>>> getAllPoints(ArrayList<Mesure> mesures, Float timestamp) {
		HashMap<Mesure, HashMap<DataType, Optional<DataPoint>>> result = new HashMap<Mesure, HashMap<DataType, Optional<DataPoint>>>();

		for (Mesure mesure: mesures) {
			result.put(mesure, this.getPoints(mesure, timestamp));
		}

		return result;
	}

	public final HashMap<DataType, ArrayList<DataPoint>> getPoints(Mesure mesure, Optional<Tag> tag) {
		HashMap<DataType, ArrayList<DataPoint>> result = new HashMap<DataType, ArrayList<DataPoint>>();

		for (Map.Entry<DataType, HashMap<Mesure, ArrayList<DataPoint>>> entry: this.points.entrySet()) {
			DataType type = entry.getKey();
			result.put(type, this.getPoints(type, mesure, tag));
		}

		return result;
	}

	public final HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> getAllPoints(ArrayList<Mesure> mesures, Optional<Tag> tag) {
		HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> result = emptyPoints();

		for (Mesure mesure: mesures) {
			for (Map.Entry<DataType,ArrayList<DataPoint>> entry: this.getPoints(mesure, tag).entrySet()) {
				result.get(entry.getKey()).put(mesure, entry.getValue());
			}
		}

		return result;
	}

	public final Range<Float> getValueRange(ArrayList<DataPoint> points) {
		if (points.isEmpty()) {
			return new Range<Float>(0.0f, 1.0f);
		}

		Float minimum = Float.POSITIVE_INFINITY;
		Float maximum = Float.NEGATIVE_INFINITY;

		for (DataPoint point: points) {
			Float value = point.getValue();
			if (value < minimum) {
				minimum = value;
			} else if (value > maximum) {
				maximum = value;
			}
		}

		return new Range<Float>(minimum, maximum);
	}

	public final <T> HashMap<T, Range<Float>> getValueRangeFromMap(HashMap<T, ArrayList<DataPoint>> points) {
		HashMap<T, Range<Float>> result = new HashMap<T, Range<Float>>();

		for (Map.Entry<T,ArrayList<DataPoint>> entry: points.entrySet()) {
			result.put(entry.getKey(), this.getValueRange(entry.getValue()));
		}

		return result;
	}

	public final <S, T> HashMap<S, HashMap<T, Range<Float>>> getValueRangeFromNestedMap(HashMap<S, HashMap<T, ArrayList<DataPoint>>> points) {
		HashMap<S, HashMap<T, Range<Float>>> result = new HashMap<S, HashMap<T, Range<Float>>>();

		for (Map.Entry<S, HashMap<T, ArrayList<DataPoint>>> entry: points.entrySet()) {
			result.put(entry.getKey(), this.getValueRangeFromMap(entry.getValue()));
		}

		return result;
	}

	public final ArrayList<Range<Float>> getOmittedRanges() {
		ArrayList<Range<Float>> result = new ArrayList<Range<Float>>(this.omittedPoints.keySet());

		Collections.sort(result);

		return result;
	}

	public final HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> getOmittedPoints(Range<Float> range) {
		return this.omittedPoints.get(range);
	}

	public final String getHeadingComment() {
		return this.headingComment;
	}

}
