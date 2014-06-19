package org.ilumbo.origami.reading;

import java.io.IOException;
import java.io.InputStream;

import org.ilumbo.origami.reading.OrigamiBuilder.FrameBuilder;
import org.ilumbo.origami.reading.OrigamiBuilder.FrameBuilder.PolygonBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Reads origami documents. This class does not create a data structure for the document. Instead, it relies on a builder to
 * turn the data into a useful format.
 *
 * An origami reader does not have any internal state that would make it unsafe to re-use when reading multiple documents. (The
 * builder, however, probably does have such internal state.)
 */
public class OrigamiReader {
	/**
	 * The instruction type for closes.
	 */
	private static final byte INSTRUCTION_TYPE_CLOSE = 2;
	/**
	 * The instruction type for lines.
	 */
	private static final byte INSTRUCTION_TYPE_LINE = 1;
	/**
	 * The instruction type for moves.
	 */
	private static final byte INSTRUCTION_TYPE_MOVE = 0;
	/**
	 * The namespace for the origami format.
	 */
	protected static final String NAMESPACE = null;
	/**
	 * Creates a new XML pull parser.
	 */
	protected static XmlPullParser createXmlPullParser() {
		final XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
		} catch (XmlPullParserException exception) {
			throw new RuntimeException(exception);
		}
		// Set the features of the resulting parser.
		try {
			factory.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
			factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		// The above might raise an exception, but this is highly unlikely. Those features should be false by default, and even
		// if that were not the case setting them to false should not be a problem. Throw a runtime exception in this
		// exceptional situation.
		} catch (XmlPullParserException exception) {
			throw new RuntimeException(exception);
		}
		try {
			return factory.newPullParser();
		} catch (XmlPullParserException exception) {
			throw new RuntimeException(exception);
		}
	}
	/**
	 * Reads an origami document from the passed input stream. The passed builder receives the data in the document, and flows
	 * it into some kind of data structure. Said "some kind of data structure" is returned.
	 */
	public Object read(InputStream inputStream, OrigamiBuilder<?> builder) {
		final XmlPullParser parser = createXmlPullParser();
		try {
			parser.setInput(inputStream, "UTF_8");
			// Read the very first element start tag.
			parser.next();
			// Read the document.
			readDocument(parser, builder);
		} catch (XmlPullParserException exception) {
			exception.printStackTrace();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		// Return the resulting data structure.
		return builder.build();
	}
	/**
	 * Reads the document from a parser which has just found an "origami" element start tag, and pushes the data to passed
	 * builder.
	 */
	protected void readDocument(XmlPullParser parser, OrigamiBuilder<?> builder) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, NAMESPACE, "origami");
		// Read the frames.
		while (XmlPullParser.END_TAG != parser.next()) {
			if (XmlPullParser.START_TAG != parser.getEventType()) {
				continue;
			}
			final FrameBuilder frameBuilder = builder.createFrameBuilder();
			readFrame(parser, frameBuilder);
			frameBuilder.build();
		}
		parser.require(XmlPullParser.END_TAG, NAMESPACE, "origami");
	}
	/**
	 * Reads the frame from a parser which has just found a "frame" element start tag, and pushes the data to passed builder.
	 */
	protected void readFrame(XmlPullParser parser, FrameBuilder builder) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, NAMESPACE, "frame");
		// Read the polygons.
		while (XmlPullParser.END_TAG != parser.next()) {
			if (XmlPullParser.START_TAG != parser.getEventType()) {
				continue;
			}
			final PolygonBuilder polygonBuilder = builder.createPolygonBuilder();
			readPolygon(parser, polygonBuilder);
			polygonBuilder.build();
		}
		parser.require(XmlPullParser.END_TAG, NAMESPACE, "frame");
	}
	/**
	 * Reads the polygon from a parser which has just found a "polygon" element start tag, and pushes the data to passed
	 * builder.
	 */
	protected void readPolygon(XmlPullParser parser, PolygonBuilder builder) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, NAMESPACE, "polygon");
		// The polygon element should have a "fill" attribute which describes the fill. Parse it. Use it.
		{
			final String fillString = parser.getAttributeValue(NAMESPACE, "fill");
			final int fillInteger;
			try {
				fillInteger = Integer.parseInt(fillString, 0x10);
			} catch (NumberFormatException exception) {
				throw new XmlPullParserException("A hexadecimal integer is expected, but not found", parser, null);
			}
			// Separate the two 8-bit channels, which are joined together in the integer above, and pass them to the builder.
			builder.setFill((fillInteger >>> 8) & 0xFF,
					(fillInteger >>> 0) & 0xFF);
		}
		// Read the contents of the polygon.
		{
			byte currentlyReadingInstructionType = Byte.MIN_VALUE;
			int currentlyReadingInstructionExactX = Integer.MIN_VALUE;
			int currentlyReadingInstructionExactY = Integer.MIN_VALUE;
			while (XmlPullParser.END_TAG != parser.next() || Byte.MIN_VALUE != currentlyReadingInstructionType) {
				switch (parser.getEventType()) {
				case XmlPullParser.START_TAG:
				{
					// If an element start tag was found while an instruction was being read, throw an exception.
					if (Byte.MIN_VALUE != currentlyReadingInstructionType) {
						throw new XmlPullParserException("No element start tag is expected", parser, null);
					}
					// Determine what type of instruction is going to be read.
					final String elementName = parser.getName();
					if ("move".equals(elementName)) {
						currentlyReadingInstructionType = INSTRUCTION_TYPE_MOVE;
					} else if ("line".equals(elementName)) {
						currentlyReadingInstructionType = INSTRUCTION_TYPE_LINE;
					} else if ("close".equals(elementName)) {
						currentlyReadingInstructionType = INSTRUCTION_TYPE_CLOSE;
					} else {
						throw new XmlPullParserException("Unexpected element name", parser, null);
					}
					break;
				}
				case XmlPullParser.TEXT:
				{
					// If text was found while no instruction was being read, or the instruction that was being read is a
					// "close" one, throw an exception.
					if (Byte.MIN_VALUE == currentlyReadingInstructionType || INSTRUCTION_TYPE_CLOSE == currentlyReadingInstructionType) {
						throw new XmlPullParserException("No text is expected", parser, null);
					}
					// The text should be coordinates for the "move" or "line" instruction. Parse 'em.
					final int coordinates;
					try {
						coordinates = Integer.parseInt(parser.getText(), 0x10);
					} catch (NumberFormatException exception) {
						throw new XmlPullParserException("A hexadecimal integer is expected, but not found", parser, null);
					}
					// Separate the two royal 11-bit coordinates, which are joined together in the integer above.
					currentlyReadingInstructionExactX = (coordinates >>> 12) & 0xFFF;
					if ((currentlyReadingInstructionExactY = (coordinates >>> 0) & 0xFFF) > 0x800 ||
							currentlyReadingInstructionExactX > 0x800) {
						throw new XmlPullParserException("A hexadecimal integer has an unexpected value", parser, null);
					}
					break;
				}
				case XmlPullParser.END_TAG:
				{
					// If an element end tag was found while no instruction was being read, throw an exception.
					if (Byte.MIN_VALUE == currentlyReadingInstructionType) {
						throw new XmlPullParserException("No element end tag is expected", parser, null);
					}
					// Add the instruction to the polygon builder.
					switch (currentlyReadingInstructionType) {
					case INSTRUCTION_TYPE_MOVE:
						parser.require(XmlPullParser.END_TAG, NAMESPACE, "move");
						builder.addMove(currentlyReadingInstructionExactX / 2048f,
								currentlyReadingInstructionExactY / 2048f,
								currentlyReadingInstructionExactX, currentlyReadingInstructionExactY);
						break;
					case INSTRUCTION_TYPE_LINE:
						parser.require(XmlPullParser.END_TAG, NAMESPACE, "line");
						builder.addLine(currentlyReadingInstructionExactX / 2048f,
								currentlyReadingInstructionExactY / 2048f,
								currentlyReadingInstructionExactX, currentlyReadingInstructionExactY);
						break;
					case INSTRUCTION_TYPE_CLOSE:
						parser.require(XmlPullParser.END_TAG, NAMESPACE, "close");
						builder.addClose();
						break;
					}
					// Reset the currently reading instruction type.
					currentlyReadingInstructionType = Byte.MIN_VALUE;
					break;
				}
				default:
					throw new XmlPullParserException("Element start or end tag, or text expected", parser, null);
				}
			}
		}
		// (A polygon implicitly ends with a "close" instruction.)
		builder.addClose();
		parser.require(XmlPullParser.END_TAG, NAMESPACE, "polygon");
	}
}