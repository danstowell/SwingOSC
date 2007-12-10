/*
 *  Pen.java
 *  SwingOSC
 *
 *  Copyright (c) 2005-2007 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *  	03-Oct-06	modified string commands (font + colour are separate)
 *  	11-Dec-06	ConstrStringInRect takes additional halign and valign args.
 *  				ConstrDraw uses CmdFill instead of CmdDraw (making the
 *  				pixel coordinates identical to cocoa pen)
 *  	04-Feb-07	discovered a weird performance problem with complex shapes
 *  				generated through g2.fill( stroke.createStrokedShape( ... ))
 *  				 (probably due to
 *  				joining calculations). interestingly, this problem disappears
 *  				when we go back to g2.draw(). using a translation of -0.5,-0.5
 *  				we still get images pixel-compatible with cocoa.
 *  				; clip and matrix are concatenating
 *  	24-Nov-07	stroke is transformed according to current AffineTransform
 *  				at draw statement (behaves like cocoa counterpart)
 */
 
package de.sciss.swingosc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

import de.sciss.gui.GUIUtil;

/**
 *	@version	0.57, 10-Dec-07
 *	@author		Hanns Holger Rutz
 */
public class Pen
implements Icon
{
	private Component 					c			= null;
//	private final GeneralPath			gp			= new GeneralPath();
//	private final AffineTransform		at			= new AffineTransform();
//	private Paint						pntDraw		= null;
//	private Paint						pntFill		= null;
	private Cmd[] 						cmds		= new Cmd[ 0 ];
	private final Stack 				context		= new Stack();
	
	private final List					recCmds		= new ArrayList();
	private final Map					mapConstr	= new HashMap();
	
	private final float[]				pt			= new float[ 8 ];
	
	private static final float			kRad2Deg	= (float) (180.0 / Math.PI);
	private static final float			kRad2DegM	= (float) (-180.0 / Math.PI);
	
	private static final BasicStroke	strkDefault	= new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER );
	private static final Font			fntDefault	= new Font( "SansSerif", Font.PLAIN, 11 );
	
	private final FontRenderContext		frc;
	private GraphicsContext				gc;
	
	private boolean						absCoords;

	public Pen()
	{
		this( false );
	}

	public Pen( boolean absCoords )
	{
		mapConstr.put( "drw", new ConstrDraw() );
		mapConstr.put( "fll", new ConstrFill() );
		mapConstr.put( "stk", new ConstrStroke() );
		mapConstr.put( "mov", new ConstrMoveTo() );
		mapConstr.put( "lin", new ConstrLineTo() );
		mapConstr.put( "qua", new ConstrQuadTo() );
		mapConstr.put( "cub", new ConstrCurveTo() );
		mapConstr.put( "rec", new ConstrAddRect() );
		mapConstr.put( "arc", new ConstrAddArc( Arc2D.OPEN ));
		mapConstr.put( "pie", new ConstrAddArc( Arc2D.PIE ) );
		mapConstr.put( "cyl", new ConstrAddCylSector() );
		mapConstr.put( "rst", new ConstrReset() );
		mapConstr.put( "trn", new ConstrTranslate() );
		mapConstr.put( "scl", new ConstrScale() );
		mapConstr.put( "rot", new ConstrRotate() );
		mapConstr.put( "shr", new ConstrShear() );
		mapConstr.put( "mat", new ConstrMatrix() );
		mapConstr.put( "dco", new ConstrDrawColor() );
		mapConstr.put( "fco", new ConstrFillColor() );
		mapConstr.put( "fnt", new ConstrFont() );
		mapConstr.put( "frc", new ConstrFillRect() );
		mapConstr.put( "fov", new ConstrFillOval() );
		mapConstr.put( "drc", new ConstrDrawRect() );
		mapConstr.put( "dov", new ConstrDrawOval() );
		mapConstr.put( "dst", new ConstrStringAtPoint() );
		mapConstr.put( "dsr", new ConstrStringInRect() );
		mapConstr.put( "psh", new ConstrPush() );
		mapConstr.put( "pop", new ConstrPop() );
		mapConstr.put( "clp", new ConstrClip() );
		mapConstr.put( "ali", new ConstrSmooth() );
		
		frc = new FontRenderContext( GraphicsEnvironment.
				getLocalGraphicsEnvironment().
				getDefaultScreenDevice().
				getDefaultConfiguration().
				getNormalizingTransform(), true, true );
//		frc	= new FontRenderContext( null, true, true );
		
		this.absCoords = absCoords;
	}
	
	public Pen( Component c )
	{
		this( c, false );
	}
	
	// absCoords : if true, all coordinates are
	// seen as relative to the window's top left
	// as is the case unfortunately with sc
	public Pen( Component c, boolean absCoords )
	{
		this( absCoords );
		setComponent( c );
	}
	
	public void setAbsCoords( boolean absCoords )
	{
		this.absCoords = absCoords;
	}
	
	public boolean getAbsCoords()
	{
		return absCoords;
	}
	
	public void beginRec()
	{
		recCmds.clear();
		context.clear();
		gc	= new GraphicsContext();
	}
	
	public void setComponent( Component c )
	{
		this.c	= c;
	}
	
	public Component getComponent()
	{
		return c;
	}
	
	public void add( Object[] oscCmds )
	{
		Object cmdID = null;

		try {
			for( int off = 0; off < oscCmds.length; ) {
				cmdID	= oscCmds[ off++ ];
				off 		= ((Constr) mapConstr.get( cmdID )).constr( oscCmds, off );
			}
		}
		catch( NullPointerException e1 ) {
			System.out.println( "Pen.add : unknown command " + cmdID );
		}
		catch( NumberFormatException e1 ) {
			System.out.println( "Pen.add : argument type mismatch for " + cmdID );
		}
		catch( IndexOutOfBoundsException e1 ) {
			System.out.println( "Pen.add : argument count mismatch for " + cmdID );
		}
	}
	
	public void stopRec()
	{
		final int numCmds = recCmds.size();
		cmds = new Cmd[ numCmds ];
		for( int i = 0; i < numCmds; i++ ) {
			cmds[ i ] = (Cmd) recCmds.get( i );
		}
		recCmds.clear();
		context.clear();
		gc = null;
	}
	
	public void dispose()
	{
		gc = null;
		recCmds.clear();
		context.clear();
		cmds = new Cmd[ 0 ];
	}
	
	public void paintIcon( Component c, Graphics g, int x, int y )
	{
		final Graphics2D		g2			= (Graphics2D) g;
		final AffineTransform 	atOrig		= g2.getTransform();
		final Stroke			strkOrig	= g2.getStroke();
		final Shape				clipOrig	= g2.getClip();
		
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		if( absCoords ) {
			final Component ref		= SwingUtilities.getRootPane( c ).getContentPane();
			final Point		ptCorr	= GUIUtil.convertPoint( ref, new Point( x, y ), c );
			if( (ptCorr.x != 0 ) || (ptCorr.y != 0) ) g2.translate( ptCorr.x, ptCorr.y );
		} else {
			if( (x != 0 ) || (y != 0) ) g2.translate( x, y );
		}
		for( int i = 0; i < cmds.length; i++ ) {
			cmds[ i ].perform( g2 );
		}

		g2.setTransform( atOrig );
		g2.setStroke( strkOrig );
		g2.setClip( clipOrig );
	}
	
    public int getIconWidth()
	{
		return c == null ? 0 : c.getWidth();
	}

	public int getIconHeight()
	{
		return c == null ? 0 : c.getHeight();
	}

	private static class GraphicsContext
	{
		private Paint					pntDraw;
		private Paint					pntFill;
		private BasicStroke				strk;
		private final AffineTransform	at;
		private Shape					clip;
//		private final GeneralPath		gp;
		private GeneralPath				gp;
		private Font					fnt;
		private RenderingHints			hints;
		
		private GraphicsContext()
		{
			pntDraw	= Color.black;
			pntFill	= Color.black;
			strk	= strkDefault;
			at		= new AffineTransform();
			clip	= null;
			gp		= new GeneralPath();
			fnt		= fntDefault;
			hints	= new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		}
		
		private GraphicsContext( GraphicsContext orig )
		{
			pntDraw	= orig.pntDraw;
			pntFill	= orig.pntFill;
			strk	= orig.strk;
			at		= new AffineTransform( orig.at );
			clip	= orig.clip;
			gp		= new GeneralPath( orig.gp );
			fnt		= orig.fnt;
			hints	= orig.hints;
		}
		
		private void restore( Graphics2D g2 )
		{
			g2.setClip( clip );
			g2.setRenderingHints( hints );
		}
	}
	
	private abstract class Cmd
	{
		protected abstract void perform( Graphics2D g2 );
	}

	private class CmdFill
	extends Cmd
	{
		private final Shape 	shp;
		private final Paint	pnt;
		
		private CmdFill( Shape shp )
		{
			this.shp	= shp;
			pnt		= gc.pntFill;
		}
		
//		private CmdFill( Shape shp, Paint pnt )
//		{
//			this.shp	= shp;
//			this.pnt	= pnt;
//		}
		
		protected void perform( Graphics2D g2 )
		{
			g2.setPaint( pnt );
			g2.fill( shp );
		}
	}

	private class CmdDraw
	extends Cmd
	{
		private final Shape 			shp;
		private final Paint				pnt;
		private final Stroke			strk;
		private final AffineTransform	at;
		
		private CmdDraw( Shape shp )
		{
			pnt			= gc.pntDraw;
			
			final AffineTransform atInv;
			
test:		if( (gc.at.getShearX() == 0.0) && (gc.at.getShearY() == 0.0) &&
				(gc.at.getScaleX() == gc.at.getScaleY()) ) {
				
				this.shp	= shp;
				if( gc.at.getScaleX() == 1.0 ) {
//System.out.println( "C" );
					strk	= gc.strk;
					at		= null;
				} else {
//System.out.println( "D" );
					strk	= new BasicStroke( (float) (gc.strk.getLineWidth() * gc.at.getScaleX()),
											   BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER );
					at		= null;
				}
			} else {
				strk = gc.strk;
				try {
					atInv		= gc.at.createInverse();
//System.out.println( "A" );
				} catch( NoninvertibleTransformException e1 ) {
//System.out.println( "NoninvertibleTransformException" );
					// ... what can we do ...
					at			= null;
					this.shp	= shp;
					break test;
				}
				this.shp	= atInv.createTransformedShape( shp );
				at			= new AffineTransform( gc.at );
//System.out.println( "B" );
			}
		}
		
//		private CmdDraw( Shape shp, Paint pnt )
//		{
//			this.shp	= shp;
//			this.pnt	= pnt;
//			strk		= gc.strk;
//		}
		
		protected void perform( Graphics2D g2 )
		{
			final AffineTransform atOrig = g2.getTransform();
			if( at != null ) {
//System.out.println( "E" );
				g2.setTransform( at );
			}
			g2.translate( -0.5, -0.5 );
			g2.setPaint( pnt );
			g2.setStroke( strk );
			g2.draw( shp );
			g2.setTransform( atOrig );
		}
	}

	private class CmdClip
	extends Cmd
	{
		private final Shape 	shp;

		private CmdClip( Shape shp )
		{
			this.shp 	= shp;
		}
		
		protected void perform( Graphics2D g2 )
		{
//			g2.setClip( shp );
			g2.clip( shp );
		}
	}

	private class CmdRestore
	extends Cmd
	{
		private final GraphicsContext gc;

		private CmdRestore( GraphicsContext gc )
		{
			this.gc = gc;
		}
		
		protected void perform( Graphics2D g2 )
		{
			gc.restore( g2 );
		}
	}

	private class CmdHint
	extends Cmd
	{
		private final RenderingHints.Key key;
		private final Object value;

		private CmdHint( RenderingHints.Key key, Object value )
		{
			this.key 	= key;
			this.value 	= value;
		}
		
		protected void perform( Graphics2D g2 )
		{
			g2.setRenderingHint( key, value );
		}
	}

	private abstract class Constr
	{
		protected abstract int constr( Object[] cmd, int off );
		
		protected final int transform( Object[] cmd, int off, int num )
		{
			for( int i = 0, j = num << 1; i < j; ) {
				pt[ i++ ] = ((Number) cmd[ off++ ]).floatValue();
			}
			
			gc.at.transform( pt, 0, pt, 0, num );

			return off;
		}
		
		protected final int decode( Object[] cmd, int off, int num )
		{
			for( int i = 0; i < num; ) {
				pt[ i++ ] = ((Number) cmd[ off++ ]).floatValue();
			}
			
			return off;
		}
		
		protected Color getColor( Object[] cmd, int off )
		{
			return new Color( Math.max( 0f, Math.min( 1f, ((Number) cmd[ off++ ]).floatValue() )),
					Math.max( 0f, Math.min( 1f, ((Number) cmd[ off++ ]).floatValue() )),
					Math.max( 0f, Math.min( 1f, ((Number) cmd[ off++ ]).floatValue() )),
					Math.max( 0f, Math.min( 1f, ((Number) cmd[ off++ ]).floatValue() )));			
		}
	}

	private class ConstrDrawColor
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc.pntDraw = getColor( cmd, off );
			return off + 4;
		}
	}

	private class ConstrFillColor
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc.pntFill = getColor( cmd, off );
			return off + 4;
		}
	}

	private class ConstrFont
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			final String 	fntName;
			final int		fntSize, fntStyle;
			
			fntName		= cmd[ off++ ].toString();
			fntSize		= ((Number) cmd[ off++ ]).intValue();
			fntStyle	= ((Number) cmd[ off++ ]).intValue();
			gc.fnt		= new Font( fntName, fntStyle, fntSize );
			return off;
		}
	}

	private class ConstrMoveTo
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = transform( cmd, off, 1 );
			gc.gp.moveTo( pt[ 0 ], pt[ 1 ]);
			return off;
		}
	}

	private class ConstrLineTo
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = transform( cmd, off, 1 );
			gc.gp.lineTo( pt[ 0 ], pt[ 1 ]);
			return off;
		}
	}

	private class ConstrQuadTo
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = transform( cmd, off, 2 );
			gc.gp.quadTo( pt[ 0 ], pt[ 1 ], pt[ 2 ], pt[ 3 ]);
			return off;
		}
	}

	private class ConstrCurveTo
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = transform( cmd, off, 3 );
			gc.gp.curveTo( pt[ 0 ], pt[ 1 ], pt[ 2 ], pt[ 3 ], pt[ 4 ], pt[ 5 ]);
			return off;
		}
	}

	private class ConstrReset
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc.gp.reset();
			return off;
		}
	}

	private class ConstrDraw
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			recCmds.add( new CmdDraw( gc.gp ));
//			recCmds.add( new CmdFill( gc.strk.createStrokedShape( gc.gp ), gc.pntDraw ));
			gc.gp = new GeneralPath();
			return off;
		}
	}

	private class ConstrFill
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
//			recCmds.add( new CmdFill( new GeneralPath( gc.gp )));
//			gc.gp.reset();
			recCmds.add( new CmdFill( gc.gp ));
			gc.gp = new GeneralPath();
			return off;
		}
	}

	private class ConstrClip
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			recCmds.add( new CmdClip( gc.gp ));
			gc.clip	= gc.gp;
			gc.gp	= new GeneralPath();
			return off;
		}
	}

	private class ConstrTranslate
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc.at.translate( ((Number) cmd[ off++ ]).doubleValue(), ((Number) cmd[ off++ ]).doubleValue() );
			return off;
		}
	}

	private class ConstrScale
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc.at.scale( ((Number) cmd[ off++ ]).doubleValue(), ((Number) cmd[ off++ ]).doubleValue() );
			return off;
		}
	}

	private class ConstrRotate
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc.at.rotate( ((Number) cmd[ off++ ]).doubleValue(), 
					  ((Number) cmd[ off++ ]).doubleValue(), ((Number) cmd[ off++ ]).doubleValue() );
			return off;
		}
	}

	private class ConstrShear
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc.at.shear( ((Number) cmd[ off++ ]).doubleValue(), ((Number) cmd[ off++ ]).doubleValue() );
			return off;
		}
	}

	// 0: sx, 1: shy, 2: shx, 3: sy, 4: tx, 5: ty
	private class ConstrMatrix
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			final double sx		= ((Number) cmd[ off++ ]).doubleValue();
			final double shx	= ((Number) cmd[ off++ ]).doubleValue();
			final double shy	= ((Number) cmd[ off++ ]).doubleValue();
			final double sy		= ((Number) cmd[ off++ ]).doubleValue();
			final double tx		= ((Number) cmd[ off++ ]).doubleValue();
			final double ty		= ((Number) cmd[ off++ ]).doubleValue();
			
