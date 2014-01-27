package sc.gui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import sc.SPCBoard;
import sc.encodings.Encodings;

public class ChessBoardPanel extends JPanel {

	private static Color DARK_SQ_COLOR = new Color(0x50, 0x76, 0x42);
	private static Color LIGHT_SQ_COLOR = new Color(0xCC, 0xCC, 0x99);
	private SPCBoard board;

	
    private int x0, y0, sqSize;
    private boolean flipped;
    private Font chessFont;
    
 // For piece animation during dragging
//	private int selectedRank;
//	private int selectedFile;

    private short activeSquare;
    private int activeRank;
    private int activeFile;
    private short lastMoveTo = -1;
    private boolean dragging;
    private int dragX;
    private int dragY;
//    private boolean cancelSelection;
    
    private List<ChessboardListener> listeners = new ArrayList<ChessboardListener>();
	private boolean acceptMouseMoves;
	
	public ChessBoardPanel(SPCBoard board) {
		this.board = board;
		
	       addMouseMotionListener(new MouseMotionAdapter() {

	    		@Override
	    		public void mouseDragged(MouseEvent evt) {
	    			if (acceptMouseMoves) {
	    				ChessBoardPanel.this.mouseDragged(evt);
	    			}
	    		}

	    	});
	    	addMouseListener(new MouseAdapter() {

	    		@Override
	    		public void mousePressed(MouseEvent evt) {
	    			if (acceptMouseMoves) {
	    				short sq = eventToSquare(evt);
	    				ChessBoardPanel.this.mousePressed(sq);
	    				
	    			}
	    		}

	    		@Override
	    		public void mouseReleased(MouseEvent evt) {
	    			if (acceptMouseMoves) {
	    				short sq = eventToSquare(evt);
	    				short[] m = ChessBoardPanel.this.mouseReleased(sq);
	    				if (m != null) {
	    					
	    						for (ChessboardListener listener: listeners) {
	    							listener.userMadeMove(m[0], m[1]);
	    						}
	    					
	    				}
	    			}

	    		}
	    	});


	}
	
	protected boolean isLegalMove(short[] m) {
		int[] moves = new int[128];
		int numMoves = board.getMoveGenerator().fillLegalMoves(moves, 0);
		for (int i = 0; i < numMoves; i++) {
			short from = Encodings.getFromSquare(moves[i]);
			short to = Encodings.getToSquare(moves[i]);
			if (from == m[0] && to == m[1]) {
				return true;
			}
		}
		return false;
	}

	public void addListener(ChessboardListener listener) {
		listeners.add(listener);
	}
	
	public void setAcceptMouseMoves(boolean acceptMouseMoves) {
		this.acceptMouseMoves = acceptMouseMoves;
	}
	
	public void setLastMoveTo(short toSquare) {
		lastMoveTo = toSquare;
	}
	
	@Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D)g0;
        Dimension size = getSize();
        sqSize = (Math.min(size.width, size.height) - 4) / 8;
        x0 = (size.width - sqSize * 8) / 2;
        y0 = (size.height - sqSize * 8) / 2;
        
