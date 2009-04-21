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
 *	@version		0.61, 21-Apr-09
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
		var numChannels;
		if( b != bus, {
			// if( (bus.server != b.server) or: { b.numChannels != bus.numChannels }, { ... });
			this.prUnregister;
			if( b.notNil, {
				if( group.notNil and: { group.server != b.server }, {
					Error( "Bus and Group cannot be on different servers" ).throw;
				});
				numChannels	= b.numChannels;
				nodeID		= Array.fill( numChannels, { b.server.nextNodeID }).first;
			}, {
				numChannels 	= 0;
				nodeID		= -1;
			});
			bus		= b;
			ctrlBus	= Bus.control( bus.server, numChannels << 1 );
			server.sendMsg( '/set', this.id, \numChannels, numChannels );
			this.prRegister;
		});
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
