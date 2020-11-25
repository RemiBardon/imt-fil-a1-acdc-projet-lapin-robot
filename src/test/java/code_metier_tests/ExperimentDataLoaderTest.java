package code_metier_tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import code_metier.DataPoint;
import code_metier.ExperimentDataLoader;
import code_metier.Measure;
import code_metier.Tag;

/**
 * 
 * @author Rémi BARDON
 */
@DisplayName("Loader")
public class ExperimentDataLoaderTest {

	static Constructor<Measure> MEASURE_CONSTRUCTOR;
	static Constructor<Tag> TAG_CONSTRUCTOR;
	static Constructor<DataPoint> DATA_POINT_CONSTRUCTOR;

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
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@DisplayName("Create Default Measure Names")
	public void testDefaultMeasureNames() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/constant-no_tag-no_measures-small.txt"));

		final Measure[] measures1 = {
			MEASURE_CONSTRUCTOR.newInstance("Mesure 1"),
			MEASURE_CONSTRUCTOR.newInstance("Mesure 2"),
			MEASURE_CONSTRUCTOR.newInstance("Mesure 3"),
			MEASURE_CONSTRUCTOR.newInstance("Mesure 4"),
			MEASURE_CONSTRUCTOR.newInstance("Mesure 5"),
		};
		assertArrayEquals(measures1, loader.getMeasures().toArray());

		loader.load(new File("src/test/resources/constant-no_tag-no_measures-2_columns-small.txt"));

		final Measure[] measures2 = {
			MEASURE_CONSTRUCTOR.newInstance("Mesure 1"),
			MEASURE_CONSTRUCTOR.newInstance("Mesure 2"),
		};
		assertArrayEquals(measures2, loader.getMeasures().toArray());
	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@DisplayName("Read Accented Tags")
	public void testReadAccentedTags() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/constant-accented_tags-small.txt"));

		final Tag[] expected = {
			TAG_CONSTRUCTOR.newInstance("tag1"),
			TAG_CONSTRUCTOR.newInstance("adrénaline"),
			TAG_CONSTRUCTOR.newInstance("tag3"),
		};
		assertArrayEquals(expected, loader.getAllTags().toArray(), "Tags: " + loader.getAllTags());
	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@DisplayName("Read Points")
	public void testReadDataPoints() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/constant.txt"));

		for (final var measure : loader.getMeasures()) {
			assertEquals(30, loader.getDataPoints(measure).size(), measure.toString());
		}
	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@DisplayName("Read File Header")
	public void testReadHeader() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/constant-header.txt"));

		final var expected = "Comment 1\nComment 2\nTest\ttab\nNo leading space\n Two leading spaces";
		assertEquals(expected, loader.getHeadingComment());
	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@DisplayName("Read Measure Names")
	public void testReadMeasureNames() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/constant-header.txt"));

		final Measure[] measures = {
			MEASURE_CONSTRUCTOR.newInstance("Pression Arterielle"),
			MEASURE_CONSTRUCTOR.newInstance("Spirometrie"),
			MEASURE_CONSTRUCTOR.newInstance("PA moyenne"),
			MEASURE_CONSTRUCTOR.newInstance("Frequence Cardiaque"),
			MEASURE_CONSTRUCTOR.newInstance("Frequence Respiratoire"),
		};
		assertArrayEquals(measures, loader.getMeasures().toArray());
	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@DisplayName("Read Tags")
	public void testReadTags() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/constant.txt"));

		final Tag[] expected = {
			TAG_CONSTRUCTOR.newInstance("preparation"),
			TAG_CONSTRUCTOR.newInstance("tag1"),
			TAG_CONSTRUCTOR.newInstance("tag2"),
		};
		assertArrayEquals(expected, loader.getAllTags().toArray(), "Tags: " + loader.getAllTags());
	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@DisplayName("Get Points By Tag")
	public void testStoreDataPointsByTag() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/constant.txt"));

		final Measure measure = MEASURE_CONSTRUCTOR.newInstance("Pression Arterielle");
		final Optional<Tag> tag = Optional.of(TAG_CONSTRUCTOR.newInstance("tag1"));

		final DataPoint[] expected = {
			DATA_POINT_CONSTRUCTOR.newInstance(10f, 36f),
			DATA_POINT_CONSTRUCTOR.newInstance(11f, 36f),
			DATA_POINT_CONSTRUCTOR.newInstance(12f, 36f),
			DATA_POINT_CONSTRUCTOR.newInstance(13f, 36f),
			DATA_POINT_CONSTRUCTOR.newInstance(14f, 36f),
		};
		assertArrayEquals(expected, loader.getDataPoints(measure, tag).toArray());
	}

	/**
	 * 
	 * @throws Exception
	 * @author Rémi BARDON
	 */
	@Test
	@Slow
	@DisplayName("Read Real Data")
	public void testLoadRealData() throws Exception {
		final ExperimentDataLoader loader = new ExperimentDataLoader();
		loader.load(new File("src/test/resources/real_data-group_3.txt"));

		final Measure measure = MEASURE_CONSTRUCTOR.newInstance("Mesure 2");

		assertEquals(4, loader.getTags(measure).size(), "Tags: " + loader.getTags(measure));

		final Tag[] tags = {
			TAG_CONSTRUCTOR.newInstance("preparation"),
			TAG_CONSTRUCTOR.newInstance("ach"),
			TAG_CONSTRUCTOR.newInstance("adr�naline"), // For some reason, we can't decode "adrénaline" correctly
			TAG_CONSTRUCTOR.newInstance("ocytocine"),
		};

		assertArrayEquals(tags, loader.getTags(measure).toArray());

		final int expectedTotalLines = 820_914;
		final int[] expectedLines = { 518_601, 43_032, 48_208, 211_073 };
		assertEquals(expectedTotalLines, Arrays.stream(expectedLines).sum());
		assertEquals(expectedTotalLines, loader.getDataPoints(measure, Optional.empty()).size());
		for (int i = 0; i < tags.length; i++) {
			final int actual = loader.getDataPoints(measure, Optional.of(tags[i])).size();
			assertEquals(expectedLines[i], actual, tags[i].toString());
		}
	}

}
