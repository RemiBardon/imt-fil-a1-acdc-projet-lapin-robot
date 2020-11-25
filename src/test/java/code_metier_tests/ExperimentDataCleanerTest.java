package code_metier_tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import code_metier.DataPoint;
import code_metier.ExperimentDataCleaner;
import code_metier.ExperimentDataLoader;
import code_metier.Measure;
import code_metier.Range;
import code_metier.Tag;


/**
 * 
 * @author Rémi BARDON
 */
@DisplayName("Cleaner")
public class ExperimentDataCleanerTest {

	static Constructor<Measure> MEASURE_CONSTRUCTOR;
	static Constructor<Tag> TAG_CONSTRUCTOR;
	static Constructor<DataPoint> DATA_POINT_CONSTRUCTOR;

	private ExperimentDataLoader loader;
	private ExperimentDataCleaner cleaner;

	/**
	 * Gets references to package-visible constructors using reflection. For more
	 * information, see <a href="https://stackoverflow.com/a/14077876/10967642">How
	 * to test a private constructor in Java application?</a>
	 * 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
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

	@BeforeEach
	public void setUp() {
		this.loader = new ExperimentDataLoader();
		this.cleaner = new ExperimentDataCleaner();
	}

	@Test
	@DisplayName("Offset Points Timestamps To Remove Holes")
	public void testOffsetTimestamps() throws Exception {
		this.loader.load(new File("src/test/resources/test_data-with_nans.txt"));

		final var expected = new HashMap<Measure, List<DataPoint>>();
		expected.put(
			MEASURE_CONSTRUCTOR.newInstance("Pression Arterielle"),
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(0f, 36f),
				DATA_POINT_CONSTRUCTOR.newInstance(1f, 36f),
				DATA_POINT_CONSTRUCTOR.newInstance(2f, 36f),
				DATA_POINT_CONSTRUCTOR.newInstance(3f, 36f),
				DATA_POINT_CONSTRUCTOR.newInstance(4f, 36f),
				DATA_POINT_CONSTRUCTOR.newInstance(5f, 36f)
			))
		);
		expected.put(
			MEASURE_CONSTRUCTOR.newInstance("Spirometrie"),
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(0f, 48f),
				DATA_POINT_CONSTRUCTOR.newInstance(1f, 48f),
				DATA_POINT_CONSTRUCTOR.newInstance(2f, 48f),
				DATA_POINT_CONSTRUCTOR.newInstance(3f, 48f),
				DATA_POINT_CONSTRUCTOR.newInstance(4f, 48f),
				DATA_POINT_CONSTRUCTOR.newInstance(5f, 48f)
			))
		);
		expected.put(
			MEASURE_CONSTRUCTOR.newInstance("PA moyenne"),
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(0f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(1f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(2f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(3f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(4f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(5f, 32f)
			))
		);
		expected.put(
			MEASURE_CONSTRUCTOR.newInstance("Frequence Cardiaque"),
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(0f, 0f),
				DATA_POINT_CONSTRUCTOR.newInstance(1f, 0f),
				DATA_POINT_CONSTRUCTOR.newInstance(2f, 0f),
				DATA_POINT_CONSTRUCTOR.newInstance(3f, 0f),
				DATA_POINT_CONSTRUCTOR.newInstance(4f, 0f),
				DATA_POINT_CONSTRUCTOR.newInstance(5f, 0f)
			))
		);
		expected.put(
			MEASURE_CONSTRUCTOR.newInstance("Frequence Respiratoire"),
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(0f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(1f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(2f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(3f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(4f, 32f),
				DATA_POINT_CONSTRUCTOR.newInstance(5f, 32f)
			))
		);

		for (final var entry : expected.entrySet()) {
			final Measure measure = entry.getKey();
			final var points = this.loader.getDataPoints(measure);
			this.cleaner.clean(points, this.loader.getPhases(measure));
			assertEquals(entry.getValue(), points, measure.toString());
		}
	}

	@Test
	@DisplayName("Offset Phases While Cleaning")
	public void testPhasesOffset() throws Exception {
		this.loader.load(new File("src/test/resources/test_data-with_nans_tags.txt"));

		final Measure measure = MEASURE_CONSTRUCTOR.newInstance("Pression Arterielle");

		final var expected = new HashMap<Tag, List<DataPoint>>();
		expected.put(
			Tag.PREPARATION,
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(0f, 36f)
			))
		);
		expected.put(
			TAG_CONSTRUCTOR.newInstance("tag1"),
			new ArrayList<DataPoint>()
		);
		expected.put(
			TAG_CONSTRUCTOR.newInstance("tag2"),
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(1f, 36f),
				DATA_POINT_CONSTRUCTOR.newInstance(2f, 36f)
			))
		);
		expected.put(
			TAG_CONSTRUCTOR.newInstance("tag3"),
			new ArrayList<DataPoint>(Arrays.asList(
				DATA_POINT_CONSTRUCTOR.newInstance(3f, 36f)
			))
		);

		this.cleaner.clean(this.loader.getDataPoints(measure), this.loader.getPhases(measure));

		for (final var entry : expected.entrySet()) {
			final Tag tag = entry.getKey();
			final var actual = this.loader.getDataPoints(measure, Optional.of(tag));
			assertArrayEquals(entry.getValue().toArray(), actual.toArray(), tag.toString());
		}
	}

	@Test
	@DisplayName("Skip Bad Values")
	public void testSkipBadValues() throws Exception {
		this.loader.load(new File("src/test/resources/test_data-with_nans.txt"));

		for (final var measure : this.loader.getMeasures()) {
			final var points = this.loader.getDataPoints(measure);
			this.cleaner.clean(points, this.loader.getPhases(measure));
			assertEquals(6, points.size(), measure.toString());
		}
	}

	@Test
	@DisplayName("Store Omitted Ranges")
	public void testStoreOmittedRanges() throws Exception {
		this.loader.load(new File("src/test/resources/test_data-with_nans.txt"));

		final var omittedRanges = new HashMap<Measure, List<Range<Float>>>();
		omittedRanges.put(
			MEASURE_CONSTRUCTOR.newInstance("Pression Arterielle"),
			new ArrayList<Range<Float>>(Arrays.asList(
				new Range<Float>( 0f,  0f),
				new Range<Float>( 2f,  4f),
				new Range<Float>(10f, 10f)
			))
		);
		omittedRanges.put(
			MEASURE_CONSTRUCTOR.newInstance("Spirometrie"),
			new ArrayList<Range<Float>>(Arrays.asList(
				new Range<Float>( 0f,  0f),
				new Range<Float>( 3f,  5f),
				new Range<Float>(10f, 10f)
			))
		);
		omittedRanges.put(
			MEASURE_CONSTRUCTOR.newInstance("PA moyenne"),
			new ArrayList<Range<Float>>(Arrays.asList(
				new Range<Float>( 0f,  0f),
				new Range<Float>( 4f,  6f),
				new Range<Float>(10f, 10f)
			))
		);
		omittedRanges.put(
			MEASURE_CONSTRUCTOR.newInstance("Frequence Cardiaque"),
			new ArrayList<Range<Float>>(Arrays.asList(
				new Range<Float>( 0f,  0f),
				new Range<Float>( 5f,  7f),
				new Range<Float>(10f, 10f)
			))
		);
		omittedRanges.put(
			MEASURE_CONSTRUCTOR.newInstance("Frequence Respiratoire"),
			new ArrayList<Range<Float>>(Arrays.asList(
				new Range<Float>( 0f,  0f),
				new Range<Float>( 6f,  8f),
				new Range<Float>(10f, 10f)
			))
		);

		for (final var entry : omittedRanges.entrySet()) {
			final Measure measure = entry.getKey();
			this.cleaner.clean(this.loader.getDataPoints(measure), this.loader.getPhases(measure));
			assertEquals(entry.getValue(), this.cleaner.getOmittedRanges(), measure.toString());
		}
	}

	@Test
	@Slow
	@DisplayName("Clean Real Data")
	public void testCleanRealData() throws Exception {
		this.loader.load(new File("src/test/resources/real_data-group_3.txt"));

		final Measure measure = MEASURE_CONSTRUCTOR.newInstance("Mesure 2");
		final Tag[] tags = {
			TAG_CONSTRUCTOR.newInstance("preparation"),
			TAG_CONSTRUCTOR.newInstance("ach"),
			TAG_CONSTRUCTOR.newInstance("adr�naline"), // For some reason, we can't decode "adrénaline" correctly
			TAG_CONSTRUCTOR.newInstance("ocytocine"),
		};

		final int expectedTotalLines = 820_914;
		final var points = this.loader.getDataPoints(measure);

		this.cleaner.clean(points, this.loader.getPhases(measure));

		final int expectedTotalCleanLines = 817_443;
		final int[] expectedCleanLines = { 515_381, 42_990, 48_052, 211_020 };
		assertEquals(expectedTotalCleanLines, Arrays.stream(expectedCleanLines).sum());
		assertEquals(expectedTotalCleanLines, points.size());
		for (int i = 0; i < tags.length; i++) {
			assertEquals(expectedCleanLines[i], loader.getDataPoints(measure, Optional.of(tags[i])).size(),
					tags[i].toString());
		}

		final var ranges = this.cleaner.getOmittedRanges();
		final var expectedRanges = new ArrayList<Range<Float>>(Arrays.asList(
			new Range<Float>(     0.0f, 3975.835f),
			new Range<Float>(  6747.4f, 6853.805f),
			new Range<Float>( 7080.36f, 7108.925f),
			new Range<Float>(8198.035f, 8198.295f)
		));
		assertArrayEquals(expectedRanges.toArray(), ranges.toArray(), ranges.toString());

		int omittedPointsCount = 0;
		for (final var range : ranges) {
			omittedPointsCount += this.cleaner.getOmittedPoints(range).size();
		}
		assertEquals(expectedTotalLines - expectedTotalCleanLines, omittedPointsCount);
	}

}
