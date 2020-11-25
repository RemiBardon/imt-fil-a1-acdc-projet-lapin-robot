package code_metier_tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import code_metier.DataPoint;
import code_metier.DataType;
import code_metier.ExperimentDataCleaner;
import code_metier.ExperimentDataDecomposer;
import code_metier.ExperimentDataLoader;
import code_metier.Measure;
import code_metier.Tag;

/**
 * 
 * @author Rémi BARDON
 */
@DisplayName("Decomposer")
public class ExperimentDataDecomposerTest {

	static Constructor<DataPoint> DATA_POINT_CONSTRUCTOR;
	static Constructor<Measure> MEASURE_CONSTRUCTOR;
	static Constructor<Tag> TAG_CONSTRUCTOR;

	private ExperimentDataLoader loader;
	private ExperimentDataCleaner cleaner;
	private ExperimentDataDecomposer decomposer;

	/**
	 * Gets references to package-visible constructors using reflection. For more
	 * information, see <a href="https://stackoverflow.com/a/14077876/10967642">How
	 * to test a private constructor in Java application?</a>
	 * 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @author Rémi BARDON
	 */
	@BeforeAll
	public static void getConstructors() throws Exception {
		MEASURE_CONSTRUCTOR = Measure.class.getDeclaredConstructor(String.class);
		MEASURE_CONSTRUCTOR.setAccessible(true);

		TAG_CONSTRUCTOR = Tag.class.getDeclaredConstructor(String.class);
		TAG_CONSTRUCTOR.setAccessible(true);

		DATA_POINT_CONSTRUCTOR = DataPoint.class.getDeclaredConstructor(float.class, Float.class);
		DATA_POINT_CONSTRUCTOR.setAccessible(true);
	}

	/**
	 * 
	 * @author Rémi BARDON
	 */
	@BeforeEach
	public void setUp() {
		this.loader = new ExperimentDataLoader();
		this.cleaner = new ExperimentDataCleaner();
		this.decomposer = new ExperimentDataDecomposer();
	}

	/**
	 * 
	 * @author Rémi BARDON
	 */
	@Nested
	public class Constant {

		private ExperimentDataLoader loader;
		private ExperimentDataCleaner cleaner;
		private ExperimentDataDecomposer decomposer;

		@BeforeEach
		public void setUp() throws Exception {
			this.loader = new ExperimentDataLoader();
			this.loader.load(new File("src/test/resources/constant.txt"));

			this.cleaner = new ExperimentDataCleaner();
			for (final var measure : this.loader.getMeasures()) {
				final var points = this.loader.getDataPoints(measure);
				this.cleaner.clean(points, this.loader.getPhases(measure));
			}

			this.decomposer = new ExperimentDataDecomposer();
		}

		@Test
		@DisplayName("Decompose Data")
		public void testDecomposeData() throws Exception {
			for (final var measure : this.loader.getMeasures()) {
				final var points = this.loader.getDataPoints(measure);
				this.decomposer.decompose(points, 2);

				for (final var type : DataType.values()) {
					assertEquals(30, this.decomposer.getPoints(type).size(), measure + "/" + type);
				}
			}
		}

	}

	/**
	 * 
	 * @author Rémi BARDON
	 */
	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	public class Performance {

		private Method DATA_POINTS_TO_DOUBLES;

		private ExperimentDataDecomposer decomposer;
		private List<DataPoint> points;

		/**
		 * Gets references to package-visible methods using reflection. For more
		 * information, see <a href="https://stackoverflow.com/a/34658/10967642">How do I test a private function or a class that has private methods, fields or inner classes?</a>
		 * 
		 * @throws NoSuchMethodException
		 * @throws SecurityException
		 * @author Rémi BARDON
		 */
		@BeforeAll
		public void getMethods() throws Exception {
			DATA_POINTS_TO_DOUBLES = ExperimentDataDecomposer.class.getDeclaredMethod("dataPointsValues", List.class);
			DATA_POINTS_TO_DOUBLES.setAccessible(true);
		}

		/**
		 * 
		 * @throws Exception
		 * @author Rémi BARDON
		 */
		@BeforeEach
		public void setUp() throws Exception {
			this.decomposer = new ExperimentDataDecomposer();
			this.points = new ArrayList<DataPoint>();
			for (float i = 0; i < 100_000; i++) {
				this.points.add(ExperimentDataDecomposerTest.DATA_POINT_CONSTRUCTOR.newInstance(i, i));
			}
		}

		/**
		 * 
		 * @throws Exception
		 * @author Rémi BARDON
		 */
		@RepeatedTest(10)
		@DisplayName("Map List<DataPoint> to double[]")
		public void testMapDataPointsToDoubleValues() throws Exception {
			@SuppressWarnings("unused")
			final var _temp = DATA_POINTS_TO_DOUBLES.invoke(this.decomposer, this.points);
		}

	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@Slow
	@DisplayName("Decompose Real Data")
	public void testDecomposeRealData() throws Exception {
		this.loader.load(new File("src/test/resources/real_data-group_3.txt"));

		final var measure = MEASURE_CONSTRUCTOR.newInstance("Mesure 2");

		final var points = this.loader.getDataPoints(measure);
		this.cleaner.clean(points, this.loader.getPhases(measure));
		this.decomposer.decompose(points, 4);

		for (final var type : DataType.values()) {
			assertEquals(817_443, this.decomposer.getPoints(type).size(), measure + "/" + type);
		}

		// RAW = TREND + SEASONNALITY + NOISE
		final var raw = this.decomposer.getPoints(DataType.RAW).get(1).getValue();
		final var trend = this.decomposer.getPoints(DataType.TREND).get(1).getValue();
		final var seasonnality = this.decomposer.getPoints(DataType.SEASONNALITY).get(1).getValue();
		final var noise = this.decomposer.getPoints(DataType.NOISE).get(1).getValue();
		assertEquals(raw, trend + seasonnality + noise);
	}

}