//			gc.at.setTransform( sx, shx, shy, sy, tx, ty );
			gc.at.concatenate( new AffineTransform( sx, shx, shy, sy, tx, ty ));
			return off;
		}
	}

	private class ConstrStroke
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			final float width = ((Number) cmd[ off++ ]).floatValue();
			gc.strk	 = new BasicStroke( width,
						BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ); // JOIN_MITER
//			recCmds.add( new CmdStroke(  ));
			return off;
		}
	}

	private class ConstrFillRect
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = decode( cmd, off, 4 );
			final Shape shp = new Rectangle2D.Float( pt[ 0 ], pt[ 1 ], pt[ 2 ], pt[ 3 ]);
			recCmds.add( new CmdFill( gc.at.createTransformedShape( shp )));
			return off;
		}
	}

	private class ConstrDrawRect
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = decode( cmd, off, 4 );
			final Shape shp = new Rectangle2D.Float( pt[ 0 ], pt[ 1 ], pt[ 2 ], pt[ 3 ]);
			recCmds.add( new CmdDraw( gc.at.createTransformedShape( shp )));
//			recCmds.add( new CmdFill( gc.at.createTransformedShape(
//					gc.strk.createStrokedShape( shp )), gc.pntDraw ));
			return off;
		}
	}

	private class ConstrFillOval
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = decode( cmd, off, 4 );
			final Shape shp = new Ellipse2D.Float( pt[ 0 ], pt[ 1 ], pt[ 2 ], pt[ 3 ]);
			recCmds.add( new CmdFill( gc.at.createTransformedShape( shp )));
			return off;
		}
	}

	private class ConstrDrawOval
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = decode( cmd, off, 4 );
			final Shape shp = new Ellipse2D.Float( pt[ 0 ], pt[ 1 ], pt[ 2 ], pt[ 3 ]);
