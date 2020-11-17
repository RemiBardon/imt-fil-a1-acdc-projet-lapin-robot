import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;

import com.opencsv.exceptions.CsvValidationException;

import code_metier.DataType;
import code_metier.ExperimentPhase;
import code_metier.Mesure;
import code_metier.RabitDataLoader;
import code_metier.Range;
import code_metier.Tag;

public class RabitDataLoaderTest {

	static Constructor<Tag> TAG_CONSTRUCTOR;
	static Constructor<ExperimentPhase> PHASE_CONSTRUCTOR;

	/**
	 * Gets a reference to {@link Tag} package-visible constructor using reflection.
	 * For more information, see <a href="https://stackoverflow.com/a/14077876/10967642">How to test a private constructor in Java application?</a>
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	@BeforeClass
	public static void getTagConstructor() throws NoSuchMethodException, SecurityException {
		TAG_CONSTRUCTOR = Tag.class.getDeclaredConstructor(String.class);
		TAG_CONSTRUCTOR.setAccessible(true);

		PHASE_CONSTRUCTOR = ExperimentPhase.class.getDeclaredConstructor(Tag.class, float.class, float.class);
		PHASE_CONSTRUCTOR.setAccessible(true);
	}

	@Test
	public void testLoad() throws CsvValidationException, IOException, ParseException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final RabitDataLoader loader = new RabitDataLoader();
		loader.load(new File("src/test/resources/real_data-group_3.txt"));

		final Mesure mesure = Mesure.SPIROMETRIE;

		assertEquals("Tags: " + loader.getTags(mesure), 4, loader.getTags(mesure).size());

		final Tag[] tags = {
			TAG_CONSTRUCTOR.newInstance("preparation"),
			TAG_CONSTRUCTOR.newInstance("ach"),
			TAG_CONSTRUCTOR.newInstance("adr�naline"), // For some reason, we can't decode "adrénaline" correctly
			TAG_CONSTRUCTOR.newInstance("ocytocine")
		};

		assertArrayEquals(tags, loader.getTags(mesure).toArray());

		final int expectedTotalLines = 820_914;
		final int[] expectedLines = { 518_601, 43_032, 48_208, 211_073 };
		assertEquals(expectedTotalLines, Arrays.stream(expectedLines).sum());
		assertEquals(expectedTotalLines, loader.getPoints(DataType.RAW, mesure, Optional.empty()).size());
		for (int i = 0; i < tags.length; i++) {
			assertEquals(tags[i].toString(), expectedLines[i], loader.getPoints(DataType.RAW, mesure, Optional.of(tags[i])).size());
		}

		loader.cleanData();

		final int expectedTotalCleanLines = 817_443;
		final int[] expectedCleanLines = { 515_381, 42_990, 48_052, 211_020 };
		//assertEquals(expectedTotalCleanLines, Arrays.stream(expectedCleanLines).sum());
		assertEquals(expectedTotalCleanLines, loader.getPoints(DataType.RAW, mesure, Optional.empty()).size());
		for (int i = 0; i < tags.length; i++) {
			assertEquals(tags[i].toString(), expectedCleanLines[i], loader.getPoints(DataType.RAW, mesure, Optional.of(tags[i])).size());
		}

		final List<Range<Float>> ranges = loader.getOmittedRanges(mesure);
		final List<Range<Float>> expectedRanges = new ArrayList<Range<Float>>(Arrays.asList(
			new Range<Float>(     0.0f, 3975.835f),
			new Range<Float>(  6747.4f, 6853.805f),
			new Range<Float>( 7080.36f, 7108.925f),
			new Range<Float>(8198.035f, 8198.295f)
		));
		assertArrayEquals(ranges.toString(), expectedRanges.toArray(), ranges.toArray());

		int omittedPointsCount = 0;
		for (Range<Float> range: ranges) {
			omittedPointsCount += loader.getOmittedPoints(mesure, range).size();
		}
		assertEquals(expectedTotalLines - expectedTotalCleanLines, omittedPointsCount);
	}

	@Test
	public void testReadHeader() throws CsvValidationException, IOException, ParseException {
		final RabitDataLoader loader = new RabitDataLoader();
		loader.load(new File("src/test/resources/constant.txt"));

		assertEquals("Temps\tPression Arterielle\tSpirometrie\tPA moyenne\tFrequence Cardiaque\tFrequence Respiratoire\tTag", loader.getHeadingComment());
	}

	@Test
	public void testReadLines() throws CsvValidationException, IOException, ParseException {
		final RabitDataLoader loader = new RabitDataLoader();
		loader.load(new File("src/test/resources/constant.txt"));

		final int size = loader.getPoints(DataType.RAW, Mesure.PRESSION_ARTERIELLE, Optional.empty()).size();
		assertEquals(30, size);
	}

	@Test
	public void testReadTags() throws CsvValidationException, IOException, ParseException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final RabitDataLoader loader = new RabitDataLoader();
		loader.load(new File("src/test/resources/constant.txt"));

		final Mesure mesure = Mesure.PRESSION_ARTERIELLE;

		assertEquals("Tags: " + loader.getTags(mesure), 3, loader.getTags(mesure).size());

		final Tag[] expected = {
			TAG_CONSTRUCTOR.newInstance("preparation"),
			TAG_CONSTRUCTOR.newInstance("tag1"),
			TAG_CONSTRUCTOR.newInstance("tag2")
		};
		assertArrayEquals(expected, loader.getTags(mesure).toArray());
	}

	@Test
	public void testReadAccentedTags() throws CsvValidationException, IOException, ParseException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final RabitDataLoader loader = new RabitDataLoader();
		loader.load(new File("src/test/resources/constant-accented_tags-small.txt"));

		final Mesure mesure = Mesure.PRESSION_ARTERIELLE;

		assertEquals("Tags: " + loader.getTags(mesure), 3, loader.getTags(mesure).size());

		final Tag[] expected = {
			TAG_CONSTRUCTOR.newInstance("tag1"),
			TAG_CONSTRUCTOR.newInstance("adrénaline"),
			TAG_CONSTRUCTOR.newInstance("tag3")
		};
		assertArrayEquals(expected, loader.getTags(mesure).toArray());
	}

	@Test
	public void testSkipBadValues() throws CsvValidationException, IOException, ParseException {
		final RabitDataLoader loader = new RabitDataLoader();
		loader.load(new File("src/test/resources/test_data-with_nans.txt"));
		loader.cleanData();

		for (Mesure mesure: Mesure.values()) {
			assertEquals(mesure.toString(), 6, loader.getPoints(DataType.RAW, mesure, Optional.empty()).size());
		}

		final List<Range<Float>> pressionArterielle = new ArrayList<Range<Float>>(Arrays.asList(
			new Range<Float>(0.0f, 	0.0f),
			new Range<Float>(2.0f, 	4.0f),
			new Range<Float>(10.0f, 10.0f)
		));
		final List<Range<Float>> spirometrie = new ArrayList<Range<Float>>(Arrays.asList(
			new Range<Float>(0.0f, 	0.0f),
			new Range<Float>(3.0f, 	5.0f),
			new Range<Float>(10.0f, 10.0f)
		));
		final List<Range<Float>> paMoyenne = new ArrayList<Range<Float>>(Arrays.asList(
			new Range<Float>(0.0f, 	0.0f),
			new Range<Float>(4.0f, 	6.0f),
			new Range<Float>(10.0f, 10.0f)
		));
		final List<Range<Float>> freqCard = new ArrayList<Range<Float>>(Arrays.asList(
			new Range<Float>(0.0f, 	0.0f),
			new Range<Float>(5.0f, 	7.0f),
			new Range<Float>(10.0f, 10.0f)
		));
		final List<Range<Float>> freqRespi = new ArrayList<Range<Float>>(Arrays.asList(
			new Range<Float>(0.0f, 	0.0f),
			new Range<Float>(6.0f, 	8.0f),
			new Range<Float>(10.0f, 10.0f)
		));
 
		assertArrayEquals(pressionArterielle.toArray(), loader.getOmittedRanges(Mesure.PRESSION_ARTERIELLE).toArray());
		assertArrayEquals(spirometrie.toArray(), 		loader.getOmittedRanges(Mesure.SPIROMETRIE).toArray());
		assertArrayEquals(paMoyenne.toArray(), 			loader.getOmittedRanges(Mesure.PA_MOYENNE).toArray());
		assertArrayEquals(freqCard.toArray(), 			loader.getOmittedRanges(Mesure.FREQUENCE_CARDIAQUE).toArray());
		assertArrayEquals(freqRespi.toArray(), 			loader.getOmittedRanges(Mesure.FREQUENCE_RESPIRATOIRE).toArray());
	}

	@Test
	public void testPhasesOffset() throws CsvValidationException, IOException, ParseException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final RabitDataLoader loader = new RabitDataLoader();
		loader.load(new File("src/test/resources/test_data-with_nans_tags.txt"));

		final List<ExperimentPhase> phases = new ArrayList<ExperimentPhase>(Arrays.asList(
			PHASE_CONSTRUCTOR.newInstance(TAG_CONSTRUCTOR.newInstance("preparation"), 0.0f, 1.0f),
			PHASE_CONSTRUCTOR.newInstance(TAG_CONSTRUCTOR.newInstance("tag1"), 2.0f, 4.0f),
			PHASE_CONSTRUCTOR.newInstance(TAG_CONSTRUCTOR.newInstance("tag2"), 5.0f, 8.0f),
			PHASE_CONSTRUCTOR.newInstance(TAG_CONSTRUCTOR.newInstance("tag3"), 9.0f, 10.0f)
		));
		assertArrayEquals(phases.toArray(), loader.getPhases(Mesure.PRESSION_ARTERIELLE).toArray());

		loader.cleanData();

		assertEquals(4, loader.getPoints(DataType.RAW, Mesure.PRESSION_ARTERIELLE, Optional.empty()).size());

		final List<ExperimentPhase> cleanPhases = new ArrayList<ExperimentPhase>(Arrays.asList(
			PHASE_CONSTRUCTOR.newInstance(TAG_CONSTRUCTOR.newInstance("preparation"), 0.0f, 0.0f),
			PHASE_CONSTRUCTOR.newInstance(TAG_CONSTRUCTOR.newInstance("tag2"), 1.0f, 2.0f),
			PHASE_CONSTRUCTOR.newInstance(TAG_CONSTRUCTOR.newInstance("tag3"), 3.0f, 3.0f)
		));
		assertArrayEquals(cleanPhases.toArray(), loader.getPhases(Mesure.PRESSION_ARTERIELLE).toArray());

		assertEquals(
			Tag.PREPARATION.toString(),
			1,
			loader.getPoints(DataType.RAW, Mesure.PRESSION_ARTERIELLE, Optional.of(Tag.PREPARATION)).size()
		);
		assertEquals(
			TAG_CONSTRUCTOR.newInstance("tag1").toString(),
			0,
			loader.getPoints(DataType.RAW, Mesure.PRESSION_ARTERIELLE, Optional.of(TAG_CONSTRUCTOR.newInstance("tag1"))).size()
		);
		assertEquals(
			TAG_CONSTRUCTOR.newInstance("tag2").toString(),
			2,
			loader.getPoints(DataType.RAW, Mesure.PRESSION_ARTERIELLE, Optional.of(TAG_CONSTRUCTOR.newInstance("tag2"))).size()
		);
	}

}
