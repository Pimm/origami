package org.ilumbo.origami.cleaning;

import java.util.ArrayList;

import org.ilumbo.origami.reading.OrigamiBuilder;

import android.graphics.Point;
import android.util.Log;

/**
 * Builds a cleaned origami document, one with all of the redundant instructions stripped out.
 */
public final class OrigamiCleaner implements OrigamiBuilder<String> {
	private static final class FrameCleaner implements FrameBuilder {
		private static final class PolygonCleaner implements PolygonBuilder {
			/**
			 * The string builder which will contain the XML origami document.
			 */
			private final StringBuilder cleanDocumentBuilder;
			/**
			 * The point that is last "move"d or "line"d to.
			 */
			private final Point currentPoint;
			/**
			 * The point where the current sub-path originated from.
			 */
			private final Point currentSubPathStartPoint;
			/**
			 * The string that is the value of the fill attribute.
			 */
			private String fillString;
			/**
			 * A list of the instruction elements (start and end tag and text), such as "<close/>".
			 */
			private final ArrayList<String> instructionElements;
			/**
			 * Whether the instruction element most recently added to the list above is a "close" one.
			 */
			private boolean lastInstructionElementIsClose;
			public PolygonCleaner(StringBuilder cleanDocumentBuilder) {
				(this.cleanDocumentBuilder = cleanDocumentBuilder)
						.append("<polygon ");
				instructionElements = new ArrayList<String>(4);
				lastInstructionElementIsClose = false;
				// Before any instructions are read, the current point which is the start point of the current sub-path is at
				// the top-left.
				currentPoint = new Point(0, 0);
				currentSubPathStartPoint = new Point(0, 0);
			}
			@Override
			public final void addClose() {
				if (false == addLine(currentSubPathStartPoint.x, currentSubPathStartPoint.y)) {
					Log.v(OrigamiCleaner.class.getSimpleName(), "Redundant close instruction");
				}
			}
			/**
			 * Adds a line instruction and returns true, or returns false if adding such an instruction would have no effect.
			 */
			private final boolean addLine(int exactX, int exactY) {
				// If the coordinates of the current point equal those passed, there is no need to add an instruction as such
				// an instruction would have no effect.
				if (currentPoint.equals(exactX, exactY)) {
					return false;
				}
				// If the passed coordinates equal those of the start point of the current sub-path, a close instruction will
				// suffice. Add the instruction.
				if (currentSubPathStartPoint.equals(exactX, exactY)) {
					instructionElements.add("<close/>");
					lastInstructionElementIsClose = true;
				} else {
					instructionElements.add(new StringBuilder(19)
							.append("<line>")
							.append(prependZeroesToThree(Integer.toHexString(exactX)))
							.append(prependZeroesToThree(Integer.toHexString(exactY)))
							.append("</line>")
							.toString());
					lastInstructionElementIsClose = false;
				}
				currentPoint.set(exactX, exactY);
				return true;
			}
			@Override
			public final void addLine(float x, float y, int exactX, int exactY) {
				if (false == addLine(exactX, exactY)) {
					Log.v(OrigamiCleaner.class.getSimpleName(), new StringBuilder(64)
							.append("Redundant line instruction to ")
							.append(x)
							.append(", ")
							.append(y)
							.append(" found")
							.toString());
				}
			}
			@Override
			public final void addMove(float x, float y, int exactX, int exactY) {
				// If the coordinates of the current point and those of the start point of the current sub-path equal the
				// passed ones, there is no need to add an instruction as such an instruction would have no effect.
				if (currentPoint.equals(exactX, exactY)) {
					Log.v(OrigamiCleaner.class.getSimpleName(), new StringBuilder(64)
							.append("Redundant move instruction to ")
							.append(x)
							.append(", ")
							.append(y)
							.append(" found")
							.toString());
					return;
				}
				// Add the instruction.
				instructionElements.add(new StringBuilder(19)
						.append("<move>")
						.append(prependZeroesToThree(Integer.toHexString(exactX)))
						.append(prependZeroesToThree(Integer.toHexString(exactY)))
						.append("</move>")
						.toString());
				lastInstructionElementIsClose = false;
				currentPoint.set(exactX, exactY);
				currentSubPathStartPoint.set(exactX, exactY);
			}
			@Override
			public final void build() {
				// If the last instruction is a "close" one, remove the element from the list. A polygon implicitly ends with a
				// "close" instruction.
				if (lastInstructionElementIsClose) {
					instructionElements.remove(instructionElements.size() - 1);
				}
				cleanDocumentBuilder.append("fill=\"")
						.append(fillString)
						.append("\">");
				for (final String instructionElement : instructionElements) {
					cleanDocumentBuilder.append(instructionElement);
				}
				cleanDocumentBuilder.append("</polygon>");
			}
			/**
			 * Returns a variant of the passed string that is (at least) three characters long, prepending zeroes if required.
			 */
			private static final String prependZeroesToThree(String input) {
				switch (input.length()) {
				default:
					return input;
				case 2:
					return '0' + input;
				case 1:
					return "00" + input;
				}
			}
			/**
			 * Returns a variant of the passed string that is (at least) two characters long, prepending zeroes if required.
			 */
			private static final String prependZeroesToTwo(String input) {
				if (1 == input.length()) {
					return '0' + input;
				} else {
					return input;
				}
			}
			@Override
			public final void setFill(int lightness, int alpha) {
				fillString = prependZeroesToTwo(Integer.toHexString(lightness)) +
						prependZeroesToTwo(Integer.toHexString(alpha));
			}
		}
		/**
		 * The string builder which will contain the XML origami document.
		 */
		private final StringBuilder cleanDocumentBuilder;
		public FrameCleaner(StringBuilder cleanDocumentBuilder) {
			(this.cleanDocumentBuilder = cleanDocumentBuilder)
					.append("\t<frame>");
		}
		@Override
		public final void build() {
			cleanDocumentBuilder.append("</frame>\n");
		}
		@Override
		public final PolygonBuilder createPolygonBuilder() {
			return new PolygonCleaner(cleanDocumentBuilder);
		}
	}
	/**
	 * The string builder which will contain the XML origami document.
	 */
	private final StringBuilder cleanDocumentBuilder;
	public OrigamiCleaner() {
		(cleanDocumentBuilder = new StringBuilder(128))
				.append("<origami>\n");
	}
	@Override
	public final String build() {
		return cleanDocumentBuilder
				.append("</origami>")
				.toString();
	}
	@Override
	public final FrameBuilder createFrameBuilder() {
		return new FrameCleaner(cleanDocumentBuilder);
	}
}