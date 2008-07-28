package de.sciss.swingosc;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

import de.sciss.net.OSCMessage;

public class DummyActionOBSOLETE
extends AbstractAction
{
	private SwingClient			client;
	private final Object[]		replyArgs;
	
	public DummyActionOBSOLETE( Object objectID, String text )
	{
		super( text );
		
		final SwingOSC osc;
		
		osc					= SwingOSC.getInstance();
		client				= osc.getCurrentClient();
//		object				= client.getObject( objectID );
		replyArgs			= new Object[ 2 ];
		replyArgs[ 0 ]		= objectID;
	}
	
	public void remove()
	{
		client = null;
	}
	
	public void actionPerformed( ActionEvent e )
	{
		if( client != null ) reply( "performed" );
	}

	protected String getOSCCommand()
	{
		return "/action";
	}
	
	protected void reply( String stateName )
	{
		try {
			replyArgs[ 1 ] = stateName;
//			osc.getProperties( object, replyArgs, 2, propertyNames, 0, propertyNames.length );
			client.reply( new OSCMessage( getOSCCommand(), replyArgs ));
		}
//		catch( NoSuchMethodException ex ) {
//			SwingOSC.printException( ex, getOSCCommand() );
//		}
//		catch( IllegalAccessException ex ) {
//			SwingOSC.printException( ex, getOSCCommand() );
//		}
//		catch( InvocationTargetException ex ) {
//			SwingOSC.printException( ex.getTargetException(), getOSCCommand() );
//		}
		catch( IOException ex ) {
			SwingOSC.printException( ex, getOSCCommand() );
		}
	}
}