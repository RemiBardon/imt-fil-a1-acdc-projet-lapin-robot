package scanner;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Scanner;

/**
 * 
 * @author Rémi BARDON
 */
public final class CSVScanner {
	
	private static String DELIMITER = "\t";
	private String headingComment;
	private Date start;
	private Date end;
	private HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> points;
	private HashMap<Range<Date>, HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>> omittedPoints;
	
	public CSVScanner() {
		this.headingComment = "";
		this.start = Date.from(Instant.now());
		this.end = Date.from(Instant.now());
		this.points = new HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>();
		this.omittedPoints = new HashMap<Range<Date>, HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>>();
	}
	
	public void load(File file) {
		Scanner scanner = null;
		try {
			// Create Scanner
			scanner = new Scanner(file);
			scanner.useDelimiter(DELIMITER);
			
			while(scanner.hasNext()) {
				System.out.print(scanner.next()+DELIMITER);
			}
		} catch (FileNotFoundException fe) {
			fe.printStackTrace();
		} finally {
			scanner.close();
		}
	}
	
	public ArrayList<Tag> getTags() {
		return new ArrayList<Tag>();
	}
	
   	public Date getStart() {
   		return this.start;
   	}
   	
   	public Date getEnd() {
   		return this.end;
   	}
	
   	public DataPoint getPoints(DataType type, Mesure mesure, Date timestamp) {
   		return new DataPoint(Date.from(Instant.now()), 0.0f);
   	}
   	
   	public HashMap<Mesure, DataPoint> getPoints(DataType type, ArrayList<Mesure> mesures, Date timestamp) {
   		return new HashMap<Mesure, DataPoint>();
   	}
   	
   	public ArrayList<DataPoint> getPoints(DataType type, Mesure mesure, Optional<Tag> tag) {
   		return new ArrayList<DataPoint>();
   	}
   	
	public HashMap<Mesure, ArrayList<DataPoint>> getPoints(DataType type, ArrayList<Mesure> mesures, Optional<Tag> tag) {
   		return new HashMap<Mesure, ArrayList<DataPoint>>();
	}
   	
   	public HashMap<DataType, DataPoint> getAllPoints(Mesure mesure, Date timestamp) {
   		return new HashMap<DataType, DataPoint>();
   	}
   	
	public HashMap<DataType, HashMap<Mesure, DataPoint>> getAllPoints(ArrayList<Mesure> mesures, Date timestamp) {
   		return new HashMap<DataType, HashMap<Mesure, DataPoint>>();
	}
   	
	public HashMap<DataType, ArrayList<DataPoint>> getAllPoints(Mesure mesure, Optional<Tag> tag) {
   		return new HashMap<DataType, ArrayList<DataPoint>>();
	}
   	
	public HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> getAllPoints(ArrayList<Mesure> mesures, Optional<Tag> tag) {
   		return new HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>();
	}
	
	public Range<Float> getValueRange(ArrayList<DataPoint> points) {
		return new Range<Float>(0.0f, 2.0f);
	}
	
	public <T> HashMap<T, Range<Float>> getValueRangeFromMap(HashMap<T, ArrayList<DataPoint>> points) {
		return new HashMap<T, Range<Float>>();
	}
	
	public <S, T> HashMap<S, HashMap<T, Range<Float>>> getValueRangeFromNestedMap(HashMap<S, HashMap<T, ArrayList<DataPoint>>> points) {
		return new HashMap<S, HashMap<T, Range<Float>>>();
	}
	
	public ArrayList<Range<Date>> getOmittedRanges() {
		return new ArrayList<Range<Date>>();
	}
	
	public HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>> getOmittedPoints(Range<Date> range) {
		return new HashMap<DataType, HashMap<Mesure, ArrayList<DataPoint>>>();
	}
	
	public String getHeadingComment() {
		return this.headingComment;
	}
	
}