//        boolean doDrag = (activeRank >= 0) && dragging;
        
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                final int xCrd = getXCrd(x);
                final int yCrd = getYCrd(y);
                g.setColor(board.isWhiteSquare(Encodings.encodeSquare(x, y)) ? DARK_SQ_COLOR : LIGHT_SQ_COLOR);
                g.fillRect(xCrd, yCrd, sqSize, sqSize);
                g.setColor(Color.black);
                byte piece = board.getPiece(Encodings.encodeSquare(x, y));
                g.drawString(Encodings.getNotation(x,y) + " " + piece, xCrd, yCrd + 15);
                if (dragging && (activeFile == x && activeRank == y)) {
                    // Skip this piece. It will be drawn later at (dragX,dragY)
                } else {
                    drawPiece(g, xCrd + sqSize / 2, yCrd + sqSize / 2, piece);
                }
            }
        }
        if (lastMoveTo >= 0) {
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(3));
            g.drawRect(getXCrd(getX(lastMoveTo)), getYCrd(getY(lastMoveTo)), sqSize, sqSize);
        }
        if (dragging) {
        	
            byte p = board.getPiece(Encodings.encodeSquare(activeFile, activeRank));
            drawPiece(g, dragX, dragY, p);
        }
        
	}
	
	private static String FONT_FILE = "fonts/DiaTTFri.TTF";
	private static String TYPE_TO_CHAR = "kqrlnpkqrlnp";
	private static char[] TYPE_TO_CHAR_ARRAY = TYPE_TO_CHAR.toCharArray();
	private Image[] images;
	
	
    private final void drawPiece(Graphics2D g, int xCrd, int yCrd, byte piece) {
    	if (Encodings.isPiece(piece)) {
    		createChessFont();
    		Image img = images[getImageIndex(piece)];
    		g.drawImage(img, xCrd - sqSize/2, yCrd - sqSize/2, this);
    	}
    }
    
	private int getImageIndex(byte p) {
		if (Encodings.isPiece(p)) {
			return p-1;
		}
		return -1;
	}

	private void createChessFont() {
		if ((chessFont == null) || (chessFont.getSize() != sqSize)) {
			try {
//		    InputStream inStream = getClass().getResourceAsStream(FONT_FILE);
			InputStream inStream = new FileInputStream(FONT_FILE);
		        Font font = Font.createFont(Font.TRUETYPE_FONT, inStream);
		        chessFont = font.deriveFont((float)sqSize);
		        images = new Image[TYPE_TO_CHAR_ARRAY.length];
		        for (int i = 0; i < TYPE_TO_CHAR_ARRAY.length; i++) {
		        	images[i] = ChessFontUtil.getPieceImage(chessFont, "" + TYPE_TO_CHAR_ARRAY[i], 
		        			Color.WHITE, Color.BLACK, i <= 5, sqSize);
		        }
		    } catch (FontFormatException ex) {
		        throw new RuntimeException(ex.getMessage());
		    } catch (IOException ex) {
		        throw new RuntimeException(ex.getMessage());
		    }
		}
	}
	
	public final short[] mousePressed(short sq) {
        short[] m = null;
        byte p = board.getPiece(sq);
        if (p == Encodings.EMPTY || (Encodings.isWhite(p) != board.getWhiteToMove())) {
        	return null;
        } 
        activeFile = getX(sq);
        activeRank = getY(sq);
        activeSquare = sq;
        dragging = false;
        dragX = dragY = -1;

        
        return m;
    }

    private final void mouseDragged(MouseEvent evt) {
        final int xCrd = evt.getX();
        final int yCrd = evt.getY();
        if (!dragging || (dragX != xCrd) || (dragY != yCrd)) {
            dragging = true;
            dragX = xCrd;
            dragY = yCrd;
            repaint();
        }
    }

    private final short[] mouseReleased(short sq) {
        short[] m = null;
        if (activeSquare >= 0) {
            if (sq != activeSquare) {
                m = new short[] {activeSquare, sq};
                if (!isLegalMove(m)) {
                	m = null;
                	activeFile = -1;
                	activeRank = -1;
                	activeSquare = -1;
                }
            } 
            repaint();
        }
        return m;
    }

    /**
     * Compute the square corresponding to the coordinates of a mouse event.
     * @param evt Details about the mouse event.
     * @return The square corresponding to the mouse event, or -1 if outside board.
     */
    public final short eventToSquare(MouseEvent evt) {
        int xCrd = evt.getX();
        int yCrd = evt.getY();

        short sq = geomCoordsToSquare(xCrd, yCrd);
        return sq;
    }

	private short geomCoordsToSquare(int xCrd, int yCrd) {
		int sq = -1;
        if ((xCrd >= x0) && (yCrd >= y0) && (sqSize > 0)) {
            int x = (xCrd - x0) / sqSize;
            int y = 7 - (yCrd - y0) / sqSize;
            if ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
                if (flipped) {
                    x = 7 - x;
                    y = 7 - y;
                }
                sq = getSquare(x, y);
            }
        }
		return (short) sq;
	}

    private short getSquare(int x, int y) {
    	return (short) (8*y+x);
    }
    
    private int getX(int sq) {
    	return sq % 8;
    }
    
    private int getY(int sq) {
    	return sq/8;
    }
    
    private final int getXCrd(int x) {
        return x0 + sqSize * (flipped ? 7 - x : x);
    }
    
    private final int getYCrd(int y) {
        return y0 + sqSize * (flipped ? y : (7 - y));
    }
	final public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

}
