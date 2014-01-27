package sc.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class ChessFontUtil {

	/**
	 * Draws the chess piece as the font would draw the piece, except with an
	 * outline of the opposite color around it. This improves visibility on both
	 * white and black squares.
	 * 
	 * @param f
	 * @param piece
	 * @return
	 */
	public static Image getPieceImage(Font font, String text, Color whiteColor,
			Color blackColor, boolean isWhitePiece, int squareSize) {
		if (squareSize == 0) {
			squareSize = 10;
		}

		Shape outLine = getOutline(font, text);
		Shape strokedGlyph;
		Shape[] mask;

		// width of surrounding (white) stroke
		int strokeWidth = (int) Math.round(font.getSize() / 24.0f);
		Stroke outlineStroke = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		if (strokeWidth > 0) {
			strokedGlyph = outlineStroke.createStrokedShape(outLine);
			mask = getMask(strokedGlyph, true);
		} else {
			mask = getMask(strokedGlyph = outLine, true);
		}
		Rectangle glyphBounds = outLine.getBounds();
		
		BufferedImage bimg = new BufferedImage(squareSize, squareSize,
				BufferedImage.TYPE_4BYTE_ABGR);

		Graphics2D g = (Graphics2D) bimg.getGraphics();
		setRenderingHints(g);
		int xoff = glyphBounds.x - strokeWidth;
		int yoff = glyphBounds.y - strokeWidth;
//		g.translate(-xoff, -yoff);
//		g.setPaint(isWhitePiece ? whiteColor : blackColor);
//		fill(g, mask);
		
		g.setFont(font);
		TextLayout layout = new TextLayout(text, font, g.getFontRenderContext());
		Rectangle2D txtBounds = layout.getBounds();
		
		
		float stringX = (float) ((float) ((squareSize - txtBounds.getWidth())/2) - txtBounds.getX());
		float stringY = (float) ((float) (2*(squareSize - txtBounds.getHeight())/3) - txtBounds.getY());
		g.translate(stringX, stringY);
		
		Stroke savedStroke = g.getStroke();
		g.setStroke(outlineStroke);
		g.setPaint(isWhitePiece ? blackColor : whiteColor);
		g.draw(strokedGlyph);
		
		g.setStroke(savedStroke);
		g.setPaint(isWhitePiece ? whiteColor : blackColor);
		layout.draw(g, 0, 0);

		
		return bimg;
	}

	public static void fill(Graphics2D g, Shape[] shapes) {
		for (int i = 0; i < shapes.length; i++)
			g.fill(shapes[i]);
	}

	private static Shape getOutline(Font fnt, String text) {
		FontRenderContext frc = new FontRenderContext(null, true, true);
		GlyphVector gv = fnt.createGlyphVector(frc, text);
		return gv.getOutline();
	}

	/**
	 * set optimal rendering
	 */
	public static void setRenderingHints(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
				RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_DITHERING,
				RenderingHints.VALUE_DITHER_ENABLE);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_DEFAULT);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public static Shape[] getMask(Shape shape, boolean outline) {
		Vector result = new Vector();

		float[] coords = new float[6];
		PathIterator pi = shape.getPathIterator(null);
		GeneralPath current = new GeneralPath(pi.getWindingRule());
		while (!pi.isDone()) {
			int type = pi.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_CLOSE:
				current.closePath();
				result.add(current);
				current = new GeneralPath(pi.getWindingRule());
				break;
			case PathIterator.SEG_CUBICTO:
				current.curveTo(coords[0], coords[1], coords[2], coords[3],
						coords[4], coords[5]);
				break;
			case PathIterator.SEG_LINETO:
				current.lineTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_MOVETO:
				current.moveTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_QUADTO:
				current.quadTo(coords[0], coords[1], coords[2], coords[3]);
				break;
			}
			pi.next();
		}

		if (current.getCurrentPoint() != null)
			result.add(current); // remaining, unclosed shape

		if (outline)
			for (int i = result.size() - 1; i >= 1; i--) {
				Shape a = (Shape) result.get(i);
				for (int j = 0; j < i; j++) {
					Shape b = (Shape) result.get(j);
					if (contains(b, a)) {
						result.remove(i);
						break;
					}
				}
			}

		Shape[] shapes = new Shape[result.size()];
		result.toArray(shapes);
		return shapes;
	}

	public static boolean contains(Shape a, Shape b) {
		return a.getBounds2D().contains(b.getBounds2D());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//
		throw new RuntimeException("Unimplemented method.");
	}

}
