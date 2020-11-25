package code_metier;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.opencsv.exceptions.CsvValidationException;

/**
 * A wrapper for all operations related to loading, cleaning and decomposing experiment data.
 * Has the advantage of using caching to improve performance.
 * @author Rémi BARDON
 */
public class ExperimentManager {

	private boolean isLoggingEnabled = false;
	private boolean isPreComputingEnabled = true;

	private ExperimentDataLoader loader;
	private ExperimentDataCleaner cleaner;
	private ExperimentDataDecomposer decomposer;

	private String currentFilePath;
	private String currentFileName;
	private Optional<Thread> loadingThread;
	private Optional<Thread> cleaningThread;
	private Map<String, Thread> preCleaningThreads;
	private Map<String, Map<Measure, ExperimentDataStore>> cleanedPointsCache;
	private Map<String, Thread> decomposingThreads;
	private Map<String, Map<Measure, Map<DataType, List<DataPoint>>>> decomposedPointsCache;

	public ExperimentManager() {
		this.loader = new ExperimentDataLoader();
		this.cleaner = new ExperimentDataCleaner();
		this.decomposer = new ExperimentDataDecomposer();

		this.loadingThread = Optional.empty();
		this.cleaningThread = Optional.empty();

		this.preCleaningThreads = new HashMap<String, Thread>();
		this.cleanedPointsCache = new HashMap<String, Map<Measure, ExperimentDataStore>>();

		this.decomposingThreads = new HashMap<String, Thread>();
		this.decomposedPointsCache = new HashMap<String, Map<Measure, Map<DataType, List<DataPoint>>>>();
	}

	/**
	 * Loads and decodes a {@code CSV} file, reading its data points.
	 * @param file The {@link File} to open
	 * @param progressCallback A {@link BiConsumer} callback called during loading process.
	 *                         Will be executed multiple times with fist argument as actual progress
	 *                         and second argument as total expected progress.
	 *                         {@link Integer}s represent the number of lines read/to read.<br>
	 *                         <b>Note: </b>Currently, {@link #progressCallback} sends {@code 0} and {@code 1} when loading finishes.
	 * @param completionHandler A {@link BiConsumer} callback called when loading finishes.
	 *                          It sends the {@link Measure}s and {@link Tag}s present in the {@link File}.
	 * @throws IOException
	 * @throws CsvValidationException
	 * @throws ParseException
	 * @author Rémi BARDON
	 */
	public void load(
		final File file,
		final BiConsumer<Integer, Integer> progressCallback,
		final BiConsumer<List<Measure>, List<Tag>> completionHandler
	) {
		// Interrupt existing thread
		this.loadingThread.ifPresent((thread) -> { thread.interrupt(); });

		progressCallback.accept(0, 1);

		final String filePath = file.getAbsolutePath();
		final String fileName = file.getName();

		this.currentFilePath = filePath;
		this.currentFileName = fileName;

		final Thread thread = new Thread(() -> {
			try {
				this.loader.load(file);

				// Log success
				if (this.isLoggingEnabled) { System.out.println("Loaded points in '" + fileName + "'"); }

				// Send completion
				final var measures 	= new ArrayList<Measure>(this.loader.getMeasures());
				final var tags 		= new ArrayList<Tag>(this.loader.getAllTags());
				progressCallback.accept(1, 1);
				completionHandler.accept(measures, tags);
			} catch (Exception e) {
				// Log error
				if (this.isLoggingEnabled) {
					System.err.println("Error loading points in '" + fileName + "'");
					e.printStackTrace();
				}

				// Send completion
				progressCallback.accept(1, 1);
				completionHandler.accept(null, null);
			}
		});
		thread.setName("Loading thread for '" + filePath + "'");

		this.loadingThread = Optional.of(thread);

		thread.start();
	}

	private void clean(
		final Measure measure,
		final BiConsumer<Integer, Integer> progressCallback,
		final Consumer<ExperimentDataStore> completionHandler
	) {
		// Interrupt existing thread
		this.cleaningThread.ifPresent((thread) -> { thread.interrupt(); });

		progressCallback.accept(0, 1);

		final String filePath = this.currentFilePath;
		final String fileName = this.currentFileName;

		if (
			this.cleanedPointsCache.containsKey(filePath)
				&& this.cleanedPointsCache.get(filePath).containsKey(measure)
		) {
			// If already cleaned, skip cleaning
			progressCallback.accept(1, 1);
			completionHandler.accept(this.cleanedPointsCache.get(filePath).get(measure));
		}

		final Thread thread = new Thread(() -> {
			try {
				final var points = this.loader.getDataPoints(measure);
				final var phases = this.loader.getPhases(measure);
				this.cleaner.clean(points, phases);

				this.cleanedPointsCache.putIfAbsent(filePath, new HashMap<Measure, ExperimentDataStore>());
				final var cleanedMeasures = this.cleanedPointsCache.get(filePath);
				final var store = this.loader.getStore(measure);
				cleanedMeasures.put(measure, store);

				// Log success
				if (this.isLoggingEnabled) { System.out.println("Cleaned '" + measure + "' points in '" + fileName + "'"); }

				// Start cleaning other measures on a background thread
				if (this.isPreComputingEnabled) { cleanOnBackgroundThread(this.loader, filePath); }

				// Send completion
				progressCallback.accept(1, 1);
				completionHandler.accept(store);
			} catch (InvalidKeyException e) {
				// Log error
				if (this.isLoggingEnabled) {
					System.err.println("Error cleaning '" + measure + "' points in '" + fileName + "'");
					e.printStackTrace();
				}

				// Send completion
				progressCallback.accept(1, 1);
				completionHandler.accept(null);
			}
		});
		thread.setName("Cleaning thread for '" + filePath + "'");

		this.cleaningThread = Optional.of(thread);

		thread.start();
	}

