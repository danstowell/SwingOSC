/*
 *  PeakMeterManager.java
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
 *		20-Dec-07	created
 */

package de.sciss.swingosc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import de.sciss.gui.PeakMeterGroup;
import de.sciss.gui.PeakMeterView;
import de.sciss.net.OSCBundle;
import de.sciss.net.OSCClient;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;

/**
 *	@version	0.59, 25-Feb-08
 *	@author		Hanns Holger Rutz
 */
public class PeakMeterManagerOLD
implements OSCListener, ActionListener
{
	private static final int	OSC_BUF_SIZE 		= 8192;

	private boolean				active				= false;
	private InetSocketAddress	addr				= null;
	private String				protocol;
	private int					index				= -1;
	private int					numChannels			= 0;
	private OSCClient			client				= null;
	
	private Object				sync				= new Object();

	private List				collClients			= new ArrayList();
	private final Map			mapClients			= new IdentityHashMap();	// maps PeakMeterGroup to Client

	private OSCBundle			meterBangBndl		= null;
	private final Timer			timer;
	private int					numTask				= 0;

	public PeakMeterManagerOLD()
	{
		timer	= new Timer( 33, this );
	}

	public void setServer( String hostName, int port, String protocol )
	{
		synchronized( sync ) {
			final boolean wasActive = isActive();
			if( wasActive ) stop();
			if( hostName.equals( "127.0.0.1" ) || hostName.equals( "localhost" )) {
				try {
					addr	= new InetSocketAddress( InetAddress.getLocalHost(), port );
//					addr	= new InetSocketAddress( "192.168.2.106", port );
				}
				catch( UnknownHostException e1 ) {
					addr	= new InetSocketAddress( hostName, port );
				}
			} else {
				addr		= new InetSocketAddress( hostName, port );
			}
//System.err.println( "host = " + hostName + " => " + addr.getHostName() );
			this.protocol	= protocol;
			if( wasActive ) start();
		}
	}

	public void addAndSetBus( PeakMeterView view, int nodeID, int newBusIndex )
	{
		final int		pmgChannels	= view.getNumChannels();
		final Client	mc;
		
		synchronized( sync ) {
			index			= newBusIndex;
			numChannels	   += view.getNumChannels();
			mc				= new Client( view, nodeID, pmgChannels, false );
// XXX
//			pmg.setSync( sync );
			collClients.add( mc );
			mapClients.put( view, mc );
			resortClients();
		}
	}
	
	public void removeAndSetBus( PeakMeterGroup pmg, int newBusIndex )
	{
		final Client	mc;

		synchronized( sync ) {
			collClients.remove( pmg );
			mc				= (Client) mapClients.remove( pmg );
			index			= newBusIndex;
			numChannels	   -= mc.numChannels;
			resortClients();
		}
	}
	
//	public void setActive( PeakMeterPanel view, boolean active )
//	{
//		setActive( view, active );
//	}
	
	public void setActive( PeakMeterView view, boolean active )
	{
		final Client mc;

		synchronized( sync ) {
			mc	= (Client) mapClients.get( view );
			if( mc.active != active ) {
				mc.active = active;
				if( active ) {
					mc.task = true;
					if( ++numTask == 1 ) start();
				}
			}
		}
	}
	
	// @synchronization	must be called with sync on sync
	private void resortClients()
	{
		if( !Thread.holdsLock( sync )) throw new IllegalMonitorStateException();

		Client mc;
		
		timer.stop();
		meterBangBndl	= new OSCBundle();
		numTask			= 0;
		meterBangBndl.addPacket( new OSCMessage( "/c_getn", new Object[] {
			new Integer( index ), new Integer( numChannels << 1 )}));
		for( int i = 0; i < collClients.size(); i++ ) {
			mc	= (Client) collClients.get( i );
			meterBangBndl.addPacket( new OSCMessage( "/n_set", new Object[] {
				new Integer( mc.nodeID ), "t_trig", new Integer( 1 )}));
			if( mc.task ) numTask++;
		}
//System.out.println( "resortClients. numTask = " + numTask );
		if( numTask > 0 ) {
			timer.restart();
		}
	}
	
	public void dispose()
	{
		synchronized( sync ) {
			collClients.clear();
			mapClients.clear();
			index	= -1;
			stop();
		}
	}

	// ------------- ActionListener interface -------------

	public void actionPerformed( ActionEvent e )
	{
		synchronized( sync ) {
			query();
		}
	}

	// ------------- OSCListener interface -------------
	
	public void messageReceived( OSCMessage msg, SocketAddress sender, long time )
	{
//		System.out.println( "got " + msg.getName() );
		
		if( !msg.getName().equals( "/c_setn" )) return;
		
		final int	busIndex	= ((Number) msg.getArg( 0 )).intValue();
		final int	numVals		= ((Number) msg.getArg( 1 )).intValue();
		Client		mc;
		
		synchronized( sync ) {
			if( (busIndex != index) || (numVals != (numChannels << 1)) ) {
//				System.out.println( "not for us: busIndex " + busIndex + "; index " + index + "; numVals " + numVals + "; numChannels " + numChannels );
				return;
			}
			
//			System.out.println( "here " + collClients.size() );

			numTask = 0;
			for( int i = 0, off = index + 2; i < collClients.size(); i++ ) {
				mc	= (Client) collClients.get( i );
				System.out.println( "i " + i + "; mc.task " + mc.task );
				if( !mc.task ) {
					off += 2;
					continue;
				}
				for( int k = 0; k < mc.peakRMSPairs.length; ) {
					mc.peakRMSPairs[ k++ ] = ((Number) msg.getArg( off++ )).floatValue();
					mc.peakRMSPairs[ k++ ] = ((Number) msg.getArg( off++ )).floatValue();
				}
//				if( mc.pmg.meterUpdate( mc.peakRMSPairs ) || mc.active ) {
				if( mc.view.meterUpdate( mc.peakRMSPairs, 0, System.currentTimeMillis() ) || mc.active ) {
					numTask++;
				} else {
					mc.task = false;
				}
			}
//			System.out.println( "numTask now " + numTask );
			if( numTask == 0 ) {
				timer.stop();
			}
		}
	}

	private void start()
	{
		synchronized( sync ) {
			if( !active ) {
				if( addr == null ) {
					throw new IllegalStateException( "Server has not been specified" );
				}
				try {
					client	= OSCClient.newUsing( protocol );
					client.setBufferSize( OSC_BUF_SIZE );
//					if( size > 0 ) {
//						setCustomDecoder();
//					}
					client.setTarget( addr );
			        client.start();
			        client.addOSCListener( this );
			        active = true;
				}
				catch( IOException e1 ) {
					if( client != null ) {
						client.removeOSCListener( this );
						client.dispose();
						client = null;
					}
					meterBangBndl	= null;
					active			= false;
					
					System.out.println( e1 );
				}
			}
			if( active && (numTask > 0) ) {
				query();
				timer.restart();
			}
		}
	}
	
	public void stop()
	{
		synchronized( sync ) {
			if( active ) {
				timer.stop();
//System.out.println(" stopped ");
				client.removeOSCListener( this );
				meterBangBndl = null;
				try {
					client.stop();
				}
				catch( IOException e1 ) {
					System.out.println( e1 );
				}
				client.dispose();
				client		= null;
				active		= false;
			}
		}
	}
	
	public boolean isActive()
	{
		return active;
	}

	// ------------- private methods -------------

	// sync: caller must be in synchronized( sync ) block!!
	private void query()
	{
//System.out.println( "query " + (meterBangBndl != null) );
		if( meterBangBndl != null ) {
			try {
				client.send( meterBangBndl );
			}
			catch( IOException e1 ) {
				System.out.println( e1 );
			}
		}
	}

//	// @synchronization	must be called with sync on sync
//	private void disposeServer()
//	{
//		MeterClient mc;
//
//		meterTimer.stop();
//		
//		if( resp != null ) {
//			try {
//				resp.remove();
//			}
//			catch( IOException e1 ) {
//				printError( "disposeServer", e1 );
//			}
//		}
//	
//		if( bus != null ) {
//			bus.free();
//			bus = null;
//		}
//		
//		if( server == null ) return;
//		
//		for( int i = 0; i < collAllClients.size(); ) {
//			mc = (MeterClient) collAllClients.get( i );
//			if( mc.server == server ) {
//				collAllClients.remove( i );
//			} else {
//				i++;
//			}
//		}
//
//		collActiveClients.clear();
//		server			= null;
//		meterBangBndl	= null;
//	}

	// ------------- internal classes -------------

//	public static interface Listener
//	{
//		public void meterUpdate( float[] peakRMSPairs );
//	}

	private static class Client
	{
		protected final float[]			peakRMSPairs;
		protected final PeakMeterView	view;
		protected final int				nodeID;
		protected final int				numChannels;
		protected boolean				active;
		protected boolean				task;
		
//		private MeterClient( Listener ml, int[] channels, boolean task )
		protected Client( PeakMeterView view, int nodeID, int numChannels, boolean active )
		{
//			this.ml				= ml;
			this.view			= view;
			this.nodeID			= nodeID;
			this.numChannels	= numChannels;
			this.active			= active;
			task				= active;

			peakRMSPairs		= new float[ numChannels << 1 ];
		}
		
//		private void setOffset( int cOffset )
//		{
//			this.cOffset	= cOffset;
//		}
//		
//		public String toString()
//		{
//			final StringBuffer sb = new StringBuffer();
//			sb.append( "[ " );
//			for( int i = 0; i < channels.length; i++ ) {
//				if( i > 0 ) sb.append( ", " );
//				sb.append( channels[ i ]);
//			}
//			sb.append( " ]" );
//			return( "MeterClient( "+ml+", "+server+", " + sb.toString() + ", " + g + ", " + task + " )" );
//		}
	}
}
