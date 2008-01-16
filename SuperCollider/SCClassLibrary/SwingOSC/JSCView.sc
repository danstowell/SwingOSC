/*
 *	JSCView
 *	(SwingOSC classes for SuperCollider)
 *
 *	Copyright (c) 2005-2008 Hanns Holger Rutz. All rights reserved.
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
 *	- JSCDragView : added interpretDroppedStrings feature
 *	- JSCCompositeView coordinates fixed
 *	- JSCCompositeView is not opaque any more ; added opaque method
 *	- added JSCMultiSliderView ; fixed JSCH/VLayoutView
 *	- added resize functionality, fixed container views,
 *	  lots of bug fixes, fixed mouse-over behaviour, fixed opacities,
 *	  added envelope view, performance improvements
 *	- argServer argument removed, since server needs to be same as parent view!
 */

/**
 *	For details, see JSCView.html and DeveloperInfo.html
 *
 *	@version		0.57, 16-Jan-08
 *	@author		Hanns Holger Rutz
 *
 *	@todo		should invoke custom dispose() methods on java gadgets
 *	@todo		need to check all default properties are readable
 */
JSCView {  // abstract class
	classvar <>currentDrag, <>currentDragString;
	classvar <>globalKeyDownAction, <>globalKeyUpAction;

	classvar fakeModifiers	= 0;	// reflects caps lock
	classvar unicodeMap;

	var dataptr, <parent, <>action, <background;
	var <mouseDownAction, <mouseUpAction, <mouseOverAction, <mouseMoveAction;	var <>keyDownAction, <>keyUpAction, <>keyTyped;	var <beginDragAction,<>canReceiveDragHandler,<receiveDragHandler;
	var <>onClose;

	var <server;	// the SwingOSC server used for this view
	var properties;
	var keyResp, dndResp, mouseResp, cmpResp;
	var <hasFocus = false;
	var <id;
	var <visible = true;
	
	var clpseMouseMove, clpseMouseDrag;

	var jinsets, scBounds, <jBounds, <allVisible;
	
	*initClass {
		unicodeMap = IdentityDictionary.new;
		// backspace and enter
		[ 8, 127, 10, 13,	
		// arrow keys and function keys
		  33, 63276, 34, 63277, 35, 63275, 36, 63273, 37, 63234, 38, 63232, 39, 63235, 40, 63233 ]
		.pairsDo({ arg key, value;
			unicodeMap.put( key, value );
		});
	}
	
	// ----------------- constructor -----------------

	/**
	 *	Creates a new instance of this view. The 'id'
	 *	argument is only used in SwingOSC, not in cocoa GUI. Usually
	 *	you leave it blank.
	 *
	 *	@param	parent	(JSCView or JSCWindow) container or window that is the parent of this component
	 *	@param	bounds	(Rect) the bounds of this component
	 *	@param	id		(Integer) the SwingOSC node id or 'nil' to automatically create one
	 */
	*new { arg parent, bounds, id;
		^super.new.init( parent, bounds, id );
	}
	
	// ----------------- public class methods -----------------

	*viewClass { ^this }
	
	*paletteExample { arg parent, bounds;
		^this.new( parent, bounds );
	}
	
	// ----------------- public instance methods -----------------

	asView { ^this }
	
	bounds {
		var pb;
		if( scBounds.isNil, {
			// need to revalidate bounds
			pb		= parent.prGetJInsets.subtractFrom( parent.bounds );  // note: this recursively calls bounds
			scBounds = jinsets.addTo( jBounds.moveBy( pb.left, pb.top ));
		});
		^scBounds.copy;
	}

	bounds_ { arg rect;
		var argBounds;
		jBounds	= this.prBoundsToJava( rect );
		argBounds	= jBounds.asSwingArg;
		if( this.prIsInsideContainer, {
			server.sendBundle( nil, [ '/set', this.id, \bounds ] ++ argBounds,
						          [ '/set', "cn" ++ this.id, \bounds ] ++ argBounds );
		}, {
			server.listSendMsg(['/set', this.id, \bounds ] ++ argBounds );
		});
		this.prInvalidateBounds;
		scBounds = rect.copy;
		// XXX CompositeView must move its children!!! sucky cocoa!!!
	}
		
//	visible { ^this.getProperty( \visible )}

	visible_ { arg bool;
//		this.setProperty( \visible, bool )}
		if( visible != bool, {
			visible = bool;	// must be set before calling prVisiblityChange
			this.prInvalidateAllVisible;
			this.prVisibilityChange;
			server.sendMsg( '/set', if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id }),
				\visible, visible );
		});
	}
	
	enabled { ^this.getProperty( \enabled )}

	enabled_ { arg bool; this.setProperty( \enabled, bool )}
	
	canFocus { ^this.getProperty( \canFocus )}
	
	canFocus_ { arg bool; this.setProperty( \canFocus, bool )}

	focus { arg flag = true;
		if( flag, {
			server.sendMsg( '/method', this.id, \requestFocus );
		}, {
			this.prGetWindow.view.focus;
		});
	}

