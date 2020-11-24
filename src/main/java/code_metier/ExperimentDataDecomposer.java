package code_metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.servicenow.ds.stats.stl.SeasonalTrendLoess;

public class ExperimentDataDecomposer {

	private static double[] dataPointsValues(final List<DataPoint> points) {
		final double[] result = new double[points.size()];
		final Iterator<DataPoint> iterator = points.iterator();

		for (int i = 0; i < result.length; i++) {
			result[i] = iterator.next().getValue();
		}

		return result;
	}

	private Map<DataType, List<DataPoint>> points;

	public ExperimentDataDecomposer() {
		this.points = new HashMap<DataType, List<DataPoint>>();
	}

	public void decompose(final List<DataPoint> points, final int period) {
		this.points.clear();

		final var seasonnality = new ArrayList<DataPoint>();
		final var trend = new ArrayList<DataPoint>();
		final var noise = new ArrayList<DataPoint>();

		this.points.put(DataType.RAW, points);
		this.points.put(DataType.SEASONNALITY, seasonnality);
		this.points.put(DataType.TREND, trend);
		this.points.put(DataType.NOISE, noise);

		// Data series must be at least 2 * periodicity in length
		// https://github.com/ServiceNow/stl-decomp-4j/blob/62937cb089e13d8194f2b13fe28b86ce43315ee8/stl-decomp-4j/src/main/java/com/github/servicenow/ds/stats/stl/SeasonalTrendLoess.java#L351
		if (points.size() < 2 * period) {
			return;
		}

		final double[] values = dataPointsValues(points);

		final var builder = new SeasonalTrendLoess.Builder();
		final var smoother = builder.setPeriodLength(period).setPeriodic().setRobust() // Expecting outliers
				.buildSmoother(values);

		final var stl = smoother.decompose();

		// Get stl decomposed values
		final double[] stlSeasonal = stl.getSeasonal();
		final double[] stlTrend = stl.getTrend();
		final double[] stlResidual = stl.getResidual();

		final int valueCount = points.size();
		final Iterator<DataPoint> iterator = points.iterator();

		for (int i = 0; i < valueCount; i++) {
			final float timestamp = iterator.next().getTimestamp();
			seasonnality.add(new DataPoint(timestamp, (float) stlSeasonal[i]));
			trend.add(new DataPoint(timestamp, (float) stlTrend[i]));
			noise.add(new DataPoint(timestamp, (float) stlResidual[i]));
		}
	}

	public List<DataPoint> getPoints(final DataType type) {
		return this.points.get(type);
	}

}