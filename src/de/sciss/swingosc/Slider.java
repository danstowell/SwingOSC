/*
 *  Slider.java
 *  SwingOSC
 *
 *  Copyright (c) 2005-2009 Hanns Holger Rutz. All rights reserved.
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
 *		11-Nov-05	created
 *		25-Dec-05	added setValueNoAction (doesn't fire an ActionEvent)
 */
 
package de.sciss.swingosc;

import java.awt.AWTEventMulticaster;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.BoundedRangeModel;
import javax.swing.InputMap;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *	A simple extention of <code>javax.swing.JSlider</code>
 *	which adds <code>ActionListener</code> functionality
 *	and a <code>setValueAction</code> method for programmatically
 *	dragging the slider.
 *
 *	@author		Hanns Holger Rutz
 *	@version	0.45, 24-Jan-06
 *
 *	@todo	could also extend SliderBase
 */
public class Slider
extends JSlider
implements ChangeListener
{
	private	ActionListener al = null;

	static {
		// Gtk+ plaf stupidly doubles gadget height by painting
		// the current value above the slider. so switch it off:
		UIManager.put( "Slider.paintValue", Boolean.FALSE );
	}
	
	public Slider()
	{
		super();
		init();
	}

	public Slider( int orientation )
	{
		super( orientation );
		init();
	}

	public Slider( int min, int max )
	{
		super( min, max );
		init();
	}
			   
	public Slider( int min, int max, int value )
	{
		super( min, max, value );
		init();
	}
			   
	public Slider( int orientation, int min, int max, int value )
	{
		super( orientation, min, max, value );
		init();
	}
			   
	public Slider( BoundedRangeModel brm )
	{
		super( brm );
		init();
	}
	
	private void init()
	{
		addChangeListener( this );
		final InputMap imap = getInputMap();
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, 0 ), "none" );
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_UP, 0 ), "none" );
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, 0 ), "none" );
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, 0 ), "none" );
	}
	
		
	public void setValueNoAction( int n )
	{
		removeChangeListener( this );
		super.setValue( n );
		addChangeListener( this );
	}
	
//	public void setValueAction( int n )
//	{
//		setValue( n );
//		fireStateChanged();
//	}
	
	public synchronized void addActionListener( ActionListener l )
	{
		al = AWTEventMulticaster.add( al, l );
	}
	
	public synchronized void removeActionListener( ActionListener l )
	{
		al = AWTEventMulticaster.remove( al, l );
	}

	public void stateChanged( ChangeEvent e )
	{
		final ActionListener l = al;
		if( l != null ) {
			l.actionPerformed( new ActionEvent( e.getSource(), ActionEvent.ACTION_PERFORMED, null ));
		}
	}
}