//	id { ^this.getProperty( \id )}
//
//	id_ { arg id; this.setProperty( \id, id )}

	opaque { ^this.getProperty( \opaque )}
	
	opaque_ { arg bool; this.setProperty( \opaque, bool )}
	
	refresh {
		server.sendBundle( nil,
			[ '/method', this.id, \revalidate ], [ '/method', this.id, \repaint ]);
	}

	findByID { arg id;
		"JSCView.findByID : not yet implemented".error;
		^nil;
	}
	
	isClosed { ^dataptr.isNil }
	
	notClosed { ^dataptr.notNil }
	
	remove {
		if( dataptr.notNil, {
			parent.prRemoveChild( this );
//			this.prRemove;
			this.prClose;
		}, {
			"JSCView-remove : this view was already removed.".debug( this );
		});
	}

	resize { ^this.getProperty( \resize )}

	resize_ { arg resize; this.setProperty( \resize, resize )}
	
	background_ { arg color;
		background = color;
		this.setProperty( \background, color );
	}

	addAction { arg func, selector = \action;
		this.perform( selector.asSetter, this.perform( selector ).addFunc( func ));
	}

	removeAction { arg func, selector=\action;
		this.perform( selector.asSetter, this.perform( selector ).removeFunc( func ));
	}

	mouseDownAction_ { arg func;
		if( func.notNil && mouseResp.isNil, { this.prCreateMouseResponder });
		mouseDownAction = func;
	}

	mouseUpAction_ { arg func;
		if( func.notNil && mouseResp.isNil, { this.prCreateMouseResponder });
		mouseUpAction = func;
	}
	
	mouseOverAction_ { arg func;
		if( func.notNil && mouseResp.isNil, { this.prCreateMouseResponder });
		mouseOverAction = func;
	}
	
	mouseMoveAction_ { arg func;
		if( func.notNil && mouseResp.isNil, { this.prCreateMouseResponder });
		mouseMoveAction = func;
	}

	beginDragAction_ { arg func;
		if( func.notNil && dndResp.isNil, { this.prCreateDnDResponder });
		beginDragAction = func;
	}
	
	receiveDragHandler_ { arg func;
		if( func.notNil && dndResp.isNil, { this.prCreateDnDResponder });
		receiveDragHandler = func;
	}
	
	defaultKeyDownAction { ^nil }
	defaultKeyUpAction { ^nil }
	
	defaultGetDrag { ^nil }
	
	canReceiveDrag {
		^if( canReceiveDragHandler.notNil, { canReceiveDragHandler.value( this )}, { this.defaultCanReceiveDrag });
	}
	
	defaultCanReceiveDrag { ^false }
	
	// get the view parent tree up to the JSCTopView
	getParents {
		var parents, view;
		view    = this;
		parents = List.new;
		while({ (view = view.parent).notNil }, { parents.add( view )});
		^parents;
	}
		
	doAction { action.value( this )}
	
	properties {
		^#[ \enabled, \canFocus, \resize, \background,
		    \minWidth, \maxWidth, \minHeight, \maxHeight, \opaque ] // JJJ: opaque, no bounds ; no visible
	}

	getPropertyList {
		^this.properties.collect({ arg name; [ name, this.perform( name )]});
	}
	
	setPropertyList { arg list;
		list.do({ arg item;
			var name, value;
			#name, value = item;
			this.perform( name.asSetter, value );
		});
	}
	
	// ----------------- private instance methods -----------------

	init { arg argParent, argBounds, id;
		parent = argParent.asView;	// actual view
		this.prInit( parent, argBounds.asRect, this.class.viewClass, parent.server, id );
		argParent.add( this );		// maybe window or viewadapter
	}
	
	// used by JSCPlugView and JSCPlugContainerView
	*prBasicNew {
		^super.new;
	}	

	prInit { arg argParent, argBounds, argViewClass, argServer, argID;
		server		= argServer; // ?? { argParent.server; };
		properties	= IdentityDictionary.new;
//		properties.put( \bounds, argBounds );
		scBounds		= argBounds;
//		properties.put( \visible, true );
		properties.put( \enabled, true );
		properties.put( \canFocus, true );
		properties.put( \resize, 1 );
//		this.id		= argID ?? { server.nextNodeID; };
//		properties.put( \id, argID ?? { server.nextNodeID });
		id			= argID ?? { server.nextNodeID };
		dataptr		= id;

		^this.prSCViewNew;
	}
	
	prClose { arg preMsg, postMsg;
		var bndl;
		
		// nil.remove is allowed
		keyResp.remove;
		dndResp.remove;
		mouseResp.remove;
		if( clpseMouseMove.notNil, { clpseMouseMove.cancel; clpseMouseMove = nil });
		if( clpseMouseDrag.notNil, { clpseMouseDrag.cancel; clpseMouseDrag = nil });
		cmpResp.remove;
		
		bndl = Array( preMsg.size + postMsg.size + 5 );
		bndl.addAll( preMsg );
		bndl.add([ '/method', "key" ++ this.id, \remove ]);
		bndl.add([ '/method', "cmp" ++ this.id, \remove ]);
		if( dndResp.notNil, { bndl.add([ '/method', "dnd" ++ this.id, \remove ]);});
		if( mouseResp.notNil, { bndl.add([ '/method', "mse" ++ this.id, \remove ]);});
		bndl.add([ '/free', "key" ++ this.id, "cmp" ++ this.id, this.id ] ++
			dndResp.notNil.if([ "dnd" ++ this.id ]) ++
			mouseResp.notNil.if([ "mse" ++ this.id ]) ++
			this.prIsInsideContainer.if([ "cn" ++ this.id ]);
		);
		bndl.addAll( postMsg );
		server.listSendBundle( nil, bndl );

		dataptr = nil;
		onClose.value(this);
	}

	prSCViewNew { arg preMsg, postMsg;
		var bndl, argBounds;
		
		if( jinsets.isNil, { jinsets = Insets.new });
		
		bndl			= List.new;
		bndl.addAll( preMsg );
		jBounds		= this.prBoundsToJava( scBounds );
		argBounds		= jBounds.asSwingArg;
		bndl.add([ '/set', this.id, \bounds ] ++ argBounds ++ [ \font, '[', '/ref', \font, ']' ]);
		if( this.prIsInsideContainer, {
			bndl.add([ '/set', "cn" ++ this.id, \bounds ] ++ argBounds );
		});
		if( this.prNeedsTransferHandler, {
			this.prCreateDnDResponder( bndl );
		});
		// NOTE: for global key actions to be working, every view
		// has to create a key responder, even if it's not using it personally ;-C
		this.prCreateKeyResponder( bndl );
		this.prCreateCompResponder( bndl );
		bndl.addAll( postMsg );
		server.listSendBundle( nil, bndl );
	}

	prGetWindow { ^parent.prGetWindow }

	prCreateDnDResponder { arg bndl;
		var msg;
	
		if( dndResp.notNil, {
			"JSCView.prCreateDnDResponder : already created!".warn;
			^nil;
		});
		dndResp = OSCpathResponder( server.addr, [ '/transfer', this.id ], { arg time, resp, msg;
			var state;
			
			state = msg[2].asSymbol;
			case { state === \export }
			{
				this.beginDrag;
				if( currentDrag.notNil, {
					server.listSendMsg([ '/set', "dnd" ++ this.id, \string ] ++ currentDrag.asString.asSwingArg );
				}, {
					server.sendMsg( '/set', "dnd" ++ this.id, \string, '[', '/ref', \null, ']' );
				});
			}
			{ state === \import }
			{
				if( msg[3].asSymbol === \string, {
					currentDrag = msg[4].asString;
					this.prImportDrag;	// compile it just as in cocoa sc
				});
				if( this.canReceiveDrag, {
					this.receiveDrag;
				});
			};
		});
		dndResp.add;
		msg = [ '/local', "dnd" ++ this.id,
				'[', '/new', "de.sciss.swingosc.DummyTransferHandler", this.id, this.prGetDnDModifiers, ']' ];
		if( bndl.notNil, {
			bndl.add( msg );
		}, {
			server.sendBundle( nil, msg );
		});
	}

	prCreateKeyResponder { arg bndl;
		var msg;
	
		if( keyResp.notNil, {
			"JSCView.prCreateKeyResponder : already created!".warn;
			^nil;
		});
		keyResp = OSCpathResponder( server.addr, [ '/key', this.id ], { arg time, resp, msg;
			var char, state, propagate, unicode, modifiers, plusMod, keyCode;
		
			state = msg[2].asSymbol;
			if( state !== \typed, {
				keyCode		= msg[3];
				unicode		= msg[4];
				modifiers		= msg[5];
				propagate		= unicode != 0xFFFF;
				plusMod		= fakeModifiers;
				if( propagate.not, {
					case { (keyCode >= 33) && (keyCode <= 40) } // arrow keys + page up/dn, home
					{
						plusMod   = plusMod | 0x800000; // 0x900000;
						unicode   = unicodeMap.at( keyCode );
						propagate = true;
					}
					{ (keyCode >= 112) && (keyCode <= 123) } // F1 ... F12
					{
						plusMod   = plusMod | 0x800000;
						unicode   = keyCode + 63124;
						propagate = true;
					}
					{ (keyCode >= 96) && (keyCode <= 111) } // numpad
					{
						plusMod   = plusMod | 0x200000;
						propagate = true;
					}
					{ keyCode == 20 } // caps lock
					{
						// on/off is reflected thru keyPressed/keyReleased
						fakeModifiers = if( state === \pressed, 								fakeModifiers | 0x10000,
							fakeModifiers & 0x80FFF
						);
					};
				}, {
					unicode	= unicodeMap.atFail( keyCode, unicode );
				});
				if( propagate, {
					// java->cocoa ; this translates shift (1), ctrl (2), cmd (4), alt (8)
					modifiers		= ((modifiers & 3) << 17) |
								  ((modifiers & 4) << 18) |
								  ((modifiers & 8) << 16) | plusMod;
					char			= unicode.asAscii;
					if( state === \pressed, {
						{ this.keyDown( char, modifiers, unicode, keyCode );}.defer;
					}, { // "released
						{ this.keyUp( char, modifiers, unicode, keyCode );}.defer;
					});
				});
			});
		});
		keyResp.add;
		msg = [ '/local', "key" ++ this.id, '[', '/new', "de.sciss.swingosc.KeyResponder", this.id, ']' ];
		if( bndl.notNil, {
			bndl.add( msg );
		}, {
			server.sendBundle( nil, msg );
		});
	}

	keyDown { arg char, modifiers, unicode,keycode;
		globalKeyDownAction.value( this, char, modifiers, unicode, keycode ); 
		this.handleKeyDownBubbling( this, char, modifiers, unicode, keycode );
	}
	
	keyUp { arg char, modifiers, unicode,keycode; 
		this.keyTyped = char;
		// always call global keydown action first
		globalKeyUpAction.value( this, char, modifiers, unicode, keycode );
		this.handleKeyUpBubbling( this, char, modifiers, unicode, keycode );
	}
	
	handleKeyDownBubbling { arg view, char, modifiers, unicode, keycode;
		var result;
		// nil from keyDownAction --> pass it on
		if( keyDownAction.isNil, {
			this.defaultKeyDownAction( char,modifiers,unicode,keycode );
			result = nil;
		}, {
			result = keyDownAction.value( view, char, modifiers, unicode, keycode );
		});
		if( result.isNil, {  
			// call keydown action of parent view
			parent.handleKeyDownBubbling( view, char, modifiers, unicode, keycode );
		});
	}
	
	handleKeyUpBubbling { arg view, char, modifiers, unicode, keycode;
		var result;
		// nil from keyDownAction --> pass it on
		if( keyUpAction.isNil, {
			this.defaultKeyUpAction( char,modifiers,unicode,keycode );
			result = nil;
		}, {
			result = keyUpAction.value( view, char, modifiers, unicode, keycode );
		});
		if( result.isNil, {  
			// call keydown action of parent view
			parent.handleKeyUpBubbling( view, char, modifiers, unicode, keycode );
		});
	}

