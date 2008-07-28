package de.sciss.swingosc;

import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.KeyStroke;

import de.sciss.common.BasicMenuFactory;
import de.sciss.gui.MenuItem;

public class SwingMenuItem
extends MenuItem
{
	private final DispatchAction action;
	
	public SwingMenuItem( String id, String text )
	{
//		this( id, text, null, 0 );
//	}
//	
//	public SwingMenuItem( String id, String text, String accel, int modifiers )
//	{
		super( id, new DispatchAction( text ));
//		super( id, new DispatchAction( text, SwingMenuItem.createKeyStroke( accel, modifiers )));
//		super( id, new DispatchAction( text, KeyStroke.getKeyStroke( KeyEvent.VK_A, InputEvent.META_MASK )));
		action = (DispatchAction) this.getAction();
	}
	
	public void addActionListener( ActionListener l )
	{
		action.addActionListener( l );
	}

	public void removeActionListener( ActionListener l )
	{
		action.removeActionListener( l );
	}
	
	public void setName( String name )
	{
		SwingMenuItem.setName( this, name );
	}
	
	public void setShortCut( String cut, int modifiers )
	{
		SwingMenuItem.setShortCut( this, cut, modifiers );
	}
	
	protected static void setName( MenuItem mi, String name )
	{
		mi.getAction().putValue( Action.NAME, name );
	}

//	protected static KeyStroke createKeyStroke( String accel, int modifiers )
//	{
//		if( (accel == null) || (accel.length() != 1) ) return null;
//		return createKeyStroke( accel.charAt( 0 ), modifiers );
//	}
//	
	protected static KeyStroke createKeyStroke( String cut, int modifiers )
	{
		if( BasicMenuFactory.MENU_SHORTCUT != InputEvent.META_MASK ) {
			final boolean meta1 = (modifiers & InputEvent.META_MASK) != 0;
			final boolean meta2 = (modifiers & InputEvent.CTRL_MASK) != 0;
			if( meta1 ) {
				modifiers = modifiers & ~InputEvent.META_MASK | BasicMenuFactory.MENU_SHORTCUT;
			}
			if( meta2 ) {
				modifiers |= InputEvent.ALT_MASK;
			}
		}
		final StringBuffer sb = new StringBuffer();
		if( (modifiers & InputEvent.SHIFT_MASK) != 0 ) {
			sb.append( "shift " );
		}
		if( (modifiers & InputEvent.CTRL_MASK) != 0 ) {
			sb.append( "ctrl " );
		}
		if( (modifiers & InputEvent.META_MASK) != 0 ) {
			sb.append( "meta " );
		}
		if( (modifiers & InputEvent.ALT_MASK) != 0 ) {
			sb.append( "alt " );
		}
		if( (modifiers & InputEvent.ALT_GRAPH_MASK) != 0 ) {
			sb.append( "altGraph " );
		}
		sb.append( cut.toUpperCase() );
		
// !!! DOESN'T WORK SINCE THE STROKE IS "TYPED" NOT "PRESSED"
//		return KeyStroke.getKeyStroke( new Character( c ), modifiers );
//		return KeyStroke.getKeyStroke( c, modifiers );
		return KeyStroke.getKeyStroke( sb.toString() );
	}
	
	protected static void setShortCut( MenuItem mi, String cut, int modifiers )
	{
		final KeyStroke accel = createKeyStroke( cut, modifiers );
		
		final Action action = mi.getAction();
		action.putValue( Action.ACCELERATOR_KEY, accel );

//System.out.println( "keyStroke : " + accel );
		
		// this is a fucking stupid bug in swing:
		// the accelerator key is not taken after the menu item
		// has been created. the hack is to set its action to
		// null and then back to the action again
		for( Iterator iter = mi.getRealized(); iter.hasNext(); ) {
			final Realized r = (Realized) iter.next();
			final AbstractButton b = (AbstractButton) r.c;
			
			b.setAction( null );
			b.setAction( action );
		}
	}
}