/*
 *	JSCViews collection 4
 *	(SwingOSC classes for SuperCollider)
 *
 *	Copyright (c) 2005-2009 Hanns Holger Rutz. All rights reserved.
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
 *	Changelog:
 */

/**
 *	@version		0.61, 22-Apr-09
 *	@author		Hanns Holger Rutz
 */
JPeakMeterManager {
	classvar all;			// IdentityDictionary mapping JSCSynth to JPeakMeterManager

	var <id;				// server side manager id
	
	var jscsynth, numViews = 0;
	
	// ----------------- quasi-constructor -----------------

	*newFrom { arg swing, scsynth;
		var res, jscsynth;
		
		jscsynth = JSCSynth.newFrom( swing, scsynth );
		if( all.isNil, {
			all	= IdentityDictionary.new;
		});
		res = all[ jscsynth ];
		if( res.isNil, {
			res = this.new( jscsynth );
			all[ jscsynth ] = res;
		});
		^res;
	}
	
	// ----------------- constructor -----------------

	*new { arg jscsynth;
		^super.new.prInit( jscsynth );
	}

	// ----------------- private instance methods -----------------

	prInit { arg argJSCSynth;
//		views		= IdentityDictionary.new;
		jscsynth		= argJSCSynth;
		id			= jscsynth.swing.nextNodeID;
		jscsynth.swing.listSendMsg([ '/method',
			'[', '/local', id, '[', '/new', "de.sciss.swingosc.PeakMeterManager", ']', ']',
			\setServer ] ++ jscsynth.asSwingArg );
	}
	
	protRegister { arg view;
		var ctrlBus;
		ctrlBus	= view.protGetCtrlBus;
//[ "protRegister", ctrlBus, view.bus, view.group ].postln;
		jscsynth.swing.listSendMsg([ '/method', this.id, \addListener ] ++ view.asSwingArg ++
			[ '[', '/new', "de.sciss.jcollider.Bus" ] ++ jscsynth.asSwingArg ++
			[ view.bus.rate, view.bus.index, view.bus.numChannels, ']' ] ++
			[ '[', '/method', "de.sciss.jcollider.Group", \basicNew ] ++ jscsynth.asSwingArg ++ [ view.group.nodeID, ']' ] ++
			[ '[', '/new', "de.sciss.jcollider.Bus" ] ++ jscsynth.asSwingArg ++
			[ ctrlBus.rate, ctrlBus.index, ctrlBus.numChannels, ']', view.active, view.protGetNodeID ]);
		numViews = numViews + 1;
	}

	protUnregister { arg view;
		jscsynth.swing.listSendMsg([ '/method', this.id, \removeListener ] ++ view.asSwingArg );
		numViews = numViews - 1;
		if( numViews == 0, {
			this.prDispose;
		});
	}
	
	prDispose {
		all.removeAt( jscsynth );
		jscsynth.swing.sendBundle( nil, [ '/method', this.id, \dispose ], [ '/free', this.id ]);
		jscsynth		= nil;
	}
	
	protSetActive { arg view, active;
		jscsynth.swing.listSendMsg([ '/method', this.id, \setListenerTask ] ++ view.asSwingArg ++ [ active ]);
	}
}