	private void cleanOnBackgroundThread(final ExperimentDataLoader loader, final String filePath) {
		final String fileName = new File(filePath).getName();

		// Avoid creating multiple threads for same task
		if (this.preCleaningThreads.containsKey(filePath)) { return; }

		if (!this.cleanedPointsCache.containsKey(filePath)) { return; }
		final var cleanedMeasures = this.cleanedPointsCache.get(filePath);

		final Thread cleaningThread = new Thread(() -> {
			for (final var measure: loader.getMeasures()) {
				if (cleanedMeasures.containsKey(measure)) {
					// Skip Measure if already cleaned
					continue;
				}

				// Clean next Measure
				this.clean(measure, (_current, _total) -> {}, store -> {
					cleanedMeasures.put(measure, store);
				});
			}
		});
		cleaningThread.setName("Pre-cleaning thread for '" + fileName + "'");

		this.preCleaningThreads.put(filePath, cleaningThread);

		cleaningThread.start();
	}

	public void decompose(
		final Measure measure,
		final int period,
		final BiConsumer<Integer, Integer> progressCallback,
		final Consumer<Map<DataType, List<DataPoint>>> completionHandler
	) {
		progressCallback.accept(0, 2);

		final String filePath = this.currentFilePath;
		final String fileName = this.currentFileName;

		if (
			this.decomposedPointsCache.containsKey(filePath)
				&& this.decomposedPointsCache.get(filePath).containsKey(measure)
				&& this.decomposedPointsCache.get(filePath).get(measure).keySet().size() == DataType.values().length
		 ) {
			// If already decomposed, skip cleaning
			progressCallback.accept(2, 2);
			completionHandler.accept(this.decomposedPointsCache.get(filePath).get(measure));
		}

		this.clean(
			measure,
			(progress, total) -> {},
			(store) -> {
				progressCallback.accept(1, 2);

				this.decomposer.decompose(store.getDataPoints(), period);

				this.decomposedPointsCache.putIfAbsent(filePath, new HashMap<Measure, Map<DataType, List<DataPoint>>>());
				this.decomposedPointsCache.get(filePath).put(measure, decomposer.getAllPoints());
				final var decomposedTypes = this.decomposedPointsCache.get(filePath).get(measure);

				// Log success
				if (this.isLoggingEnabled) { System.out.println("Decomposed '" + measure + "' points in '" + fileName + "'"); }

				// Send completion
				progressCallback.accept(2, 2);
				completionHandler.accept(decomposedTypes);
			}
		);
	}

	public void emptyCache(final String filePath) {
		if (this.decomposingThreads.containsKey(filePath)) {
			this.decomposingThreads.get(filePath).interrupt();
		}
		this.decomposedPointsCache.remove(filePath);

		if (this.preCleaningThreads.containsKey(filePath)) {
			this.preCleaningThreads.get(filePath).interrupt();
		}
		this.cleanedPointsCache.remove(filePath);
	}

	public void stopBackgroundThreads() {
		final Consumer<Thread> interrupt = (thread) -> { thread.interrupt(); };

		this.cleaningThread.ifPresent(interrupt) ;
		this.decomposingThreads.values().forEach(interrupt);
		this.preCleaningThreads.values().forEach(interrupt);

		if (this.isLoggingEnabled) { System.out.println("Stopped all background threads"); }
	}

	public String getHeadingComment() {
		return this.loader.getHeadingComment();
	}

	public List<DataPoint> getDataPoints(final Measure measure) throws InvalidKeyException {
		return this.loader.getDataPoints(measure);
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
	 */
	public List<DataPoint> getDataPoints(final Measure measure, final Optional<Tag> optionalTag) throws InvalidKeyException {
		return this.loader.getDataPoints(measure, optionalTag);
	}

	public List<Range<Float>> getOmittedRanges() {
		return this.cleaner.getOmittedRanges();
	}

	public List<DataPoint> getOmittedPoints(final Range<Float> range) {
		return this.cleaner.getOmittedPoints(range);
	}

	public void setLoggingEnabled(final boolean enabled) {
		this.isLoggingEnabled = enabled;
	}

	public void setPreComputingEnabled(final boolean enabled) {
		this.isPreComputingEnabled = enabled;
	}

	@Override
	public void finalize() {
		this.stopBackgroundThreads();
	}

}
