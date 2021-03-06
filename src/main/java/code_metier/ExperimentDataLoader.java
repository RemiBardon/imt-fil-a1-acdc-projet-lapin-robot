package code_metier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
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

/**
 * 
 * @author Rémi BARDON
 */
public final class ExperimentDataLoader {

	/**
	 * The {@code CSV} delimiter
	 * @author Rémi BARDON
	 */
	private static char DELIMITER = '\t';
	/**
	 * The comment prefix for file header
	 * @author Rémi BARDON
	 */
	private static Pattern COMMENT_PREFIX = Pattern.compile("^#\s?");
	/**
	 * The French {@link NumberFormat} for decoding comma ({@code ,}) separated decimal numbers
	 * @author Rémi BARDON
	 */
	private static NumberFormat FORMAT = NumberFormat.getInstance(Locale.FRANCE);
	/**
	 * The prefix for {@link Tag}s in data files
	 * @author Rémi BARDON
	 */
	private static Pattern TAG_PREFIX = Pattern.compile("^#\\* ");

	/**
	 * 
	 * @author Rémi BARDON
	 */
	private String headingComment;
	/**
	 * 
	 * @author Rémi BARDON
	 */
	private List<Measure> measures;
	/**
	 * 
	 * @author Rémi BARDON
	 */
	private Map<Measure, ExperimentDataStore> stores;

	/**
	 * A class responsible for loading data points from a {@code CSV} file
	 * @author Rémi BARDON
	 */
	public ExperimentDataLoader() {
		this.headingComment = "";
		this.measures = new ArrayList<Measure>();
		this.stores = new HashMap<Measure, ExperimentDataStore>();
	}

	/**
	 * 
	 * @param file The {@link File} to load and parse
	 * @throws IOException
	 * @throws CsvValidationException
	 * @throws ParseException
	 * @author Rémi BARDON
	 */
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

	/**
	 * 
	 * @param file
	 * @param csvReader
	 * @throws IOException
	 * @throws CsvValidationException
	 * @author Rémi BARDON
	 */
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

	/**
	 * 
	 * @param file
	 * @param csvReader
	 * @throws CsvValidationException
	 * @throws IOException
	 * @throws ParseException
	 * @author Rémi BARDON
	 */
	private void readDataPoints(final File file, final CSVReader csvReader) throws CsvValidationException, IOException, ParseException {
		this.stores.clear();
		Tag actualTag = Tag.PREPARATION;
		for (final Measure measure : this.measures) {
			final var store = new ExperimentDataStore();
			store.getPhases().put(actualTag, new Range<Float>(0f, 0f));
			this.stores.put(measure, store);
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
					this.stores.values().forEach((store) -> {
						store.getPhases().clear();
					});
				}

				actualTag = new Tag(TAG_PREFIX.matcher(split[6]).replaceFirst(""));

				for (final var store: this.stores.values()) {
					store.getPhases().put(actualTag, new Range<Float>(timestamp, timestamp));
				}
			}

			// Parse measures data and store it
			for (int i = 0; i < measureCount; i++) {
				final Float value = FORMAT.parse(split[i + measuresIndex]).floatValue();
				final DataPoint newPoint = new DataPoint(timestamp, value);
				final Measure measure = this.measures.get(i);

				try {
					this.getDataPoints(measure).add(newPoint);
				} catch (InvalidKeyException e) {
					// Silently dismiss exception
				}
			}

			// Update phase end
			for (final var store: this.stores.values()) {
				store.getPhases().get(actualTag).setMaximum(timestamp);
			}

			validLinesRead++;
		}
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public String getHeadingComment() {
		return this.headingComment;
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public List<Measure> getMeasures() {
		return this.measures;
	}

	/**
	 * 
	 * @param measure
	 * @return
	 * @throws InvalidKeyException
	 * @author Rémi BARDON
	 */
	public Map<Tag, Range<Float>> getPhases(final Measure measure) throws InvalidKeyException {
		if (!this.stores.containsKey(measure)) {
			throw new InvalidKeyException();
		}

		return this.stores.get(measure).getPhases();
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public List<Tag> getAllTags() {
		final Set<Tag> tags = new LinkedHashSet<Tag>();

		this.stores.values().forEach((store) -> {
			tags.addAll(store.getTags());
		});

		return new ArrayList<Tag>(tags);
	}

	/**
	 * 
	 * @param measure
	 * @return
	 * @throws InvalidKeyException
	 * @author Rémi BARDON
	 */
	public List<Tag> getTags(final Measure measure) throws InvalidKeyException {
		if (!this.stores.containsKey(measure)) {
			throw new InvalidKeyException();
		}

		return new ArrayList<Tag>(this.stores.get(measure).getTags());
	}

	/**
	 * 
	 * @return
	 * @author Rémi BARDON
	 */
	public Map<Measure, ExperimentDataStore> getStores() {
		return this.stores;
	}

	/**
	 * 
	 * @param measure
	 * @return
	 * @throws InvalidKeyException
	 * @author Rémi BARDON
	 */
	public ExperimentDataStore getStore(final Measure measure) throws InvalidKeyException {
		if (!this.stores.containsKey(measure)) {
			throw new InvalidKeyException();
		}

		return this.stores.get(measure);
	}

	/**
	 * 
	 * @param measure
	 * @return
	 * @throws InvalidKeyException
	 * @author Rémi BARDON
	 */
	public List<DataPoint> getDataPoints(final Measure measure) throws InvalidKeyException {
		return this.getStore(measure).getDataPoints();
	}

	/**
	 * 
	 * @param measure
	 * @param optionalTag An {@link Optional} {@link Tag} to filter results
	 * @return <ul>
	 *     <li>All points for given {@link Measure} if given {@link Optional}<{@link Tag}> is {@code empty}</li>
	 *     <li>An empty {@link List} if the given {@link Tag} doesn't exist (for given {@link Measure})</li>
	 *     <li>Otherwise, the {@link DataPoint}s corresponding to given {@link Measure} and {@link Tag}</li>
	 * </ul>
	 * @throws InvalidKeyException If the given {@link Measure} doesn't exist
	 * @author Rémi BARDON
	 */
	public List<DataPoint> getDataPoints(final Measure measure, final Optional<Tag> optionalTag) throws InvalidKeyException {
		return this.getStore(measure).getDataPoints(optionalTag);
	}

}
