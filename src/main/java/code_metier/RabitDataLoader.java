package code_metier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
	private final static int PERIOD = 200; // Period of 1s
	private final static Pattern TAG_PREFIX = Pattern.compile("^#\\* ");
	private final static NumberFormat FORMAT = NumberFormat.getInstance(Locale.FRANCE);

	private String headingComment;
	private final Map<Mesure, List<ExperimentPhase>> phases;
	private final Map<Mesure, Map<Tag, ExperimentPhase>> phasesByTag;
	private final Map<Mesure, Map<Float, ExperimentPhase>> phasesByStart;
	private final Map<Mesure, Map<Float, ExperimentPhase>> phasesByEnd;
	private final Map<DataType, Map<Mesure, List<DataPoint>>> points;
	private final Map<Mesure, Map<Range<Float>, List<DataPoint>>> omittedPoints;

	public RabitDataLoader() {
		this.headingComment = "";
		this.phases 		= new HashMap<Mesure, List<ExperimentPhase>>();
		this.phasesByTag 	= new HashMap<Mesure, Map<Tag, ExperimentPhase>>();
		this.phasesByStart 	= new HashMap<Mesure, Map<Float, ExperimentPhase>>();
		this.phasesByEnd 	= new HashMap<Mesure, Map<Float, ExperimentPhase>>();
		this.points 		= new HashMap<DataType, Map<Mesure, List<DataPoint>>>();
		this.omittedPoints 	= new HashMap<Mesure, Map<Range<Float>, List<DataPoint>>>();
	}

	public final void reset() {
		this.headingComment = "";
		this.phases.clear();
		addMissingMeasures(this.phases, () -> new ArrayList<ExperimentPhase>());
		this.phasesByTag.clear();
		addMissingMeasures(this.phasesByTag, () -> new HashMap<Tag, ExperimentPhase>());
		this.phasesByStart.clear();
		addMissingMeasures(this.phasesByStart, () -> new HashMap<Float, ExperimentPhase>());
		this.phasesByEnd.clear();
		addMissingMeasures(this.phasesByEnd, () -> new HashMap<Float, ExperimentPhase>());
		this.points.clear();
		addMissingDataTypes(this.points, () -> new HashMap<Mesure, List<DataPoint>>());
		for (final DataType type: DataType.values()) {
			addMissingMeasures(this.points.get(type), () -> new ArrayList<DataPoint>());
		}
		this.omittedPoints.clear();
		addMissingMeasures(this.omittedPoints, () -> new HashMap<Range<Float>, List<DataPoint>>());
	}

	private static final <T> void addMissingDataTypes(final Map<DataType, T> map, final Supplier<T> supplier) {
		for (final DataType type: DataType.values()) {
			map.putIfAbsent(type, supplier.get());
		}
	}

	private static final <T> void addMissingMeasures(final Map<Mesure, T> map, final Supplier<T> supplier) {
		for (final Mesure mesure: Mesure.values()) {
			map.putIfAbsent(mesure, supplier.get());
		}
	}

	public final void load(final File file) throws IOException, CsvValidationException, ParseException {
		this.reset();

		CSVReader csvReader = null;

		try {
			// Create CSV Reader
			final FileReader fileReader = new FileReader(file, Charset.forName("UTF8"));
			final CSVParser parser = new CSVParserBuilder().withSeparator(DELIMITER).build();
			csvReader = new CSVReaderBuilder(fileReader).withCSVParser(parser).build();

			// Get reference to raw points
			final Map<Mesure, List<DataPoint>> raw = this.points.get(DataType.RAW);
			addMissingMeasures(raw, () -> new ArrayList<DataPoint>());

			// Initialize phases with preparation phase
			final Tag preparationTag = Tag.PREPARATION;
			final Map<Mesure, ExperimentPhase> actualPhase = new HashMap<Mesure, ExperimentPhase>();
			for (final Mesure mesure: Mesure.values()) {
				final ExperimentPhase localPhase = new ExperimentPhase(preparationTag, 0.0f, 0.0f);
				actualPhase.put(mesure, localPhase);
				this.phases.get(mesure)			.add(localPhase);
				this.phasesByTag.get(mesure)	.put(preparationTag, localPhase);
				this.phasesByStart.get(mesure)	.put(localPhase.getStart(), localPhase);
				this.phasesByEnd.get(mesure)	.put(localPhase.getEnd(), localPhase);
			}

			// Read CSV and parse data
			String[] split = null;
			float lastTimestamp = 0.0f;
			boolean isHeader = true;
			int validLinesRead = 0;
			while ((split = csvReader.readNext()) != null) {
				// `isHeader` is `true` until we meet a line starting with `"0\t"` for the first time
				isHeader = isHeader && !split[0].equals("0");

				if (isHeader) {
					// If we're still in file header

					// Read comment
					this.headingComment += String.join("\t", split);
				} else {
					// If we're not in header anymore

					// Skip line if empty
					if (split[0].isEmpty()) { continue; }

					// Parse timestamp
					final float timestamp = FORMAT.parse(split[0]).floatValue();

					// Parse measures data and store it in raw points
					raw.get(Mesure.PRESSION_ARTERIELLE)		.add(new DataPoint(timestamp, FORMAT.parse(split[1]).floatValue()));
					raw.get(Mesure.SPIROMETRIE)				.add(new DataPoint(timestamp, FORMAT.parse(split[2]).floatValue()));
					raw.get(Mesure.PA_MOYENNE)				.add(new DataPoint(timestamp, FORMAT.parse(split[3]).floatValue()));
					raw.get(Mesure.FREQUENCE_CARDIAQUE)		.add(new DataPoint(timestamp, FORMAT.parse(split[4]).floatValue()));
					raw.get(Mesure.FREQUENCE_RESPIRATOIRE)	.add(new DataPoint(timestamp, FORMAT.parse(split[5]).floatValue()));

					if (!split[6].isEmpty()) {
						// End phase at last timestamp
						for (final Mesure mesure: Mesure.values()) {
							final ExperimentPhase localPhase = actualPhase.get(mesure);
							this.phasesByEnd.get(mesure).remove(localPhase.getEnd());
							localPhase.setEnd(lastTimestamp);
							this.phasesByEnd.get(mesure).put(localPhase.getEnd(), localPhase);
						}

						// Get tag value, and remove `"#* "` prefix
						final Tag newTag = new Tag(TAG_PREFIX.matcher(split[6]).replaceFirst(""));

						for (final Mesure mesure: Mesure.values()) {
							// If first valid line has a tag, remove default preparation tag
							if (validLinesRead == 0) {
								this.phases.get(mesure)			.clear();
								this.phasesByTag.get(mesure)	.clear();
								this.phasesByStart.get(mesure)	.clear();
								this.phasesByEnd.get(mesure)	.clear();
							}

							// Create a new phase starting and ending at actual timestamp
							final ExperimentPhase newPhase = new ExperimentPhase(newTag, timestamp, timestamp);

							// Store new phase
							this.phases.get(mesure)			.add(newPhase);
							this.phasesByTag.get(mesure)	.put(newTag, newPhase);
							this.phasesByStart.get(mesure)	.put(newPhase.getStart(), newPhase);
							this.phasesByEnd.get(mesure)	.put(newPhase.getEnd(), newPhase);

							// Update actual phase
							actualPhase.put(mesure, newPhase);
						}
					}

					// Increment counter
					validLinesRead++;

					// Update last timestamp for next read
					lastTimestamp = timestamp;
				}
			}

			for (final Mesure mesure: Mesure.values()) {
				// End last phase at last timestamp
				final ExperimentPhase localPhase = actualPhase.get(mesure);
				this.phasesByEnd.get(mesure).remove(localPhase.getEnd());
				localPhase.setEnd(lastTimestamp);
				this.phasesByEnd.get(mesure).put(localPhase.getEnd(), localPhase);

				// Remove preparation phase if empty
				if (this.phasesByTag.get(mesure).containsKey(Tag.PREPARATION) 
				 && this.getPoints(DataType.RAW, Mesure.PRESSION_ARTERIELLE, Optional.of(Tag.PREPARATION)).isEmpty()
				) {
					final ExperimentPhase preparationPhase = this.phasesByTag.get(mesure).get(Tag.PREPARATION);
					this.phases.get(mesure)			.remove(preparationPhase);
					this.phasesByTag.get(mesure)	.remove(Tag.PREPARATION);
					this.phasesByStart.get(mesure)	.remove(preparationPhase.getStart());
					this.phasesByEnd.get(mesure)	.remove(preparationPhase.getEnd());
				}
			}
		} finally {
			if (csvReader != null) {
				csvReader.close();
			}
		}
	}

	/**
	 * Remove data with NaN values, and offset timestamps to avoid holes
	 */
	@SuppressWarnings("unchecked")
	public void cleanData() {
		// Get reference to raw points
		final Map<Mesure, List<DataPoint>> raw = this.points.get(DataType.RAW);

		for (final Map.Entry<Mesure, List<DataPoint>> entry: raw.entrySet()) {
			final Mesure mesure = entry.getKey();
			final List<ExperimentPhase> phases 				= this.phases.get(mesure);
			final Map<Tag, ExperimentPhase> phasesByTag 	= this.phasesByTag.get(mesure);
			final Map<Float, ExperimentPhase> phasesByStart = this.phasesByStart.get(mesure);
			final Map<Float, ExperimentPhase> phasesByEnd 	= this.phasesByEnd.get(mesure);
			final Map<Range<Float>, List<DataPoint>> omittedRanges = this.omittedPoints.get(mesure);

			Optional<Float> start = Optional.empty();
			float overallOffset = 0.0f;
			float lastTimestamp = 0.0f;
			final ArrayList<DataPoint> omitted = new ArrayList<DataPoint>();

			// Store NaN values
			Iterator<DataPoint> iterator = entry.getValue().iterator();
			while (iterator.hasNext()) {
				final DataPoint actual = iterator.next();
				final float actualTimestamp = actual.getTimestamp();

				if (actual.getValue().isNaN()) {
					// If in a NaN series
					if (start.isEmpty()) {
						// If new NaN series
						start = Optional.of(actualTimestamp);
					}

					// Store values to omit
					omitted.add(actual);
				}

				if (!actual.getValue().isNaN() || !iterator.hasNext()) {
					// If outside of NaN series or last line

					// Adjust offset
					if (start.isPresent()) {
						// If end of NaN series

						// Store omitted values
						final Range<Float> range = new Range<Float>(start.get(), iterator.hasNext() ? lastTimestamp : actualTimestamp);
						omittedRanges.put(range, (List<DataPoint>)omitted.clone());

						// Increment overall offset
						final float delta = actualTimestamp - start.get();
						overallOffset -= delta;

						// DEBUG
						//System.out.println(mesure + ": offset of " + overallOffset + " at " + actualTimestamp);

						// Clear data for next series
						start = Optional.empty();
						omitted.clear();
					}

					if (!iterator.hasNext()) {
						final float lastDelta = actualTimestamp - lastTimestamp;
						overallOffset -= lastDelta;

						// DEBUG
						//System.out.println(mesure + ": last offset of " + overallOffset + " at " + actualTimestamp);
					}

					// Offset timestamps
					actual.setTimestamp(actualTimestamp + overallOffset);
				}

				// Offset phases start
				if (phasesByStart.containsKey(actualTimestamp)) {
					// If on the start of a new phase

					// If some phase is empty, remove it
					if (overallOffset != 0 && phasesByStart.containsKey(actualTimestamp + overallOffset)) {
						final ExperimentPhase emptyPhase = phasesByStart.get(actualTimestamp + overallOffset);
						phases			.remove(emptyPhase);
						phasesByTag		.remove(emptyPhase.getTag());
						phasesByStart	.remove(emptyPhase.getStart());
						phasesByEnd		.remove(emptyPhase.getEnd());

						// DEBUG
						//System.out.println(mesure + ": removed phase " + emptyPhase + " at " + actualTimestamp + ". Conflicted with " + phasesByStart.get(actualTimestamp));
					}

					// Update start
					final ExperimentPhase phase = phasesByStart.get(actualTimestamp);
					// DEBUG
					//System.out.println(mesure + ": start of phase " + phase + " at " + actualTimestamp + ". Setting start to " + (actualTimestamp + overallOffset));
					phasesByStart.remove(actualTimestamp);
					phase.setStart(actualTimestamp + overallOffset);
					phasesByStart.put(phase.getStart(), phase);
				}

				// Offset phases end
				if (phasesByEnd.containsKey(actualTimestamp)) {
					final ExperimentPhase phase = phasesByEnd.get(actualTimestamp);
					// DEBUG
					//System.out.println(mesure + ": end of phase " + phase + " at " + actualTimestamp + ". Setting end to " + (actualTimestamp + overallOffset));
					phasesByEnd.remove(phase.getEnd());
					phase.setEnd(actualTimestamp + overallOffset);
					phasesByEnd.put(phase.getEnd(), phase);
				}

				// Update last timestamp for next read
				lastTimestamp = actualTimestamp;
			}

			// Remove NaN values
			entry.getValue().removeIf((point) -> point.getValue().isNaN());
		}
	}

	/**
	 * Decompose time series: extract seasonnality, trend and noise
	 */
	public void decomposeData() {
		// Get reference to raw points
		final Map<Mesure, List<DataPoint>> raw = this.points.get(DataType.RAW);

		for (final Map.Entry<Mesure, List<DataPoint>> entry: raw.entrySet()) {
			// Data series must be at least 2 * periodicity in length
			// https://github.com/ServiceNow/stl-decomp-4j/blob/62937cb089e13d8194f2b13fe28b86ce43315ee8/stl-decomp-4j/src/main/java/com/github/servicenow/ds/stats/stl/SeasonalTrendLoess.java#L351
			if (entry.getValue().size() < 2*PERIOD) { continue; }

			final double[] values = dataPointsValues(entry.getValue());

			final SeasonalTrendLoess.Builder builder = new SeasonalTrendLoess.Builder();
			final SeasonalTrendLoess smoother = builder
				.setPeriodLength(PERIOD)
				.setPeriodic() 	// <=> .setSeasonalWidth(1)
				.setRobust() 	// Expecting outliers
				.buildSmoother(values);

			final SeasonalTrendLoess.Decomposition stl = smoother.decompose();

			// Get stl decomposed values
			final double[] stlSeasonal 	= stl.getSeasonal();
			final double[] stlTrend 		= stl.getTrend();
			final double[] stlResidual 	= stl.getResidual();

			// Get references to points
			final Mesure mesure = entry.getKey();
			final List<DataPoint> seasonnality 	= this.points.get(DataType.SEASONNALITY).get(mesure);
			final List<DataPoint> trend 			= this.points.get(DataType.TREND)		.get(mesure);
			final List<DataPoint> noise 			= this.points.get(DataType.NOISE)		.get(mesure);

			final int valueCount = entry.getValue().size();
			final Iterator<DataPoint> iterator = entry.getValue().iterator();

			for (int i = 0; i < valueCount; i++) {
				final float timestamp = iterator.next().getTimestamp();
				seasonnality.add(new DataPoint(timestamp, (float) stlSeasonal[i]));
				trend		.add(new DataPoint(timestamp, (float) stlTrend[i]));
				noise		.add(new DataPoint(timestamp, (float) stlResidual[i]));
			}
		}
	}

	private static double[] dataPointsValues(final List<DataPoint> points) {
		final double[] result = new double[points.size()];
		final Iterator<DataPoint> iterator = points.iterator();

		for (int i = 0; i < result.length; i++) {
			result[i] = (double)iterator.next().getValue();
		}

		return result;
	}

	public final List<Tag> getTags(final Mesure mesure) {
		final List<Tag> tags = new ArrayList<Tag>();

		for (final ExperimentPhase phase: this.phases.get(mesure)) {
			tags.add(phase.getTag());
		}

		return tags;
	}

	public final List<ExperimentPhase> getPhases(final Mesure mesure) {
		return this.phases.get(mesure);
	}

	public final Optional<DataPoint> getPoint(final DataType type, final Mesure mesure, final float timestamp) {
		final List<DataPoint> filtered = this.points.get(type).get(mesure);

		for (final DataPoint point: filtered) {
			if (point.getTimestamp() == timestamp) {
				return Optional.of(point);
			}
		}

		return Optional.empty();
	}

	public final Map<Mesure, Optional<DataPoint>> getAllPoints(final DataType type, final List<Mesure> mesures, final float timestamp) {
		final Map<Mesure, Optional<DataPoint>> result = new HashMap<Mesure, Optional<DataPoint>>();

		for (final Mesure mesure: mesures) {
			result.put(mesure, this.getPoint(type, mesure, timestamp));
		}

		return result;
	}

	public final List<DataPoint> getPoints(final DataType type, final Mesure mesure, final Optional<Tag> tag) {
		final List<DataPoint> filtered = this.points.get(type).get(mesure);
		final Map<Tag, ExperimentPhase> phasesByTag = this.phasesByTag.get(mesure);

		// Return all data if no tag was given
		if (tag.isEmpty()) {
			return filtered;
		}

		// Return empty array if tag doesn't exist
		if (!phasesByTag.containsKey(tag.get())) {
			return new ArrayList<DataPoint>();
		}

		final ExperimentPhase phase = phasesByTag.get(tag.get());

		final ArrayList<DataPoint> result = new ArrayList<DataPoint>();

		for (final DataPoint point: filtered) {
			final Float timestamp = point.getTimestamp();
			if (timestamp >= phase.getStart() && timestamp <= phase.getEnd()) {
				result.add(point);
			}
		}

		return result;
	}

	public final Map<Mesure, List<DataPoint>> getAllPoints(final DataType type, final List<Mesure> mesures, final Optional<Tag> tag) {
		final Map<Mesure, List<DataPoint>> result = new HashMap<Mesure, List<DataPoint>>();

		for (final Mesure mesure: mesures) {
			result.put(mesure, this.getPoints(type, mesure, tag));
		}

		return result;
	}

	public final Map<DataType, Optional<DataPoint>> getPoints(Mesure mesure, Float timestamp) {
		final Map<DataType, Optional<DataPoint>> result = new HashMap<DataType, Optional<DataPoint>>();

		for (final Map.Entry<DataType, Map<Mesure, List<DataPoint>>> entry: this.points.entrySet()) {
			final DataType type = entry.getKey();
			result.put(type, this.getPoint(type, mesure, timestamp));
		}

		return result;
	}

	public final Map<Mesure, Map<DataType, Optional<DataPoint>>> getAllPoints(final List<Mesure> mesures, final float timestamp) {
		final Map<Mesure, Map<DataType, Optional<DataPoint>>> result = new HashMap<Mesure, Map<DataType, Optional<DataPoint>>>();

		for (final Mesure mesure: mesures) {
			result.put(mesure, this.getPoints(mesure, timestamp));
		}

		return result;
	}

	public final Map<DataType, List<DataPoint>> getPoints(final Mesure mesure, final Optional<Tag> tag) {
		final Map<DataType, List<DataPoint>> result = new HashMap<DataType, List<DataPoint>>();

		for (final Map.Entry<DataType, Map<Mesure, List<DataPoint>>> entry: this.points.entrySet()) {
			final DataType type = entry.getKey();
			result.put(type, this.getPoints(type, mesure, tag));
		}

		return result;
	}

	public final Map<DataType, Map<Mesure, List<DataPoint>>> getAllPoints(final List<Mesure> mesures, final Optional<Tag> tag) {
		final Map<DataType, Map<Mesure, List<DataPoint>>> result = new HashMap<DataType, Map<Mesure, List<DataPoint>>>();
		addMissingDataTypes(result, () -> new HashMap<Mesure, List<DataPoint>>());

		for (final Mesure mesure: mesures) {
			for (final Map.Entry<DataType, List<DataPoint>> entry: this.getPoints(mesure, tag).entrySet()) {
				result.get(entry.getKey()).put(mesure, entry.getValue());
			}
		}

		return result;
	}

	public final Range<Float> getValueRange(final List<DataPoint> points) {
		if (points.isEmpty()) {
			return new Range<Float>(0.0f, 1.0f);
		}

		Float minimum = Float.POSITIVE_INFINITY;
		Float maximum = Float.NEGATIVE_INFINITY;

		for (final DataPoint point: points) {
			Float value = point.getValue();
			if (value < minimum) {
				minimum = value;
			} else if (value > maximum) {
				maximum = value;
			}
		}

		return new Range<Float>(minimum, maximum);
	}

	public final <T> Map<T, Range<Float>> getValueRangeFromMap(final Map<T, List<DataPoint>> points) {
		final Map<T, Range<Float>> result = new HashMap<T, Range<Float>>();

		for (final Map.Entry<T, List<DataPoint>> entry: points.entrySet()) {
			result.put(entry.getKey(), this.getValueRange(entry.getValue()));
		}

		return result;
	}

	public final <S, T> Map<S, Map<T, Range<Float>>> getValueRangeFromNestedMap(final Map<S, Map<T, List<DataPoint>>> points) {
		final Map<S, Map<T, Range<Float>>> result = new HashMap<S, Map<T, Range<Float>>>();

		for (final Map.Entry<S, Map<T, List<DataPoint>>> entry: points.entrySet()) {
			result.put(entry.getKey(), this.getValueRangeFromMap(entry.getValue()));
		}

		return result;
	}

	public final List<Range<Float>> getOmittedRanges(final Mesure mesure) {
		final List<Range<Float>> result = new ArrayList<Range<Float>>(this.omittedPoints.get(mesure).keySet());

		Collections.sort(result);

		return result;
	}

	public final List<Range<Float>> getAllOmittedRanges() {
		final List<Range<Float>> result = new ArrayList<Range<Float>>();

		// Get all omitted ranges, but do not use `getOmittedRanges` to avoid costly sort
		for (final Map<Range<Float>, List<DataPoint>> ranges: this.omittedPoints.values()) {
			result.addAll(ranges.keySet());
		}

		Collections.sort(result);

		return result;
	}

	public final List<DataPoint> getOmittedPoints(final Mesure mesure, final Range<Float> range) {
		return this.omittedPoints.get(mesure).get(range);
	}

	public final String getHeadingComment() {
		return this.headingComment;
	}

}
