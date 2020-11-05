package scanner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

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
	
	private final void reset() {
		this.points = emptyPoints();
		this.phases.clear();
		this.phasesMap.clear();
		this.omittedPoints.clear();
	}
	
	private static final HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> emptyPoints() {
		HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> empty = new HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>();
		
		empty.put(DataType.RAW, 			emptyMesures());
		empty.put(DataType.TREND, 			emptyMesures());
		empty.put(DataType.SEASONNALITY, 	emptyMesures());
		empty.put(DataType.NOISE, 			emptyMesures());
		
		return empty;
	}
	
	private static final HashMap<Mesure, ArrayList<DataPoint>> emptyMesures() {
		HashMap<Mesure, ArrayList<DataPoint>> empty = new HashMap<Mesure, ArrayList<DataPoint>>();
		
		empty.put(Mesure.PRESSION_ARTERIELLE, 		new ArrayList<DataPoint>());
		empty.put(Mesure.SPIROMETRIE, 				new ArrayList<DataPoint>());
		empty.put(Mesure.PA_MOYENNE, 				new ArrayList<DataPoint>());
		empty.put(Mesure.FREQUENCE_CARDIAQUE, 		new ArrayList<DataPoint>());
		empty.put(Mesure.FREQUENCE_RESPIRATOIRE, 	new ArrayList<DataPoint>());
		
		return empty;
	}
	
	public final void load(File file) {
		this.reset();
		
		CSVReader csvReader = null;
        
        try {
        	// Create CSV Reader
        	FileReader fileReader = new FileReader(file);
        	CSVParser parser = new CSVParserBuilder().withSeparator(DELIMITER).build();
        	csvReader = new CSVReaderBuilder(fileReader).withCSVParser(parser).build();
        	
        	// Get reference to raw points
            HashMap<Mesure, ArrayList<DataPoint>> raw = this.points.get(DataType.RAW);
            
            // Read CSV and parse data
            String[] split = null;
            Tag preparationTag = Tag.preparation();
            ExperimentPhase actualPhase = new ExperimentPhase(preparationTag, 0.0f, 0.0f);
    		this.phases.add(actualPhase);
    		this.phasesMap.put(preparationTag, actualPhase);
            Float lastTimestamp = 0.0f;
            while((split = csvReader.readNext()) != null) {
            	try {
            		Float timestamp = Float.parseFloat(split[0]);
                	
                	raw.get(Mesure.PRESSION_ARTERIELLE)		.add(new DataPoint(timestamp, Float.parseFloat(split[1])));
                	raw.get(Mesure.SPIROMETRIE)				.add(new DataPoint(timestamp, Float.parseFloat(split[2])));
                	raw.get(Mesure.PA_MOYENNE)				.add(new DataPoint(timestamp, Float.parseFloat(split[3])));
                	raw.get(Mesure.FREQUENCE_CARDIAQUE)		.add(new DataPoint(timestamp, Float.parseFloat(split[4])));
                	raw.get(Mesure.FREQUENCE_RESPIRATOIRE)	.add(new DataPoint(timestamp, Float.parseFloat(split[5])));
                	
                	if (!split[6].isEmpty()) {
                		actualPhase.end = lastTimestamp;
                		
                		Tag newTag = new Tag(split[6]);
                		ExperimentPhase newPhase = new ExperimentPhase(newTag, timestamp, timestamp);
                		this.phases.add(newPhase);
                		this.phasesMap.put(newTag, newPhase);
                		
                		actualPhase = newPhase;
                	}
                	
                	lastTimestamp = timestamp;
            	} catch(NumberFormatException e) {
            		headingComment += String.join(" ", split);
            	}
            }
        } catch(IOException e) {
            e.printStackTrace();
        } catch(CsvValidationException e) {
            e.printStackTrace();
        } finally {
        	try {
				csvReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	public final ArrayList<Tag> getTags() {
		ArrayList<Tag> tags = new ArrayList<Tag>();
		
		for (ExperimentPhase phase: this.phases) {
			tags.add(phase.tag);
		}
		
		return tags;
	}
	
   	public final DataPoint getPoints(DataType type, Mesure mesure, Date timestamp) {
   		return new DataPoint(0.0f, 0.0f);
   	}
   	
   	public final HashMap<Mesure, DataPoint> getPoints(DataType type, ArrayList<Mesure> mesures, Date timestamp) {
   		return new HashMap<Mesure, DataPoint>();
   	}
   	
   	public final ArrayList<DataPoint> getPoints(DataType type, Mesure mesure, Optional<Tag> tag) {
   		return new ArrayList<DataPoint>();
   	}
   	
	public final HashMap<Mesure, ArrayList<DataPoint>> getPoints(DataType type, ArrayList<Mesure> mesures, Optional<Tag> tag) {
   		return new HashMap<Mesure, ArrayList<DataPoint>>();
	}
   	
   	public final HashMap<DataType, DataPoint> getAllPoints(Mesure mesure, Date timestamp) {
   		return new HashMap<DataType, DataPoint>();
   	}
   	
	public final HashMap<DataType, HashMap<Mesure, DataPoint>> getAllPoints(ArrayList<Mesure> mesures, Date timestamp) {
   		return new HashMap<DataType, HashMap<Mesure, DataPoint>>();
	}
   	
	public final HashMap<DataType, ArrayList<DataPoint>> getAllPoints(Mesure mesure, Optional<Tag> tag) {
   		return new HashMap<DataType, ArrayList<DataPoint>>();
	}
   	
	public final HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> getAllPoints(ArrayList<Mesure> mesures, Optional<Tag> tag) {
   		return new HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>();
	}
	
	public final Range<Float> getValueRange(ArrayList<DataPoint> points) {
		return new Range<Float>(0.0f, 2.0f);
	}
	
	public final <T> HashMap<T, Range<Float>> getValueRangeFromMap(HashMap<T, ArrayList<DataPoint>> points) {
		return new HashMap<T, Range<Float>>();
	}
	
	public final <S, T> HashMap<S, HashMap<T, Range<Float>>> getValueRangeFromNestedMap(HashMap<S, HashMap<T, ArrayList<DataPoint>>> points) {
		return new HashMap<S, HashMap<T, Range<Float>>>();
	}
	
	public final ArrayList<Range<Float>> getOmittedRanges() {
		return new ArrayList<Range<Float>>();
	}
	
	public final HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> getOmittedPoints(Range<Float> range) {
		return new HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>();
	}
	
	public final String getHeadingComment() {
		return this.headingComment;
	}
	
}
