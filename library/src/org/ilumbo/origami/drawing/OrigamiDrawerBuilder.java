package org.ilumbo.origami.drawing;

import java.util.ArrayList;

import org.ilumbo.origami.reading.OrigamiBuilder;

import android.graphics.Paint;
import android.graphics.Path;
import android.util.SparseArray;

/**
 * Builds an origami drawer. An origami drawer is just an array of frame drawers, which draw the frames of the origami.
 */
public class OrigamiDrawerBuilder implements OrigamiBuilder<FrameDrawer[]> {
	/**
	 * Builds a frame drawer.
	 */
	protected class FrameDrawerBuilder implements FrameBuilder {
		/**
		 * Builds a {@link Path}-{@link Paint} combination.
		 */
		protected final class PathPaintBuilder implements PolygonBuilder {
			/**
			 * The paint, which is part of the result.
			 */
			private Paint paint;
			/**
			 * The path, which is part of the result.
			 */
			private final Path path;
			public PathPaintBuilder() {
				path = new Path();
			}
			@Override
			public final void addClose() {
				path.close();
			}
			@Override
			public final void addLine(float x, float y) {
				path.lineTo(x, y);
			}
			@Override
			public final void addMove(float x, float y) {
				path.moveTo(x, y);
			}
			@Override
			public final void build() {
				paintList.add(paint);
				pathList.add(path);
			}
			@Override
			public final void setFill(int lightness, int alpha) {
				paint = getSharedPaint(lightness, alpha);
			}
		}
		/**
		 * The paints that will appear in the resulting frame drawer. This list is a sibeling of the one below. A paint with an
		 * index should be used with the path with that same index.
		 */
		/* package */ final ArrayList<Paint> paintList;
		/**
		 * The paths that will appear in the resulting frame drawer. This list is a sibeling of the one above. A paint with an
		 * index should be used with the path with that same index.
		 */
		/* package */ final ArrayList<Path> pathList;
		public FrameDrawerBuilder() {
			paintList = new ArrayList<Paint>(8);
			pathList = new ArrayList<Path>(8);
		}
		@Override
		public final void build() {
			Path[] paths = new Path[pathList.size()];
			paths = pathList.toArray(paths);
			Paint[] paints = new Paint[paintList.size()];
			paints = paintList.toArray(paints);
			frameDrawerList.add(new FrameDrawer(paths, paints));
		}
		@Override
		public PolygonBuilder createPolygonBuilder() {
			return this.new PathPaintBuilder();
		}
	}
	/**
	 * The frame drawers that are the result of the building.
	 */
	/* package */ final ArrayList<FrameDrawer> frameDrawerList;
	/**
	 * The paints that have been created, where the key is (lightness << 8) | (alpha << 0).
	 */
	private final SparseArray<Paint> sharedPaints;
	public OrigamiDrawerBuilder() {
		frameDrawerList = new ArrayList<FrameDrawer>(8);
		sharedPaints = new SparseArray<Paint>(16);
	}
	@Override
	public final FrameDrawer[] build() {
		FrameDrawer[] frameDrawers = new FrameDrawer[frameDrawerList.size()];
		frameDrawers = frameDrawerList.toArray(frameDrawers);
		return frameDrawers;
	}
	@Override
	public FrameBuilder createFrameBuilder() {
		return this.new FrameDrawerBuilder();
	}
	/**
	 * Creates a paint with the passed lightness and alpha.
	 */
	protected Paint createPaint(int lightness, int alpha) {
		final Paint result = new Paint();
		result.setColor((alpha << 24) |
				(lightness << 16) |
				(lightness << 8) |
				(lightness << 0));
		return result;
	}
	/**
	 * Returns the shared paint with the passed lightness and alpha that was previously created, or creates a new paint and
	 * returns that.
	 */
	/* package */ final Paint getSharedPaint(int lightness, int alpha) {
		final int key = (lightness << 8) | (alpha << 0);
		Paint result = sharedPaints.get(key);
		if (null == result) {
			sharedPaints.put(key,
					result = createPaint(lightness, alpha));
		}
		return result;
	}
}