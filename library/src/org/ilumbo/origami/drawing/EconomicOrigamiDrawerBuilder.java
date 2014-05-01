package org.ilumbo.origami.drawing;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Similar to {@link OrigamiDrawerBuilder}, but tries to use as few paths as possible internally. This typically reduces the
 * number of times a path has to be drawn, but increases the boundries of those paths.
 */
public class EconomicOrigamiDrawerBuilder extends OrigamiDrawerBuilder {
	protected class EconomicFrameDrawerBuilder extends FrameDrawerBuilder {
		protected class ReusedPathPaintBuilder implements PolygonBuilder {
			/**
			 * The path-paint builder this wrapper wraps around. Said builder has a re-used path and paint.
			 */
			private final PathPaintBuilder wrapee;
			public ReusedPathPaintBuilder(Path path, Paint paint) {
				(wrapee = new PathPaintBuilder(path))
						.setFill(paint);
			}
			@Override
			public final void addClose() {
				wrapee.addClose();
			}
			@Override
			public final void addLine(float x, float y, int exactX, int exactY) {
				wrapee.addLine(x, y, exactX, exactY);
			}
			@Override
			public final void addMove(float x, float y, int exactX, int exactY) {
				wrapee.addMove(x, y, exactX, exactY);
			}
			@Override
			public final void build() {
				// The path and paint have already been added to the lists.
			}
			@Override
			public final void setFill(int lightness, int alpha) {
				throw new UnsupportedOperationException();
			}
		}
		protected class PathPaintBuilderWrapper implements PolygonBuilder {
			/**
			 * The polygon builder this wrapper wraps around. This is determined in the setFill method.
			 */
			private PolygonBuilder wrapee;
			@Override
			public final void addClose() {
				wrapee.addClose();
			}
			@Override
			public final void addLine(float x, float y, int exactX, int exactY) {
				wrapee.addLine(x, y, exactX, exactY);
			}
			@Override
			public final void addMove(float x, float y, int exactX, int exactY) {
				wrapee.addMove(x, y, exactX, exactY);
			}
			@Override
			public final void build() {
				wrapee.build();
			}
			@Override
			public final void setFill(int lightness, int alpha) {
				// Grab the paint that the wrapee should include.
				final Paint paint = getSharedPaint(lightness, alpha);
				// Check whether the frame drawer builder already used this paint.
				final int paintCount = paintList.size();
				for (int index = 0; paintCount != index; index++) {
					if (paint == paintList.get(index)) {
						wrapee = new ReusedPathPaintBuilder(pathList.get(index), paintList.get(index));
						return;
					}
				}
				// If no polygon builder was found with the paint, create it and wrap it and forward the setFill call to it
				// directly.
				final PathPaintBuilder newPolygonBuider = EconomicFrameDrawerBuilder.this.new PathPaintBuilder();
				wrapee = newPolygonBuider;
				newPolygonBuider.setFill(paint);
			}
		}
		@Override
		public PolygonBuilder createPolygonBuilder() {
			return this.new PathPaintBuilderWrapper();
		}
	}
	@Override
	public FrameBuilder createFrameBuilder() {
		return this.new EconomicFrameDrawerBuilder();
	}
}