//	prSetScBounds { arg rect; scBounds = rect }
	prGetJInsets { ^jinsets }

	// subclasses can override this to do special refreshes
	prBoundsUpdated {}

	prCreateCompResponder { arg bndl;
		var msg;
	
		if( cmpResp.notNil, {
			"JSCView.prCreateCompResponder : already created!".warn;
			^nil;
		});
		cmpResp = OSCpathResponder( server.addr, [ '/component', this.id ], { arg time, resp, msg;
			var state, x, y, w, h, dx, dy, dw, dh, temp;
//			var scBounds;
		
			state = msg[2].asSymbol;
//			case
//			{ (state === \moved) || (state === \resized) }
//			{
//				this.prUpdateBounds( this.prBoundsFromJava( Rect( msg[3], msg[4], msg[5], msg[6] )));
//			}
			switch( state, 
			\resized, {
				w			= msg[5];
				h			= msg[6];
				dw		 	= w - jBounds.width;
				dh 			= h - jBounds.height;
//[ "w", w, "h", h, "dw", dw, "dh", dh, "jBounds", jBounds, "scBounds", scBounds ].postln;
				jBounds.width	= w;
				jBounds.height= h;
				if( scBounds.notNil, {
					scBounds.width	= scBounds.width + dw;
					scBounds.height	= scBounds.height + dh;
				});
//[ "--> jBounds", jBounds, "scBounds", scBounds ].postln;
				this.prBoundsUpdated;
			},
			\moved, {
				x			= msg[3];
				y			= msg[4];
				dx		 	= x - jBounds.left;
				dy 			= y - jBounds.top;
				jBounds.left	= x;
				jBounds.top	= y;
				temp			= scBounds;	// save it since prInvalidateBounds deletes it!
				if( temp.notNil, {
					temp.left	= temp.left + dx;
					temp.top	= temp.top + dy;
				});
				this.prInvalidateBounds;
				scBounds		= temp;
				this.prBoundsUpdated;
			},
			\gainedFocus, {
				hasFocus = true;
				this.prFocusChange;
			},
			\lostFocus, {
				hasFocus = false;
				this.prFocusChange;
			});
		});
		cmpResp.add;
		msg = [ '/local', "cmp" ++ this.id, '[', '/new', "de.sciss.swingosc.ComponentResponder", this.id, ']' ];
		if( bndl.notNil, {
			bndl.add( msg );
		}, {
			server.sendBundle( nil, msg );
		});
	}
	
	prInvalidateBounds { scBounds = nil }
	prInvalidateAllVisible { allVisible = nil }

	// subclasses can override this to invoke special refreshes
	prFocusChange {}
	
	// subclasses can override this to invoke special refreshes
	// ; this is called _before_ the visibility message is sent out!
	prVisibilityChange {}

	prCreateMouseResponder { arg bndl;
		var msg, win;
	
		if( mouseResp.notNil, {
			"JSCView.prCreateMouseResponder : already created!".warn;
			^nil;
		});
		clpseMouseMove	= Collapse({ arg x, y, modifiers; this.mouseOver( x, y, modifiers )});
		clpseMouseDrag	= Collapse({ arg x, y, modifiers; this.mouseMove( x, y, modifiers )});
		mouseResp			= OSCpathResponder( server.addr, [ '/mouse', this.id ], { arg time, resp, msg;
			var state, x, y, modifiers, button, clickCount;
		
			// [ '/mouse', id, state, x, y, modifiers, button, clickCount ]
			state 		= msg[2].asSymbol;
			x			= msg[3];
			y			= msg[4];
			modifiers		= msg[5];

			// java->cocoa ; this translates shift (1), ctrl (2), cmd (4), alt (8)
			modifiers		= ((modifiers & 3) << 17) |
						  ((modifiers & 4) << 18) |
						  ((modifiers & 8) << 16); // | plusMod;

			case { state === \pressed }
			{
				button		= msg[6];
				clickCount	= msg[7];
				{ this.mouseDown( x, y, modifiers, button, clickCount )}.defer;
			}
			{ state === \released }
			{
				{ this.mouseUp( x, y, modifiers )}.defer;
			}
			{ state === \moved }
			{
				clpseMouseMove.instantaneous( x, y, modifiers );
			}
			{ state === \dragged }
			{
				clpseMouseDrag.instantaneous( x, y, modifiers );
			};
// note: entered is followed by moved with equal coordinates
// so we can just ignore it
//			{ state === \entered }
//			{
//				{ this.mouseOver( x, y, modifiers )}.defer;
//			};
		});
		mouseResp.add;
		msg = [ '/local', "mse" ++ this.id, '[', '/new', "de.sciss.swingosc.MouseResponder", this.id, true, this.prGetWindow.id, ']' ];
		if( bndl.notNil, {
			bndl.add( msg );
		}, {
			server.sendBundle( nil, msg );
		});
	}

	mouseDown { arg x, y, modifiers, buttonNumber, clickCount;
		mouseDownAction.value( this, x, y, modifiers, buttonNumber, clickCount );
	}
	
	mouseUp { arg x, y, modifiers;
		mouseUpAction.value( this, x, y, modifiers );
	}
	
	mouseMove { arg x, y, modifiers;
		mouseMoveAction.value( this, x, y, modifiers );
	}
	
	mouseOver { arg x, y, modifiers;
		mouseOverAction.value( this, x, y, modifiers );
	}
	