//final double[] mm = new double[ 6 ];
//gc.at.getMatrix( mm );
//for( int k = 0; k < mm.length; k++ ) System.out.print( mm[ k ] + ", " );
//System.out.println();
//final AffineTransform atTest = new AffineTransform( gc.at );
//recCmds.add( new CmdDraw( atTest.createTransformedShape( shp )));
			recCmds.add( new CmdDraw( gc.at.createTransformedShape( shp )));
			return off;
		}
	}
	
	private class ConstrAddRect
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			off = decode( cmd, off, 4 );
			final Shape shp = new Rectangle2D.Float( pt[ 0 ], pt[ 1 ], pt[ 2 ], pt[ 3 ]);
			gc.gp.append( gc.at.createTransformedShape( shp ), false );
			return off;
		}
	}

	private class ConstrAddArc
	extends Constr
	{
		private int type;
		
		private ConstrAddArc( int type )
		{
			this.type = type;
		}
		
		protected int constr( Object[] cmd, int off )
		{
			off = decode( cmd, off, 5 );
			final Arc2D arc = new Arc2D.Float();
			arc.setArcByCenter( pt[ 0 ], pt[ 1 ], pt[ 2 ],
							    (pt[ 3 ] + pt[ 4 ]) * kRad2DegM, pt[ 4 ] * kRad2Deg, type );
			gc.gp.append( gc.at.createTransformedShape( arc ), false );
			return off;
		}
	}

	private class ConstrAddCylSector
	extends Constr
	{
		// 0: cx, 1: cy, 2: ri, 3: ro, 4: angSt, 5, angExt
		protected int constr( Object[] cmd, int off )
		{
			off = decode( cmd, off, 6 );
			final Arc2D pie = new Arc2D.Float();
			final float innerDiam = pt[ 2 ] * 2;
			pie.setArcByCenter( pt[ 0 ], pt[ 1 ], pt[ 3 ],
							   		  (pt[ 4 ] + pt[ 5 ]) * kRad2DegM, pt[ 5 ] * kRad2Deg, Arc2D.PIE );
			final Ellipse2D cyl = new Ellipse2D.Float(
					pt[ 0 ] - pt[ 2 ], pt[ 1 ] - pt[ 2 ], innerDiam, innerDiam );

			final Area shp = new Area( pie );
			shp.subtract( new Area( cyl ));
			gc.gp.append( gc.at.createTransformedShape( shp ), false );
			return off;
		}
	}

	// 0: str, 1: x, 2: y, NOT ANY MORE: 3: fntName, 4: fntSize, 5: fntStyle,
	// 6: colrRed, 7: colrGreen, 8: colrBlue, 9: colrAlpha
	private class ConstrStringAtPoint
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			final String		str;
//			final String		fntName;
//			final Paint			pnt;
//			final int			fntSize, fntStyle;
//			final Font			fnt;
			final GlyphVector	glyph;
			final LineMetrics	lineMetr;
			final Shape			shp;
			
			str 		= cmd[ off++ ].toString();
			off 		= decode( cmd, off, 2 );