JSCPeakMeter : JSCControlView {
	var <bus, <group, manager;
	var <active = true;
	var <border = false, <caption = false, <captionVisible = true, <captionPosition = \left;
	var <rmsPainted = true, <holdPainted = true;
//	var acResp;	// OSCpathResponder for action listening
	var weCreatedGroup = false;
	var ctrlBus, nodeID;
	var <numChannels = 0;

	// ----------------- public instance methods -----------------

	active_ { arg bool;
		if( bool != active, {
			active = bool;
			if( manager.notNil, {
				manager.protSetActive( this, active );
			});
		});
	}
	
	border_ { arg bool;
		if( bool != border, {
			border = bool;
			this.setProperty( \border, border );
		});
	}
	
	caption_ { arg bool;
		if( bool != caption, {
			caption = bool;
			this.setProperty( \caption, caption );
		});
	}

	captionVisible_ { arg bool;
		if( bool != captionVisible, {
			captionVisible = bool;
			this.setProperty( \captionVisible, captionVisible );
		});
	}

	captionPosition_ { arg value;
		if( value != captionPosition, {
			captionPosition = value;
			this.setProperty( \captionPosition, captionPosition );
		});
	}
	
	rmsPainted_ { arg bool;
		if( bool != rmsPainted, {
			rmsPainted = bool;
			this.setProperty( \rmsPainted, rmsPainted );
		});
	}

	holdPainted_ { arg bool;
		if( bool != holdPainted, {
			holdPainted = bool;
			this.setProperty( \holdPainted, holdPainted );
		});
	}

	font { ^this.getProperty( \font )}
	font_ { arg argFont;
		this.setProperty( \font, argFont );
	}

	group_ { arg g;
		if( g != group, {
			this.prUnregister;
			if( g.notNil and: { bus.notNil and: { g.server != bus.server }}, {
				Error( "Bus and Group cannot be on different servers" ).throw;
			});
			group = g;
			this.prRegister;
		});
	}
	
	bus_ { arg b;
		if( b != bus, {
			// if( (bus.server != b.server) or: { b.numChannels != bus.numChannels }, { ... });
			this.prUnregister;
			if( b.notNil, {
				if( group.notNil and: { group.server != b.server }, {
					Error( "Bus and Group cannot be on different servers" ).throw;
				});
				numChannels	= b.numChannels;
				nodeID		= Array.fill( numChannels, { b.server.nextNodeID }).first;
				ctrlBus		= Bus.control( b.server, numChannels << 1 );
				server.sendMsg( '/set', this.id, \numChannels, numChannels );
				bus			= b;
				this.prRegister;
			}, {
//				numChannels 	= 0;
				nodeID		= -1;
				bus			= nil;
			});
		});
	}
	
	numChannels_ { arg ch;
		if( ch != numChannels, {
			if( bus.notNil, {
				Error( "Cannot change numChannels when bus is set" ).throw;
			});
			numChannels = ch;
			server.sendMsg( '/set', this.id, \numChannels, numChannels );
		});
	}

	// ----------------- public class methods -----------------
	
	*meterServer { arg server;
		var win, inBus, outBus, fntSmall, viewWidth, inMeterWidth, outMeterWidth, inMeter, outMeter,
		    inGroup, outGroup, chanWidth = 13, meterHeight = 200, fLab, fBooted, numIn, numOut, fPeriod;

		numIn		= server.options.numOutputBusChannels;
		numOut		= server.options.numInputBusChannels;
		inMeterWidth	= numIn * chanWidth + 29;
		outMeterWidth	= numOut * chanWidth + 29;
		viewWidth		= inMeterWidth + outMeterWidth + 11;

	    win		= JSCWindow( server.name ++ " levels (dBFS)", Rect( 5, 305, viewWidth, meterHeight + 26 ), false );
	    inMeter	= JSCPeakMeter( win, Rect( 4, 4, inMeterWidth, meterHeight ))
	    	.border_( true ).caption_( true );
//	    	.numChannels_( inGroup.numChannels );
//	    	.group_( inGroup )
//	    	.bus_( inBus );
	    outMeter	= JSCPeakMeter( win, Rect( inMeterWidth + 8, 4, outMeterWidth, meterHeight ))
	    	.border_( true ).caption_( true );
//	    	.numChannels_( outGroup.numChannels );
//	    	.group_( outGroup )
//	    	.bus_( outBus );
	    	
	    	fntSmall = JFont( "Helvetica", 8 );
	    	
	    	fLab = { arg name, numChannels, xOff; var comp;
	  		comp = JSCCompositeView( win, Rect( xOff, meterHeight + 4, numChannels * chanWidth + 28, 18 ))
	  			.background_( Color.black );
	  		JSCStaticText( comp, Rect( 0, 0, 22, 18 ))
	  			.align_( \right ).font_( fntSmall ).stringColor_( Color.white ).string_( name );
	  		numChannels.do({ arg ch;
		  		JSCStaticText( comp, Rect( 21 + (ch * chanWidth), 0, 20, 18 ))
		  			.align_( \center ).font_( fntSmall ).stringColor_( Color.white ).string_( ch.asString );
	  		});
	    	};
	    	
	    	fLab.value( "in", numIn, 4 );
	    	fLab.value( "out", numOut, 8 + inMeterWidth );
	    	
	    	fBooted = {
//	    		"-----------Yo".postln;
			inGroup			= Group.head( RootNode( server ));
			outGroup			= Group.tail( RootNode( server ));
			outBus			= Bus( \audio, 0, server.options.numOutputBusChannels, server );
			inBus			= Bus( \audio, outBus.numChannels, server.options.numInputBusChannels, server );
			inMeter.group		= inGroup;
			inMeter.bus		= inBus;
			outMeter.group	= outGroup;
			outMeter.bus		= outBus;
	    	};
	    	
	    	fPeriod = {
	    		inMeter.bus		= nil;
	    		inMeter.group		= nil;
	    		outMeter.bus		= nil;
	    		outMeter.group	= nil;
	    	};
	    	
		win.front;

		win.onClose_({
			ServerTree.remove( fBooted );
			CmdPeriod.remove( fPeriod );
			ServerQuit.remove( fPeriod );
			inGroup.free; inGroup = nil;
			outGroup.free; outGroup = nil;
		});

		ServerTree.add( fBooted );
		CmdPeriod.add( fPeriod );
		ServerQuit.add( fPeriod );
		if( server.serverRunning, fBooted ); // otherwise starts when booted
	}
	
	// ----------------- private instance methods -----------------

	protGetCtrlBus { ^ctrlBus }
	protGetNodeID { ^nodeID }

	prClose { arg preMsg, postMsg;
		this.prUnregister;
		^super.prClose( preMsg, postMsg );
	}

	prInitView {
		^this.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.gui.PeakMeterPanel", ']' ]
		]);
	}
	
	prUnregister {
		if( manager.notNil, {
			manager.protUnregister( this );
			manager = nil;
		});
		if( weCreatedGroup, {
			group.free;
			group = nil;
			weCreatedGroup = false;
		});
		ctrlBus.free;
		ctrlBus = nil;
	}
	
	prRegister {
		if( bus.notNil and: { bus.numChannels > 0 }, {
			if( group.isNil, {
//				group			= Group.tail( bus.server );
				group			= Group.tail( RootNode( bus.server ));
				weCreatedGroup	= true;
			});
			manager		= JPeakMeterManager.newFrom( this.server, bus.server );
			manager.protRegister( this );
		});
	}
	
	prSendProperty { arg key, value;
		key	= key.asSymbol;

		// fix keys
		case { key === \captionPosition }
		{
			switch( value,
			\left,   { value = 2 },
			\right,  { value = 4 },
			\center, { value = 0 }
			);
		}
		{ key === \rmsPainted }
		{
			key = \rMSPainted;
		};
		^super.prSendProperty( key, value );
	}
}