//	prRemove { }

	prIsInsideContainer { ^false }	// default: no
	
	prGetDnDModifiers { ^2 }	// default: control key
	
	prNeedsTransferHandler { ^false }
	
	beginDrag {
		currentDrag = if( beginDragAction.notNil, {
			beginDragAction.value( this );
		}, {
			this.defaultGetDrag;
		});
		currentDragString = currentDrag.asCompileString;
	}
	
	receiveDrag {
		if( receiveDragHandler.notNil, { receiveDragHandler.value( this )},{ this.defaultReceiveDrag });
		currentDrag = currentDragString = nil;
	}

	// "setProperty returns true if action needs to be called."
	setProperty { arg key, value;
		var oldValue;
		
		oldValue	= properties.at( key );
		properties.put( key, value );
		this.prSendProperty( key, value );
		^(oldValue != value);
	}
	
	prSendProperty { arg key, value;
		var id;

		key	= key.asSymbol;

		// fix keys
		switch( key,
			\resize, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
				if( value == 1, {
					server.sendBundle( nil, [ '/method', id, \putClientProperty, "resize", '[', '/ref', \null, ']' ],
								          [ '/method', id, \putClientProperty, "sizeref", '[', '/ref', \null, ']' ]);
				}, {
					server.sendBundle( nil, [ '/method', id, \putClientProperty, "sizeref",
											'[', '/methodr', '[', '/method', id, \getParent, ']', \getSize, ']' ],
								          [ '/method', id, \putClientProperty, "resize", value ]);
				});
				^nil;
			},
			\canFocus, {
				key = \focusable;
			},
//			\id, {
//				^nil; // not forwarded
//			},
			\minWidth, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });				server.listSendMsg([ '/method', id, \putClientProperty, "minWidth" ] ++ value.asSwingArg );
				^nil;
			},
			\maxWidth, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
				server.listSendMsg([ '/method', id, \putClientProperty, "maxWidth" ] ++ value.asSwingArg );
				^nil;
			},
			\minHeight, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
				server.listSendMsg([ '/method', id, \putClientProperty, "minHeight" ] ++ value.asSwingArg );
				^nil;
			},
			\maxHeight, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
				server.sendMsg([ '/method', id, \putClientProperty, "maxHeight" ] ++ value.asSwingArg );
				^nil;
			}
		);
		server.listSendMsg([ '/set', this.id, key ] ++ value.asSwingArg );
	}

	getProperty { arg key, value;
		^properties.atFail( key, value );
	}	

	setPropertyWithAction { arg symbol, obj;
		// setting some properties may need to have the action called.
		if( this.setProperty( symbol, obj ), {
			// setProperty returns true if action needs to be called.
			this.doAction;
		});
	}
	
	// never called with SwingOSC (?)
	*importDrag { 
		// this is called when an NSString is the drag object
		// from outside of the SC app
		// we compile it to an SCObject.
		currentDragString = currentDrag;
		currentDrag = currentDrag.interpret;
	}
	
	// this can be overridden
	prImportDrag { JSCView.importDrag }

	// contract: the returned rect is not identical to the one passed in
	prBoundsToJava { arg rect;
		var pb, pinsets;
		
		pb = parent.prGetJInsets.subtractFrom( parent.bounds );
		// moveBy guarantees that we get a copy!
		^jinsets.subtractFrom( rect ).moveBy( pb.left.neg, pb.top.neg );
	}

	// contract: the returned rect is not identical to the one passed in
	prBoundsFromJava { arg rect;
		var pb;
		
		pb = parent.prGetJInsets.subtractFrom( parent.bounds );
		// moveBy guarantees that we get a copy!
		^jinsets.addTo( rect ).moveBy( pb.left, pb.top );
	}

	protDraw {}

	prAllVisible {
//		[ this, "> prAllVisible", visible, allVisible ].postln;
		if( allVisible.isNil, {	// cache the info
			if( visible.not, {
				allVisible = false;
			}, {
				allVisible = this.parent.prAllVisible;
			});
		});
//		[ this, "< prAllVisible", visible, allVisible ].postln;
		^allVisible;
	}
}