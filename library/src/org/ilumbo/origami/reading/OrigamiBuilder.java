package org.ilumbo.origami.reading;

public interface OrigamiBuilder<Result> {
	public interface FrameBuilder {
		public interface PolygonBuilder {
			/**
			 * Adds a "close" instruction to the polygon: a line should be drawn from the current point to the starting point
			 * of this sub-path, and a new sub-path should be initiated.
			 */
			public void addClose();
			/**
			 * Adds a "line" instruction to the polygon: a line should be drawn from the current point to the passed
			 * coordinates, which shold become the new current point. The regular coordinates (x and y) are 0…1 (inclusive),
			 * where 0 is left or top and 1 is right or bottom. The exact coordinates (exactX and exactY) are equal to the
			 * regular ones, but are 0…0xFFF.
			 */
			public void addLine(float x, float y, int exactX, int exactY);
			/**
			 * Adds a "move" instruction to the polygon: a new sub-path should be started at the passed coordinates. The
			 * regular coordinates (x and y) are 0…1 (inclusive), where 0 is left or top and 1 is right or bottom. The exact
			 * coordinates (exactX and exactY) are equal to the regular ones, but are 0…0xFFF.
			 */
			public void addMove(float x, float y, int exactX, int exactY);
			/**
			 * Completes the polygon, and perhaps adds said polygon to a list or something.
			 */
			public void build();
			/**
			 * Sets the properties of the fill. Both arguments are 0…255 (inclusive), where 0 is black or completely
			 * transparent and 255 is white or completely opaque.
			 */
			public void setFill(int lightness, int alpha);
		}
		/**
		 * Completes the frame, and perhaps adds said frame to a list or something.
		 */
		public void build();
		/**
		 * Creates a builder that will receive the data of one polygon which belongs to this frame.
		 */
		public PolygonBuilder createPolygonBuilder();
	}
	/**
	 * Completes the useful structure, and returns it.
	 */
	public Result build();
	/**
	 * Creates a builder that will receive the data of one frame.
	 */
	public FrameBuilder createFrameBuilder();
}