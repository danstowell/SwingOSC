package de.sciss.swingosc;

import java.awt.event.ActionListener;

import de.sciss.gui.MenuGroup;

public class SwingMenuGroup
extends MenuGroup
{
	private final DispatchAction action;
	
	public SwingMenuGroup( String id, String text )
	{
//		this( id, text, null, 0 );
//	}
//	
//	public SwingMenuGroup( String id, String text, String accel, int modifiers )
//	{
//		super( id, new DispatchAction( text, SwingMenuItem.createKeyStroke( accel, modifiers )));
		super( id, new DispatchAction( text ));
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
}