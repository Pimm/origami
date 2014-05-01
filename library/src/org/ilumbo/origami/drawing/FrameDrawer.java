package org.ilumbo.origami.drawing;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Draws a frame.
 */
public class FrameDrawer {
	/**
	 * The paints used to draw the polygons. This array is a sibeling of the originalPaths array. A paint with an index should
	 * be used with the path with that same index.
	 */
	protected final Paint[] paints;
	/**
	 * The paths for the polygons. This array is a sibeling of the paints array. A paint with an index should be used with the
	 * path with that same index.
	 */
	protected final Path[] paths;
	public FrameDrawer(Path[] paths, Paint[] paints) {
		this.paths = paths;
		this.paints = paints;
	}
	/**
	 * Draws the frame to the passed canvas. It is not uncommon to call {@link Canvas#translate(float, float)} before calling
	 * this.
	 */
	public void draw(Canvas canvas) {
		final int pathCount = paths.length;
		for (int index = 0; pathCount != index; index++) {
			canvas.drawPath(paths[index], paints[index]);
		}
	}
	/**
	 * Creates and returns a frame drawer that draws polygons transformed according to the passed properties.
	 */
	public FrameDrawer transform(float x, float y, float width, float height) {
		// Create the transformation matrix.
		final Matrix transformationMatrix = new Matrix();
		transformationMatrix.setValues(new float[]{
				width, 0, x,
				0, height, y,
				0, 0, 1
		});
		// Transform the paths.
		final int pathCount = paths.length;
		final Path[] transformedPaths = new Path[pathCount];
		for (int index = 0; pathCount != index; index++) {
			paths[index].transform(transformationMatrix,
					transformedPaths[index] = new Path());
		}
		// Return the new frame drawer with the transformed paths. It shared the paints array. Be careful!
		return new FrameDrawer(transformedPaths, paints);
	}
}