//			off 		= transform( cmd, off, 1 );
//			fntName		= cmd[ off++ ].toString();
//			fntSize		= ((Number) cmd[ off++ ]).intValue();
//			fntStyle	= ((Number) cmd[ off++ ]).intValue();
//			fnt			= new Font( fntName, fntStyle, fntSize );
//			pnt			= getColor( cmd, off );
//			off	  	   += 4;

//System.out.println( "text '"+str+"'; at "+pt[0]+","+pt[1]+"; font "+fnt+"; colr "+pnt );
			
			glyph	= gc.fnt.createGlyphVector( frc, str );
			lineMetr= gc.fnt.getLineMetrics( str, frc );
			shp		= glyph.getOutline( pt[ 0 ], pt[ 1 ] + lineMetr.getAscent() + lineMetr.getDescent() ); // ???

			recCmds.add( new CmdFill( gc.at.createTransformedShape( shp )));

			return off;
		}
	}

	// 0: str, 1: x, 2: y, 3: w, 4: h, 5: halign, 6: valign
	private class ConstrStringInRect
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			final String		str;
			
			str 		= cmd[ off++ ].toString();
			off 		= decode( cmd, off, 6 );

			final AttributedCharacterIterator styledText = new AttributedString( str, gc.fnt.getAttributes() ).getIterator();
			final LineBreakMeasurer lbm = new LineBreakMeasurer( styledText, frc);
			final float w = pt[ 2 ];
			float x = pt[ 0 ];               
			float y = pt[ 1 ];
			final float yStop = y + pt[ 3 ];
			final float halign = pt[ 4 ];
			final float valign = pt[ 5 ];
			final GeneralPath gp = new GeneralPath();
			final AffineTransform atPos = new AffineTransform();
			final float dy;
			float dx;
			TextLayout txtLay;
		    
			while( lbm.getPosition() < styledText.getEndIndex() ) {
		         txtLay	= lbm.nextLayout( w );
		         y  	   += txtLay.getAscent();
		         if( y + txtLay.getDescent() > yStop ) break;
		         dx		= (w - txtLay.getVisibleAdvance()) *
		         	(txtLay.isLeftToRight() ? halign : (1.0f - halign));

		         atPos.setToTranslation( x + dx, y );
		         gp.append( txtLay.getOutline( atPos ), false );
		         y 	   += txtLay.getDescent() + txtLay.getLeading();
		     }
			dy = (yStop - y) * valign;
			if( dy != 0f ) gp.transform( AffineTransform.getTranslateInstance( 0, dy ));

			recCmds.add( new CmdFill( gc.at.createTransformedShape( gp )));

			return off;
		}
	}

	private class ConstrPush
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			context.push( gc );
			gc = new GraphicsContext( gc );
			return off;
		}
	}

	private class ConstrPop
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			gc = (GraphicsContext) context.pop();
			recCmds.add( new CmdRestore( gc ));
			return off;
		}
	}

	// 0: on/off
	private class ConstrSmooth
	extends Constr
	{
		protected int constr( Object[] cmd, int off )
		{
			final boolean	onOff;
			final Object	value;
			onOff		= ((Number) cmd[ off++ ]).intValue() != 0;
			value		= onOff ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
			recCmds.add( new CmdHint( RenderingHints.KEY_ANTIALIASING, value ));
			gc.hints	= new RenderingHints( (Map) gc.hints );
			gc.hints.put( RenderingHints.KEY_ANTIALIASING, value );
			return off;
		}
	}
}