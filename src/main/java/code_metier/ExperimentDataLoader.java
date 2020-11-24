package code_metier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

public final class ExperimentDataLoader {

	private static char DELIMITER = '\t';
	private static Pattern COMMENT_PREFIX = Pattern.compile("^#\s?");
	private static NumberFormat FORMAT = NumberFormat.getInstance(Locale.FRANCE);
	private static Pattern TAG_PREFIX = Pattern.compile("^#\\* ");

	private String headingComment;
	private List<Measure> measures;
	private Map<Measure, List<DataPoint>> dataPoints;
	private Map<Measure, Map<Tag, Range<Float>>> phases;

	public ExperimentDataLoader() {
		this.headingComment = "";
		this.measures = new ArrayList<Measure>();
		this.dataPoints = new HashMap<Measure, List<DataPoint>>();
		this.phases = new HashMap<Measure, Map<Tag, Range<Float>>>();
	}

	public Map<Measure, List<DataPoint>> getDataPoints() {
		return this.dataPoints;
	}

	/**
	 * 
	 * @param measure
	 * @param optionalTag
	 * @return
	 *         <ul>
	 *         <li>An empty {@link List} if the given {@link Measure} doesn't
	 *         exist</li>
	 *         <li>All points for given {@link Measure} if given
	 *         {@link Optional}<{@link Tag}> is {@code empty}</li>
	 *         <li>An empty {@link List} if the given {@link Tag} doesn't exist (for
	 *         given {@link Measure})</li>
	 *         <li>Otherwise, the {@link DataPoint}s corresponding to given
	 *         {@link Measure} and {@link Tag}</li>
	 *         </ul>
	 */
	public List<DataPoint> getDataPoints(final Measure measure, final Optional<Tag> optionalTag) {
		if (!this.dataPoints.containsKey(measure) || !this.phases.containsKey(measure)) {
			return new ArrayList<DataPoint>();
		}

		if (optionalTag.isEmpty()) {
			return this.dataPoints.get(measure);
		}
		final Tag tag = optionalTag.get();

		// Return empty {@code List} if
		if (!this.phases.get(measure).containsKey(tag)) {
			return new ArrayList<DataPoint>();
		}

		final Range<Float> range = this.phases.get(measure).get(tag);
		final List<DataPoint> points = this.dataPoints.get(measure);
		final List<DataPoint> result = new ArrayList<DataPoint>();

		for (DataPoint point : points) {
			if (range.contains(point.getTimestamp())) {
				result.add(point);
			}
		}

		return result;
	}

	public String getHeadingComment() {
		return this.headingComment;
	}

	public List<Measure> getMeasures() {
		return this.measures;
	}

	public Map<Measure, Map<Tag, Range<Float>>> getPhases() {
		return this.phases;
	}

	public List<Tag> getTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();

		for (final Measure measure : this.measures) {
			tags.addAll(this.getTags(measure));
		}

		return new ArrayList<Tag>(tags);
	}

	public List<Tag> getTags(final Measure measure) {
		if (!this.phases.containsKey(measure)) {
			return new ArrayList<Tag>();
		}

		return new ArrayList<Tag>(this.phases.get(measure).keySet());
	}

	public void load(final File file) throws IOException, CsvValidationException, ParseException {
		CSVReader csvReader = null;

		try {
			// Create CSV Reader
			final FileReader fileReader = new FileReader(file, Charset.forName("UTF-8"));
			final CSVParser parser = new CSVParserBuilder().withSeparator(DELIMITER).build();
			csvReader = new CSVReaderBuilder(fileReader).withCSVParser(parser).build();

			this.readHeader(file, csvReader);
			this.readDataPoints(file, csvReader);
		} finally {
			if (csvReader != null) {
				csvReader.close();
			}
		}
	}

	private void readDataPoints(final File file, final CSVReader csvReader)
			throws CsvValidationException, IOException, ParseException {
		this.dataPoints.clear();
		Tag actualTag = Tag.PREPARATION;
		this.phases.clear();
		for (final Measure measure : this.measures) {
			this.dataPoints.put(measure, new ArrayList<DataPoint>());
			this.phases.put(measure, new LinkedHashMap<Tag, Range<Float>>());
			this.phases.get(measure).put(actualTag, new Range<Float>(0f, 0f));
		}

		final int measureCount = this.measures.size();
		final int measuresIndex = 1;
		final int tagIndex = measuresIndex + measureCount;

		int validLinesRead = 0;

		String[] split = null;
		while ((split = csvReader.readNext()) != null) {
			// Skip line if empty
			if (split[0].isEmpty()) {
				continue;
			}

			// Parse timestamp
			final float timestamp = FORMAT.parse(split[0]).floatValue();

			// Store tag if any
			if (!split[tagIndex].isEmpty()) {
				if (validLinesRead == 0) {
					// If first line has a tag, remove default preparation tag
					for (final Measure measure : this.measures) {
						this.phases.get(measure).clear();
					}
				}

				actualTag = new Tag(TAG_PREFIX.matcher(split[6]).replaceFirst(""));

				for (final Measure measure : this.measures) {
					this.phases.get(measure).put(actualTag, new Range<Float>(timestamp, timestamp));
				}
			}

			// Parse measures data and store it
			for (int i = 0; i < measureCount; i++) {
				final DataPoint newPoint = new DataPoint(timestamp,
						FORMAT.parse(split[i + measuresIndex]).floatValue());
				final Measure measure = this.measures.get(i);

				this.dataPoints.get(measure).add(newPoint);
			}

			// Update phase end
			for (final Measure measure : this.measures) {
				this.phases.get(measure).get(actualTag).setMaximum(timestamp);
			}

			validLinesRead++;
		}
	}

	private void readHeader(final File file, final CSVReader csvReader) throws IOException, CsvValidationException {
		this.headingComment = "";
		this.measures.clear();

		String[] split;

		// Loop until we meet a line starting with `"0\t"` for the first time
		// Note: We use `.peek()` in the condition to avoid moving cursor after first
		// line of data
		while (((split = csvReader.peek()) != null) && !split[0].equals("0")) {
			// Read line again running validations and moving cursor in file
			split = csvReader.readNext();

			if (COMMENT_PREFIX.matcher(split[0]).find()) {
				// If line is a comment

				// Remove comment prefix
				split[0] = COMMENT_PREFIX.matcher(split[0]).replaceFirst("");

				// Store comment line
				String newComment = String.join(DELIMITER + "", split);
				this.headingComment = String.join(this.headingComment.isEmpty() ? "" : "\n", this.headingComment,
						newComment);
			} else if (this.measures.isEmpty()) {
				for (int i = 1; i < split.length - 1; i++) {
					this.measures.add(new Measure(split[i]));
				}
			}
		}

		// Use default measure names if none were found
		if (this.measures.isEmpty()) {
			final int valueCount = split.length - 2;
			for (int i = 1; i <= valueCount; i++) {
				this.measures.add(new Measure("Mesure " + i));
			}
		}
	}

}