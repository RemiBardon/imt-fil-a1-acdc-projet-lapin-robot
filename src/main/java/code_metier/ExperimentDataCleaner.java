package code_metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ExperimentDataCleaner {

	private final Map<Range<Float>, List<DataPoint>> omittedPoints;

	public ExperimentDataCleaner() {
		this.omittedPoints = new HashMap<Range<Float>, List<DataPoint>>();
	}

	public void clean(final List<DataPoint> points, final Map<Tag, Range<Float>> phases) {
		this.omittedPoints.clear();

		final Map<Range<Float>, Tag> tagsByRanges = new HashMap<Range<Float>, Tag>();
		final Map<Float, Range<Float>> rangesByStart = new HashMap<Float, Range<Float>>();
		final Map<Float, Range<Float>> rangesByEnd = new HashMap<Float, Range<Float>>();
		for (final Map.Entry<Tag, Range<Float>> entry : phases.entrySet()) {
			final Range<Float> range = entry.getValue();
			final Tag tag = entry.getKey();
			tagsByRanges.put(range, tag);
			rangesByStart.put(range.getMinimum(), range);
			rangesByEnd.put(range.getMaximum(), range);
		}

		float overallOffset = 0.0f;
		float lastTimestamp = 0.0f;
		final LinkedList<DataPoint> omitted = new LinkedList<DataPoint>();

		// Store NaN values and offset timestamps
		Iterator<DataPoint> iterator = points.iterator();
		while (iterator.hasNext()) {
			final DataPoint point = iterator.next();
			final float actualTimestamp = point.getTimestamp();

			if (shouldRemovePoint(point)) {
				// Store omitted point
				omitted.add(point);
			} else {
				// Increment overall offset
				if (!omitted.isEmpty()) {
					final float delta = actualTimestamp - omitted.getFirst().getTimestamp();
					overallOffset -= delta;
				}

				// Offset point timestamp
				point.setTimestamp(point.getTimestamp() + overallOffset);
			}

			if (!shouldRemovePoint(point) || !iterator.hasNext()) {
				// If outside of NaN series or last line

				// Update omitted points
				if (!omitted.isEmpty()) {
					float start = omitted.getFirst().getTimestamp();
					float end = iterator.hasNext() ? lastTimestamp : actualTimestamp;

					this.omittedPoints.put(new Range<Float>(start, end), new ArrayList<DataPoint>(omitted));

					omitted.clear();
				}
			}

			// Offset phases start
			if (rangesByStart.containsKey(actualTimestamp)) {
				// If on the start of a new phase

				// If some phase is empty, remove it
				if (overallOffset != 0 && rangesByStart.containsKey(actualTimestamp + overallOffset)) {
					final Range<Float> emptyRange = rangesByStart.get(actualTimestamp + overallOffset);
					phases.remove(tagsByRanges.get(emptyRange));
					rangesByStart.remove(emptyRange.getMinimum());
					rangesByEnd.remove(emptyRange.getMaximum());

					// DEBUG
					// System.out.println("Removed range " + emptyRange + " in phase '" +
					// tagsByRanges.get(emptyRange) + "' at " + actualTimestamp + ". Conflicted with
					// " + rangesByStart.get(actualTimestamp));
				}

				// Update start
				final Range<Float> range = rangesByStart.get(actualTimestamp);
				// DEBUG
				// System.out.println("Start of phase '" + tagsByRanges.get(range) + "' at " +
				// actualTimestamp + ". Setting start to " + (actualTimestamp + overallOffset));
				rangesByStart.remove(actualTimestamp);
				range.setMinimum(actualTimestamp + overallOffset);
				rangesByStart.put(range.getMinimum(), range);
			}

			// Offset phases end
			if (rangesByEnd.containsKey(actualTimestamp)) {
				final Range<Float> range = rangesByEnd.get(actualTimestamp);
				// DEBUG
				// System.out.println("End of phase '" + tagsByRanges.get(range) + "' at " +
				// actualTimestamp + ". Setting end to " + (actualTimestamp + overallOffset));
				rangesByEnd.remove(range.getMaximum());
				range.setMaximum(actualTimestamp + overallOffset);
				rangesByEnd.put(range.getMaximum(), range);
			}

			// Update last timestamp for next read
			lastTimestamp = point.getTimestamp();
		}

		// Remove NaN values
		points.removeIf(ExperimentDataCleaner::shouldRemovePoint);
	}

	private static boolean shouldRemovePoint(final DataPoint point) {
		return point.getValue().isNaN();
	}

	public List<Range<Float>> getOmittedRanges() {
		final List<Range<Float>> omittedRanges = new ArrayList<Range<Float>>(this.omittedPoints.keySet());

		Collections.sort(omittedRanges);

		return omittedRanges;
	}

	public List<DataPoint> getOmittedPoints(final Range<Float> range) {
		if (!this.omittedPoints.containsKey(range)) {
			return new ArrayList<DataPoint>();
		}

		return this.omittedPoints.get(range);
	}

}
