package code_metier_tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import code_metier.DataPoint;
import code_metier.DataType;
import code_metier.ExperimentDataStore;
import code_metier.ExperimentManager;
import code_metier.Measure;

/**
 * 
 * @author Rémi BARDON
 */
@DisplayName("Manager")
class ExperimentManagerTest {

	private static Method CLEAN;

	private ExperimentManager manager;

	/**
	 * Gets references to package-visible methods using reflection. For more
	 * information, see <a href="https://stackoverflow.com/a/34658/10967642">How do I test a private function or a class that has private methods, fields or inner classes?</a>
	 * 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	@BeforeAll
	public static void getMethods() throws Exception {
		CLEAN = ExperimentManager.class.getDeclaredMethod("clean", Measure.class, BiConsumer.class, Consumer.class);
		CLEAN.setAccessible(true);
	}

	@BeforeEach
	public void setUp() {
		this.manager = new ExperimentManager();
		this.manager.setLoggingEnabled(false);
		this.manager.setPreComputingEnabled(false);
	}

	@AfterEach
	public void tearDown() {
		this.manager.setLoggingEnabled(true);
		this.manager.stopBackgroundThreads();
	}

	@Test
	@Slow
	@Timeout(1)
	@DisplayName("Async Clean")
	void testAsyncClean() throws Exception {
		final var file = new File("src/test/resources/constant.txt");

		final var result = new AsyncResult<ExperimentDataStore>();

		this.manager.setLoggingEnabled(true);
		this.manager.setPreComputingEnabled(true);
		this.manager.load(
			file,
			(progress, total) -> {},
			(measures, tags) -> {
				System.out.println("Measures: " + measures + "; Tags: " + tags);

				System.out.println("Cleaning...");
				BiConsumer<Integer, Integer> progressCallback = (progress, total) -> { System.out.println("Cleaning " + progress + "/" + total); };
				Consumer<ExperimentDataStore> completionHandler = store -> {
					result.setValue(store);
				};

				try {
					CLEAN.invoke(
						this.manager,
						measures.get(0),
						progressCallback,
						completionHandler
					);
				} catch (Exception e) {
					e.printStackTrace();
					result.setValue(null);
				}
			}
		);

		while (!result.isAvailable()) {
			Thread.sleep(100);
		}

		// Use assertTrue instead of assertNull to avoid printing (long) object if assertion failed
		assertTrue(result.getValue() != null);
	}

	@Test
	@Slow
	@Timeout(1)
	@DisplayName("Async Decompose")
	void testDecompose() throws Exception {
		final var file = new File("src/test/resources/constant.txt");

		final var result = new AsyncResult<Map<DataType, List<DataPoint>>>();

		this.manager.setLoggingEnabled(true);
		this.manager.setPreComputingEnabled(false);
		this.manager.load(
			file,
			(progress, total) -> {},
			(measures, tags) -> {
				System.out.println("Measures: " + measures + "; Tags: " + tags);

				System.out.println("Decomposing...");
				this.manager.decompose(
					measures.get(0),
					4,
					(progress, total) -> { System.out.println("Decomposing " + progress + "/" + total); },
					(map) -> {
						result.setValue(map);
					}
				);
			}
		);

		while (!result.isAvailable()) {
			Thread.sleep(100);
		}

		// Use assertTrue instead of assertNull to avoid printing (long) object if assertion failed
		assertTrue(result.getValue() != null);
	}

	@Test
	@Slow
	@Timeout(15)
	@DisplayName("Async Decompose Real Data")
	void testDecomposeRealData() throws Exception {
		final var file = new File("src/test/resources/real_data-group_3.txt");

		final var result = new AsyncResult<Map<DataType, List<DataPoint>>>();

		this.manager.setLoggingEnabled(true);
		this.manager.setPreComputingEnabled(false);
		this.manager.load(
			file,
			(progress, total) -> {},
			(measures, tags) -> {
				System.out.println("Measures: " + measures + "; Tags: " + tags);

				System.out.println("Decomposing...");
				this.manager.decompose(
					measures.get(0),
					4,
					(progress, total) -> { System.out.println("Decomposing " + progress + "/" + total); },
					(map) -> {
						result.setValue(map);
					}
				);
			}
		);

		while (!result.isAvailable()) {
			Thread.sleep(100);
		}

		// Use assertTrue instead of assertNull to avoid printing (long) object if assertion failed
		assertTrue(result.getValue() != null);
	}

	class AsyncResult<T> {

		private boolean available = false;
		private T value = null;

		T getValue() {
			return this.value;
		}

		void setValue(T value) {
			this.value = value;
			this.available = true;
		}

		boolean isAvailable() {
			return this.available;
		}

	}

}
