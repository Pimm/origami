package org.ilumbo.origami.drawing;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

/**
 * Draws a frame.
 */
public class FrameDrawer {
	/**
	 * The untransformed paths for the polygons. This array is a sibeling of the paints array. A paint with an index should be
	 * used with the path with that same index.
	 */
	protected final Path[] originalPaths;
	/**
	 * The paints used to draw the polygons. This array is a sibeling of the originalPaths array. A paint with an index should
	 * be used with the path with that same index.
	 */
	protected final Paint[] paints;
	/**
	 * The transformation that has been applied to the originalPaths array to generate the transformedPaths array. The right
	 * and bottom properties of this rectangle are misused to represent width and height.
	 */
	private final Rect transformation;
	/**
	 * The paths that have been transformed, and can be re-used whenever the requested transformation equals the transformation
	 * applied.
	 */
	private final Path[] transformedPaths;
	public FrameDrawer(Path[] paths, Paint[] paints) {
		final int pathCount = (this.originalPaths = paths).length;
		this.paints = paints;
		// Create the initial transformed paths, which aren't transformed at all. They are just copies.
		transformedPaths = new Path[pathCount];
		for (int index = 0; pathCount != index; index++) {
			(transformedPaths[index] = new Path())
					.set(paths[index]);
		}
		// As the initial transformed paths are just copies, the transformation is x: 0, y: 0, width: 1, height: 1.
		transformation = new Rect(0, 0, 1, 1);
	}
	/**
	 * Draws the frame to the passed canvas.
	 */
	public void draw(Canvas canvas, int x, int y, int width, int height) {
		final Path[] transformedPaths = transformOriginalPaths(x, y, width, height);
		final int pathCount = transformedPaths.length;
		for (int index = 0; pathCount != index; index++) {
			canvas.drawPath(transformedPaths[index], paints[index]);
		}
	}
	/**
	 * Returns the original paths (as passed to the constructor) transformed according to the passed arguments. The resulting
	 * array is cached, so do not alter it.
	 */
	protected final Path[] transformOriginalPaths(int x, int y, int width, int height) {
		// Check whether the previously transformed path can be re-used.
		if (transformation.right == width && transformation.bottom == height &&
				transformation.left == x && transformation.top == y) {
			return transformedPaths;
		}
		// Create the transformation matrix.
		final Matrix transformationMatrix = new Matrix();
		transformationMatrix.setValues(new float[]{
				width, 0, x,
				0, height, y,
				0, 0, 1
		});
		// Transform the paths.
		final int pathCount = originalPaths.length;
		for (int index = 0; pathCount != index; index++) {
			originalPaths[index].transform(transformationMatrix, transformedPaths[index]);
		}
		// Set the transformation rectangle, which is used at the start of this method.
		transformation.set(x, y, width, height);
		return transformedPaths;
	}
}