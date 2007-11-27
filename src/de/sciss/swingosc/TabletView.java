/*
 *  TabletView.java
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
 *		26-Nov-07	created
 */
package de.sciss.swingosc;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;

import de.sciss.app.DynamicAncestorAdapter;
import de.sciss.app.DynamicListening;
import de.sciss.gui.AquaFocusBorder;

import com.jhlabs.jnitablet.TabletEvent;
import com.jhlabs.jnitablet.TabletProximityEvent;
import com.jhlabs.jnitablet.TabletListener;
import com.jhlabs.jnitablet.TabletWrapper;

/**
 *	@author		Hanns Holger Rutz
 *	@version	0.57, 26-Nov-07
 */
public class TabletView
extends JComponent
implements DynamicListening, TabletListener, FocusListener, MouseListener
{
	private boolean			focusBorderVisible	= true;
	private AquaFocusBorder	border;
	private boolean 		added				= false;
	
	private final List		listeners			= new ArrayList();
	
	public TabletView()
	{
		super();
		border = new AquaFocusBorder();
		setBorder( border );
		putClientProperty( "insets", getInsets() );
		setFocusable( true );
		addFocusListener( this );
		new DynamicAncestorAdapter( this ).addTo( this );
		addMouseListener( this );
	}
	
	public void addTabletListener( TabletListener l )
	{
		listeners.add( l );
	}
	
	public void removeTabletListener( TabletListener l )
	{
		listeners.remove( l );
	}

	public void setFocusVisible( boolean b )
	{
		if( b != focusBorderVisible ) {
			focusBorderVisible = b;
			border.setVisible( b );
		}
	}

	public void paintComponent( Graphics g )
	{
		final Color		bg 		= getBackground();
		final Insets	insets	= getInsets();
		if( (bg != null) && (bg.getAlpha() > 0) ) {
			g.setColor( bg );
			g.fillRect( insets.left, insets.top,
					getWidth() - (insets.left + insets.right),
					getHeight() - (insets.top + insets.bottom ));
		}
	}

//	 ---------------- DynamicListening interface ----------------

	public void startListening()
	{
//		TabletWrapper.getInstance().addTabletListener( this );
	}

	public void stopListening()
	{
		if( added ) {
//			System.out.println( "removing" );
			TabletWrapper.getInstance().removeTabletListener( this );
			added = false;
		}
	}
	
//	 ---------------- FocusListener interface ----------------

	public void focusGained( FocusEvent e )
	{
		repaint();
	}

	public void focusLost( FocusEvent e )
	{
		repaint();
	}

//	 ---------------- MouseListener interface ----------------

	public void mouseEntered( MouseEvent e ) {
		if( !added ) {
//			System.out.println( "adding" );
			TabletWrapper.getInstance().addTabletListener( this );
			added = true;
		}
	}

	public void mouseExited( MouseEvent e ) {
		if( added ) {
//			System.out.println( "removing" );
			TabletWrapper.getInstance().removeTabletListener( this );
			added = false;
		}
	}
	
	public void mouseClicked( MouseEvent e ) {}
	public void mousePressed( MouseEvent e ) {}
	public void mouseReleased( MouseEvent e ) {}

//	 ---------------- TabletListener interface ----------------

	public void tabletEvent( TabletEvent e )
	{
//System.out.println( "event " + listeners.size() );
		for( Iterator iter = listeners.iterator(); iter.hasNext(); ) {
			((TabletListener) iter.next()).tabletEvent( e );
		}
//		System.out.println( "TabletEvent" );
//		System.out.println( "  id                         " + e.getID() );
//		System.out.println( "  x                          " + e.getX() );
//		System.out.println( "  y                          " + e.getY() );
//		System.out.println( "  absoluteY                  " + e.getAbsoluteY() );
//		System.out.println( "  absoluteX                  " + e.getAbsoluteX() );
//		System.out.println( "  absoluteZ                  " + e.getAbsoluteZ() );
//		System.out.println( "  buttonMask                 " + e.getButtonMask() );
//		System.out.println( "  pressure                   " + e.getPressure() );
//		System.out.println( "  rotation                   " + e.getRotation() );
//		System.out.println( "  tiltX                      " + e.getTiltX() );
//		System.out.println( "  tiltY                      " + e.getTiltY() );
//		System.out.println( "  tangentialPressure         " + e.getTangentialPressure() );
//		System.out.println( "  vendorDefined1             " + e.getVendorDefined1() );
//		System.out.println( "  vendorDefined2             " + e.getVendorDefined2() );
//		System.out.println( "  vendorDefined3             " + e.getVendorDefined3() );
//		System.out.println();
	}

	public void tabletProximity( TabletProximityEvent e )
	{
		for( Iterator iter = listeners.iterator(); iter.hasNext(); ) {
			((TabletListener) iter.next()).tabletProximity( e );
		}
//		System.out.println( "TabletProximityEvent" );
//		System.out.println( "  capabilityMask             " + e.getCapabilityMask() );
//		System.out.println( "  deviceID                   " + e.getDeviceID() );
//		System.out.println( "  enteringProximity          " + e.isEnteringProximity() );
//		System.out.println( "  pointingDeviceID           " + e.getPointingDeviceID() );
//		System.out.println( "  pointingDeviceSerialNumber " + e.getPointingDeviceSerialNumber() );
//		System.out.println( "  pointingDeviceType         " + e.getPointingDeviceType() );
//		System.out.println( "  systemTabletID             " + e.getSystemTabletID() );
//		System.out.println( "  tabletID                   " + e.getTabletID() );
//		System.out.println( "  uniqueID                   " + e.getUniqueID() );
//		System.out.println( "  vendorID                   " + e.getVendorID() );
//		System.out.println( "  vendorPointingDeviceType   " + e.getVendorPointingDeviceType() );
//		System.out.println();
	}
}