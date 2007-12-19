/*
 *	JSCView
 *	(SwingOSC classes for SuperCollider)
 *
 *	Replacements for the basic (Cocoa) views.
 *	changes to v0.36 : - JSCDragView : added interpretDroppedStrings feature
 *	changes to v0.40 : - JSCCompositeView coordinates fixed
 *	changes to v0.42 : - JSCCompositeView is not opaque any more ; added opaque method
 *	changes to v0.43 : - added JSCMultiSliderView ; fixed JSCH/VLayoutView
 *	changes to v0.44 : - added resize functionality, fixed container views,
 *					  lots of bug fixes, fixed mouse-over behaviour, fixed opacities,
 *					  added envelope view, performance improvements
 *	changes to v0.54 : - argServer argument removed, since server needs to be same as parent view!
 *
 *	For details, see JSCView.help.rtf and DeveloperInfo.rtf
 *
 *	@version		0.57, 18-Dec-07
 *	@author		Hanns Holger Rutz
 *	@author		SuperCollider Developers
 *
 *	@todo		should invoke custom dispose() methods on java gadgets
 *	@todo		need to check all default properties are readable
 *	@todo		should call revalidate() only if parent window is already showing
 */
JSCView {  // abstract class
	classvar <>currentDrag, <>currentDragString;
	classvar <>globalKeyDownAction, <>globalKeyUpAction;

	classvar fakeModifiers	= 0;	// reflects caps lock
	classvar unicodeMap;

	var dataptr, <parent, <>action, <background;
	var <mouseDownAction, <mouseUpAction, <mouseOverAction, <mouseMoveAction;	var <>keyDownAction, <>keyUpAction, <>keyTyped;	var <beginDragAction,<>canReceiveDragHandler,<receiveDragHandler;
	var <>onClose;

	var <server;	// the SwingOSC server used for this gadget
	var properties;
	var keyResp, dndResp, mouseResp, cmpResp;
	var <hasFocus = false;
	
	var clpseMouseMove, clpseMouseDrag;

	var jinsets, scBounds, jBounds;
	
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
		this.prInvalidateChildBounds;
		scBounds = rect.copy;
		// XXX CompositeView must move its children!!! sucky cocoa!!!
	}
		
	visible { ^this.getProperty( \visible )}

	visible_ { arg bool; this.setProperty( \visible, bool )}
	
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

	id { ^this.getProperty( \id )}

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
		if(dataptr.notNil,{
			parent.prRemoveChild(this);
			this.prRemove;
			this.prClose;
		},{
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
		^#[ \visible, \enabled, \canFocus, \resize, \background,
		    \minWidth, \maxWidth, \minHeight, \maxHeight, \opaque ] // JJJ: opaque, no bounds
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
		properties.put( \visible, true );
		properties.put( \enabled, true );
		properties.put( \canFocus, true );
		properties.put( \resize, 1 );
//		this.id		= argID ?? { server.nextNodeID; };
		properties.put( \id, argID ?? { server.nextNodeID });
		dataptr		= this.id;

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
		
		bndl = List.new;
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
		// has to create a key responder, even if it's not using it personally ;-(
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
		globalKeyUpAction.value(this, char, modifiers, unicode, keycode);
		this.handleKeyUpBubbling(this, char, modifiers, unicode, keycode);
	}
	
	handleKeyDownBubbling { arg view, char, modifiers, unicode, keycode;
		var result;
		// nil from keyDownAction --> pass it on
		if (keyDownAction.isNil) {
			this.defaultKeyDownAction(char,modifiers,unicode,keycode);
			result = nil;
		}{
			result = keyDownAction.value(view, char, modifiers, unicode, keycode);
		};
		if(result.isNil) {  
			// call keydown action of parent view
			parent.handleKeyDownBubbling(view, char, modifiers, unicode, keycode);
		};
	}
	
	handleKeyUpBubbling { arg view, char, modifiers, unicode, keycode;
		var result;
		// nil from keyDownAction --> pass it on
		if (keyUpAction.isNil) {
			this.defaultKeyUpAction(char,modifiers,unicode,keycode);
			result = nil;
		}{
			result = keyUpAction.value(view, char, modifiers, unicode, keycode);
		};
		if(result.isNil) {  
			// call keydown action of parent view
			parent.handleKeyUpBubbling(view, char, modifiers, unicode, keycode);
		};
	}

	prSetScBounds { arg rect; scBounds = rect }
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
			var state, x, y, w, h, dx, dy, dw, dh;
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
//				scBounds		= properties[ \bounds ];
//(": "++this.id++" moved by "++dx++", "++dy).postln;
				if( scBounds.notNil, {
//("::: not nil").postln;
					scBounds.left	= scBounds.left + dx;
					scBounds.top	= scBounds.top + dy;
				});
				this.prInvalidateChildBounds;
				this.prBoundsUpdated;
			},
			\gainedFocus, {
				this.prHasFocus_( true );
			},
			\lostFocus, {
				this.prHasFocus_( false );
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
	
	prInvalidateChildBounds {}

	// subclasses can override this to invoke special refreshes
	prHasFocus_ { arg focus;
		hasFocus = focus;
	}

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
	
	prRemove {
	}

	prIsInsideContainer {
		^false;	// default: no
	}
	
	prGetDnDModifiers {
		^2;		// default: control key
	}
	
	prNeedsTransferHandler {
		^false;
	}
	
	beginDrag {
		currentDrag = if (beginDragAction.notNil) 
		{	
			beginDragAction.value(this)
		}{
			this.defaultGetDrag
		};
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
			\visible, {
				if( this.prIsInsideContainer, {
					server.sendMsg( '/set', "cn" ++ this.id, key, value.asSwingArg );
					^nil;
				});
			},
//			\background, {
//				if( value.isNil or: { value.isKindOf( Gradient ) or: { value.isKindOf( HiliteGradient ) or: { value.alpha < 1.0 }}}, {
//					if( this.opaque != false, {	// nil or false
//						this.opaque_( false );
//					});
//				}, { if( value.alpha == 1.0, {
//					if( this.opaque == false, {
//						this.opaque_( true );
//					});
//				})})
//			},
//			\bounds, {
//				value = this.prBoundsToJava( value );
//				if( this.prIsInsideContainer, {
//					server.sendBundle( nil, [ '/set', this.id, key ] ++ value.asSwingArg,
//								          [ '/set', "cn" ++ this.id, key ] ++ value.asSwingArg );
//					^nil;
//				});
//			},
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
			\id, {
				^nil; // not forwarded
			},
			\minWidth, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
//				server.sendMsg( '/method', id, \putClientProperty, "minWidth",
//					(value + jinsets.left + jinsets.right).asSwingArg );
				server.sendMsg( '/method', id, \putClientProperty, "minWidth", value.asSwingArg );
				^nil;
			},
			\maxWidth, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
//				server.sendMsg( '/method', id, \putClientProperty, "maxWidth",
//					(value + jinsets.left + jinsets.right).asSwingArg );
				server.sendMsg( '/method', id, \putClientProperty, "maxWidth", value.asSwingArg );
				^nil;
			},
			\minHeight, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
//				server.sendMsg( '/method', id, \putClientProperty, "minHeight",
//					(value + jinsets.top + jinsets.bottom).asSwingArg );
				server.sendMsg( '/method', id, \putClientProperty, "minHeight", value.asSwingArg );
				^nil;
			},
			\maxHeight, {
				id = if( this.prIsInsideContainer, { "cn" ++ this.id }, { this.id });
//				server.sendMsg( '/method', id, \putClientProperty, "maxHeight",
//					(value + jinsets.top + jinsets.bottom).asSwingArg );
				server.sendMsg( '/method', id, \putClientProperty, "maxHeight", value.asSwingArg );
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
	
	// never called with SwingOSC!
	*importDrag { 
		// this is called when an NSString is the drag object
		// from outside of the SC app
		// we compile it to an SCObject.
		currentDragString = currentDrag;
		currentDrag = currentDrag.interpret;
	}
	
	// this can be overridden
	prImportDrag {
		JSCView.importDrag;
	}

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
}

JSCContainerView : JSCView { // abstract class
	var <children, <>decorator;
			
	// ----------------- public instance methods -----------------

	removeAll {
		children.copy.do { arg child; child.remove };
	}
	
	// ----------------- quasi-interface methods : crucial-lib support -----------------

	asPageLayout { arg title, bounds;
		// though it won't go multi page
		// FlowView better ?
		^MultiPageLayout.on( this, bounds );
	}

	flow { arg func, bounds;
		var f, comp;
		f = FlowView( this, bounds /*?? { this.bounds }*/ );
		func.value( f );
		f.resizeToFit;
		^f;
	}
	
	horz { arg func, bounds;
		var comp;
		comp = JSCHLayoutView.new( this, bounds ?? { this.bounds });
		func.value( comp );
		^comp;
	}
	
	vert { arg func, bounds;
		var comp;
		comp = JSCVLayoutView.new( this, bounds ?? { this.bounds });
		func.value( comp );
		^comp;
	}
	
	comp { arg func, bounds;
		var comp;
		comp = JSCCompositeView.new( this, bounds ?? { this.bounds });
		func.value( comp );
		^comp;
	}		

	// ----------------- private instance methods -----------------

	add { arg child;
		var bndl;
		
		children = children.add( child );
		if (decorator.notNil, { decorator.place(child); });

		if( child.id.notNil, { 
			bndl = List.new;
			bndl.add([ '/method', this.id, \add,
					'[', '/ref', child.prIsInsideContainer.if({ "cn" ++ child.id }, child.id ), ']' ]);
			if( this.prGetWindow.visible, {
				bndl.add([ '/method', this.id, \revalidate ]);
				bndl.add([ '/method', this.id, \repaint ]);
			});
			server.listSendBundle( nil, bndl );
		});
	}
	
	prInvalidateChildBounds { children.do({ arg child; child.prSetScBounds( nil ); child.prInvalidateChildBounds })}

	prRemoveChild { arg child;
		children.remove(child);
		if( child.prIsInsideContainer, {
			server.sendMsg( '/method', this.id, \remove, '[', '/ref', "cn" ++ child.id, ']' );
		}, {
			server.sendMsg( '/method', this.id, \remove, '[', '/ref', child.id, ']' );
		});
		// ... decorator replace all
	}
	//bounds_  ... replace all

	prClose {
		super.prClose;
		children.do({ arg item; item.prClose });
	}

	prSCViewNew { arg preMsg, postMsg;
		properties.put( \canFocus, false );
		^super.prSCViewNew( preMsg, postMsg );
	}
	
	protDraw {
		children.do({ arg child; child.protDraw });
	}
}

JSCCompositeView : JSCContainerView {
	// ----------------- quasi-interface methods : crucial-lib support -----------------

	asFlowView { arg bounds;
		^FlowView(this,bounds ?? {this.bounds})
	}

	// ----------------- private instance methods -----------------

	prSCViewNew {
		properties.put( \opaque, false );
		jinsets = Insets( 3, 3, 3, 3 );  // so focus borders of children are not clipped
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.Panel", '[', '/new', "de.sciss.swingosc.ColliderLayout", ']', ']' ]
		]);
	}
}

JSCTopView : JSCContainerView {	// NOT subclass of JSCCompositeView
	var window;

	*new { arg window, bounds, id;  // JJJ
		^super.new.prInitTopView( window, bounds, id );
	}
	
	init { }	// kind of overriden by prInitTopView

	prInitTopView { arg argWindow, argBounds, id;
//		parent		= argParent.asView;	// actual view
		window		= argWindow;
//		scBounds		= argBounds;
//		jBounds		= this.prBoundsToJava( scBounds );
//		jinsets		= Insets.new;
		this.prInit( nil, argBounds.asRect, this.class.viewClass, window.server, id );
//		argParent.add( this );		// maybe window or viewadapter
	}

//	prInitTopView { arg argWindow;
//		window = argWindow;
//	}

	focus { arg flag = true;
		if( flag, {
			server.sendMsg( '/method', this.id, \requestFocus );
		}, {
			"JSCTopView.focus( false ) : not yet implemented".error;
		});
	}

	prGetWindow {
		^window;
	}

	// created by JSCWindow
	handleKeyDownBubbling { arg view, char, modifiers, unicode, keycode;
		keyDownAction.value( view, char, modifiers, unicode, keycode );
	}

	handleKeyUpBubbling { arg view, char, modifiers, unicode, keycode;
		keyUpAction.value( view, char, modifiers, unicode, keycode );
	}

	//only in construction mode, handled internally	canReceiveDrag { ^currentDrag.isKindOf(Class)}
	findWindow {
		^window;//		JSCWindow.allWindows.do({|win|//			if(win.view == this){//				^win//			}//		});
//		^nil;	}		defaultReceiveDrag {		var win, view;		win = this.findWindow;		view = currentDrag.paletteExample( win, Rect( 10, 10, 140, 24 ));		view.keyDownAction_({ arg view, char, modifiers, unicode, keycode;			if( keycode == 51, { view.remove });		});	}

//	prSendProperty { arg key, value;
//		var bndl;
//
//		key	= key.asSymbol;
//
//		switch( key,
//			\bounds, { ^nil; }	// shouldn't resize content pane
//		);
//		^super.prSendProperty( key, value );
//	}

	prBoundsToJava { arg rect;
		^rect.copy;
	}

	prBoundsFromJava { arg rect;
		^rect.copy;
	}

	prBoundsUpdated {
		if( window.drawHook.notNil, { window.refresh });
	}
}

JSCScrollTopView : JSCTopView {
	var <autohidesScrollers = true, <hasHorizontalScroller = true, <hasVerticalScroller = true;
	var <autoScrolls = true;
	var vpID;
	
	prInit { arg ... args;
		var result;
		result = super.prInit( *args );
		vpID = "vp" ++ this.id;
		server.sendMsg( '/local', vpID, '[', '/methodr', '[', '/method', this.id, \getViewport, ']', \getView, ']' );
	}

	autohidesScrollers_ { arg bool;
		var hPolicy, vPolicy;
		autohidesScrollers = bool;
		hPolicy = this.prCalcPolicy( bool, hasHorizontalScroller ) + 30;
		vPolicy = this.prCalcPolicy( bool, hasVerticalScroller ) + 20;

		server.sendMsg( '/set', this.id, \horizontalScrollBarPolicy, hPolicy, \verticalScrollBarPolicy, vPolicy );
	}
	
	prCalcPolicy { arg auto, has;
//		autohidesScrollers			1	0	1	0
//		hasHorizontalScroller		1	1	0	0
//		--------------------------------------------
//		horizontalScrollBarPolicy	0	2	1	1	+ 30
		^(has.not.binaryValue | ((auto.not && has).binaryValue << 1));
	}
	
	hasHorizontalScroller_ { arg bool;
		var policy;
		hasHorizontalScroller = bool;
		policy = this.prCalcPolicy( autohidesScrollers, bool ) + 30;
		server.sendMsg( '/set', this.id, \horizontalScrollBarPolicy, policy );
	}
	
	hasVerticalScroller_ { arg bool;
		var policy;
		hasVerticalScroller = bool;
		policy = this.prCalcPolicy( autohidesScrollers, bool ) + 20;
		server.sendMsg( '/set', this.id, \horizontalScrollBarPolicy, policy );
	}
	
	visibleOrigin_ { arg point;
//		"JSCScrollTopView.visibleOrigin_ : not yet implemented".warn;
		properties.put( \clipViewOrigin, point );
		server.sendMsg( '/method', this.id, \setViewPosition, point.x, point.y );
//		this.setProperty( \clipViewOrigin, point );
		this.doAction;
	}
	
	visibleOrigin {
		"JSCScrollTopView.visibleOrigin : not yet implemented".warn;
		^this.getProperty( \clipViewOrigin, Point.new );
	}
	
	autoScrolls_ { arg bool;
		"JSCScrollTopView.autoScrolls_ : not yet implemented".warn;
		autoScrolls = bool;
//		server.sendMsg( '/set', this.id, \autoScrolls, bool );
	}
	
	innerBounds {
		"JSCScrollTopView.innerBounds : not yet implemented".warn;
		^this.getProperty( \innerBounds, Rect.new )
	}

	// ----------------- private instance methods -----------------

	add { arg child;	// overriden to redirect to viewport
		var bndl;
		
		children = children.add( child );
		if (decorator.notNil, { decorator.place(child); });

		if( child.id.notNil, { 
			bndl = List.new;
			bndl.add([ '/method', vpID, \add,
					'[', '/ref', child.prIsInsideContainer.if({ "cn" ++ child.id }, child.id ), ']' ]);
			if( this.prGetWindow.visible, {
				bndl.add([ '/method', vpID, \revalidate ]);
				bndl.add([ '/method', vpID, \repaint ]);
			});
			server.listSendBundle( nil, bndl );
		});
	}
	
	prRemoveChild { arg child;	// overriden to redirect to viewport
		children.remove(child);
		if( child.prIsInsideContainer, {
			server.sendMsg( '/method', vpID, \remove, '[', '/ref', "cn" ++ child.id, ']' );
		}, {
			server.sendMsg( '/method', vpID, \remove, '[', '/ref', child.id, ']' );
		});
		// ... decorator replace all
	}
	
	prSendProperty { arg key, value;
		switch( key,
		\background, {	// overriden to redirect to viewport
			server.listSendMsg([ '/set', vpID, key ] ++ value.asSwingArg );
			^nil;
		});
		^super.prSendProperty( key, value );
	}
}

// JJJ : abstract!
JSCLayoutView : JSCContainerView {
	// ----------------- public instance methods -----------------

	properties { ^super.properties ++ #[\spacing] }
	
	spacing { ^this.getProperty( \spacing, 0 )}
	
	spacing_ { arg distance; this.setProperty( \spacing, distance )}

//	add { arg child;
//		var childID;
//		
//		if( child.prGetJInsets.notNil, {
//			childID = if( child.prIsInsideContainer, { "cn" ++ child.id }, { child.id });
//			server.sendBundle( nil, [ '/method', id, \putClientProperty, "insets" ] ++ child.prGetJInsets.asSwingArg );
//		});
//		^super.add( child );
//	}
	
	// ----------------- quasi-interface methods : crucial-lib support -----------------

	asFlowView {}

	// ----------------- private instance methods -----------------

	prSendProperty { arg key, value;
		var bndl;

		key	= key.asSymbol;

		switch( key,
			\spacing, {
				server.sendBundle( nil, [ '/methodr', '[', '/method', this.id, \getLayout, ']', \setSpacing, value ],
									 [ '/method', this.id, \revalidate ]);
				^nil;
			}
		);
		^super.prSendProperty( key, value );
	}

	prSCViewNew { arg preMsg, postMsg;
		properties.put( \spacing, 4 );
		jinsets = Insets( 3, 3, 3, 3 );  // so focus borders of children are not clipped
		^super.prSCViewNew( preMsg, postMsg );
	}
}

JSCHLayoutView : JSCLayoutView {
	prSCViewNew {
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.Panel", '[', '/new', "de.sciss.swingosc.ColliderAxisLayout", 0, 4, ']', ']' ]
		]);
	}
}

JSCVLayoutView : JSCLayoutView {
	prSCViewNew {
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.Panel", '[', '/new', "de.sciss.swingosc.ColliderAxisLayout", 1, 4, ']', ']' ]
		]);
	}
}

JSCControlView : JSCView { // abstract class
}

JSCSliderBase : JSCControlView {
	
	knobColor {
		^this.getProperty(\knobColor, Color.new)
	}
	knobColor_ { arg color;
		this.setProperty(\knobColor, color)
	}
	
	step_ { arg stepSize;
		this.setPropertyWithAction(\step, stepSize);
	}
	step {
		^this.getProperty(\step)
	}
	
	properties {
		^super.properties ++ #[\knobColor, \step]
	}

	prSnap { arg val;
		if( this.step <= 0.0, {
			^val.clip( 0.0, 1.0 );
		}, {
			^(val.clip( 0.0, 1.0 ) / this.step).round * this.step;
		});
	}
}

JSCSlider : JSCSliderBase
{
	var acResp, keyResp;	// OSCpathResponder for action listening
	var orientation;	// 0 for horiz, 1 for vert
	var clpse;

	value { ^this.getProperty( \value )}
	
	value_ { arg val;
		this.setProperty( \value, this.prSnap( val ));
	}
	
	valueAction_ { arg val;
		this.setPropertyWithAction( \value, this.prSnap( val ));
	}	
	
	increment {
		var inc;
		inc = (if( orientation == 0, { this.bounds.width }, { this.bounds.height }) - 2).max( 1 ).reciprocal.max( this.step );
		^this.valueAction = this.value + inc;
	}
	
	decrement {
		var inc;
		inc = (if( orientation == 0, { this.bounds.width }, { this.bounds.height }) - 2).max( 1 ).reciprocal.max( this.step );
		^this.valueAction = this.value - inc;
	}
	
	defaultKeyDownAction { arg char, modifiers, unicode, keycode;
		// standard keydown
		if (char == $r, { this.valueAction = 1.0.rand; ^this });		if (char == $n, { this.valueAction = 0.0; ^this });		if (char == $x, { this.valueAction = 1.0; ^this });		if (char == $c, { this.valueAction = 0.5; ^this });
		if (char == $], { this.increment; ^this });
		if (char == $[, { this.decrement; ^this });
		if (unicode == 16rF700, { this.increment; ^this });
		if (unicode == 16rF703, { this.increment; ^this });
		if (unicode == 16rF701, { this.decrement; ^this });
		if (unicode == 16rF702, { this.decrement; ^this });
		^nil		// bubble if it's an invalid key	}
	
	defaultGetDrag { ^this.value }
	defaultCanReceiveDrag { ^currentDrag.isNumber }
	defaultReceiveDrag { this.valueAction = currentDrag }

	prNeedsTransferHandler { ^true }

	thumbSize { ^this.getProperty( \thumbSize, 12 )}
	
	thumbSize_ { arg size;
		"JSCSlider.thumbSize_ : not yet implemented".warn;
		this.setProperty( \thumbSize, size );
	}
	
	properties {
		^super.properties ++ #[ \thumbSize ];
	}

	prClose {
		acResp.remove;
		clpse.cancel;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					   [ '/free', "ac" ++ this.id ]]);
	}

	prSCViewNew {
		properties.put( \value, 0.0 );
		properties.put( \step, 0.0 );
		orientation = if( this.bounds.width > this.bounds.height, 0, 1 );
		clpse	= Collapse({ this.doAction });
		acResp	= OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			var newVal;
		
//			newVal = msg[4] / 0x40000000;
			newVal = this.prSnap( msg[4] / 0x40000000 );
			if( newVal != this.value, {
				// don't call valueAction coz we'd create a loop
				properties.put( \value, newVal );
				clpse.instantaneous;
			});
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id,
				'[', '/new', "de.sciss.swingosc.Slider", orientation, 0, 0x40000000, 0, ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, \value, ']' ]
		]);
	}
	
	bounds_ { arg rect;
		var result;
		result = super.bounds_( rect );
		if( if( rect.width > rect.height, 0, 1 ) != orientation, {
			orientation = 1 - orientation;
			server.sendMsg( '/set', this.id, \orientation, orientation );
		});
		^result;
	}

	prSendProperty { arg key, value;

		key	= key.asSymbol;

		// fix keys
		case { key === \value }
		{
			key		= \valueNoAction;
			value	= value * 0x40000000;
		}
		{ key === \step }
		{
			value = max( 1, value * 0x40000000 ).asInteger;
//			server.sendMsg( '/set', this.id, \snapToTicks, value != 0,
//							\minorTickSpacing, value, \extent, value );
			server.sendMsg( '/set', this.id, \snapToTicks, value != 0,
							\majorTickSpacing, value ); // stupidly, using extent won't let you move the slider to the max
			^nil;
		};
		^super.prSendProperty( key, value );
	}
}

JSCKnob : JSCSlider
{
}

JSCRangeSlider : JSCSliderBase {

	var acResp;	// OSCpathResponder for action listening
	var clpse;
	var orientation;	// 0 for horiz, 1 for vert

	*paletteExample { arg parent, bounds;
		var v;
		v = this.new( parent, bounds );
		v.setSpan( 0.2, 0.7 );
		^v;	}

	step_ { arg stepSize;
		super.step_( stepSize );
		this.setSpan( this.lo, this.hi );
	}
	
	lo { ^this.getProperty( \lo )}

	lo_ { arg val;
		this.setProperty( \lo, this.prSnap( val ));
	}
	
	activeLo_ { arg val;
		this.setPropertyWithAction( \lo, this.prSnap( val ));
	}
	
	hi { ^this.getProperty( \hi )}

	hi_ { arg val;
		this.setProperty( \hi, this.prSnap( val ));
	}
	
	activeHi_ { arg val;
		this.setPropertyWithAction( \hi, this.prSnap( val ));
	}
	
	range { ^(this.hi - this.lo).abs }

	range_ { arg val;
		this.hi_( this.prSnap( this.lo + val ));
	}
	
	activeRange_ { arg val;
		this.range_( val );
		this.doAction;
	}
	
	setSpan { arg lo, hi;
		lo = this.prSnap( lo );
		hi = this.prSnap( hi );
		properties.put( \lo, lo );
		properties.put( \hi, hi );
		server.sendMsg( '/set', this.id, \knobPos, min( lo, hi ), \knobExtent, abs( hi - lo ));
	}
	
	setSpanActive { arg lo, hi;
		this.setSpan( lo, hi );
		this.doAction;
	}

	properties {
		^super.properties ++ #[ \lo, \hi ];
	}
	
	increment {
		var inc, val; 
		inc = (if( orientation == 0, { this.bounds.width }, { this.bounds.height }) - 2).max( 1 ).reciprocal;
		val = this.hi + inc;
		if( val > 1, {
			inc = 1 - this.hi;
			val = 1;
		});
		this.setSpanActive( this.lo + inc, val );
	}
	
	decrement { 
		var inc, val; 
		inc = (if( orientation == 0, { this.bounds.width }, { this.bounds.height }) - 2).max( 1 ).reciprocal;
		val = this.lo - inc;
		if( val < 0, {
			inc = this.lo;
			val = 0;
		});
		this.setSpanActive( val, this.hi - inc );
	}

	defaultKeyDownAction { arg char, modifiers, unicode;
		var a, b;
		// standard keydown
		if (char == $r, { 
			a = 1.0.rand;
			b = 1.0.rand;
			this.setSpanActive( min( a, b ), max( a, b ));
			^this;		});
		if (char == $n, { this.setSpanActive( 0.0, 0.0 ); ^this });		if (char == $x, { this.setSpanActive( 1.0, 1.0 ); ^this });		if (char == $c, { this.setSpanActive( 0.5, 0.5 ); ^this });		if (char == $a, { this.setSpanActive( 0.0, 1.0 ); ^this });		if (unicode == 16rF700, { this.increment; ^this });		if (unicode == 16rF703, { this.increment; ^this });		if (unicode == 16rF701, { this.decrement; ^this });		if (unicode == 16rF702, { this.decrement; ^this });		^nil;		// bubble if it's an invalid key
	}

	defaultGetDrag { ^Point( this.lo, this.hi )}	
	defaultCanReceiveDrag {	 ^currentDrag.isKindOf( Point )}
	
	defaultReceiveDrag {
		// changed to x,y instead of lo, hi
		this.setSpanActive( currentDrag.x, currentDrag.y );
	}

	prNeedsTransferHandler { ^true }

	prClose {
		acResp.remove;
		clpse.cancel;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					  [ '/free', "ac" ++ this.id ],
					  [ '/method', this.id, \dispose ]]);
	}

	prSCViewNew {
		properties.put( \lo, 0.0 );
		properties.put( \hi, 1.0 );
		properties.put( \step, 0.0 );
		jinsets		= Insets( 3, 3, 3, 3 );
		orientation	= if( this.bounds.width > this.bounds.height, 0, 1 );
		clpse		= Collapse({ this.doAction });
		acResp		= OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			var newLo, newHi;
		
			newLo	= msg[4];
			newHi 	= newLo + msg[6];
			if( (newLo != this.lo) || (newHi != this.hi), {
				// don't call valueAction coz we'd create a loop
				properties.put( \lo, newLo );
				properties.put( \hi, newHi );
				clpse.instantaneous;
			});
		}).add;
		^super.prSCViewNew([
			[ '/set', '[', '/local', this.id, '[', '/new', "de.sciss.swingosc.RangeSlider", orientation, ']', ']',
				\knobColor ] ++ Color.blue.asSwingArg,
			[ '/local', "ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, '[', '/array', \knobPos, \knobExtent, ']', ']' ]
		]);
	}

	bounds_ { arg rect;
		var result;
		result = super.bounds_( rect );
		if( if( rect.width > rect.height, 0, 1 ) != orientation, {
			orientation = 1 - orientation;
			server.sendMsg( '/set', this.id, \orientation, orientation );
		});
		^result;
	}

	prSendProperty { arg key, value;
		key	= key.asSymbol;

		// fix keys
		case { key === \lo }
		{
			server.sendMsg( '/set', this.id, \knobPos, min( value, this.hi ), \knobExtent, abs( value - this.hi ));
			^nil;		
		}
		{ key === \hi }
		{
			server.sendMsg( '/set', this.id, \knobPos, min( value, this.lo ), \knobExtent, abs( value - this.lo ));
			^nil;		
		}
		{ key === \step }
		{
			key = \stepSize;
		};
		^super.prSendProperty( key, value );
	}
}

JSC2DSlider : JSCSliderBase {

	var acResp;	// OSCpathResponder for action listening
	var clpse;

	step_ { arg stepSize;
		super.step_( stepSize );
		this.x_( this.x );
		this.y_( this.y );
	}

	x { ^this.getProperty( \x )}

	x_ { arg val;
		this.setProperty( \x, this.prSnap( val ));
	}
	
	activex_ { arg val;
		this.setPropertyWithAction( \x, this.prSnap( val ));
	}
	
	y { ^this.getProperty( \y )}

	y_ { arg val;
		this.setProperty( \y, this.prSnap( val ));
	}
	
	activey_ { arg val;
		this.setPropertyWithAction( \y, this.prSnap( val ));
	}
	
	properties {
		^super.properties ++ #[ \x, \y ];
	}
	
	setXY { arg x, y;
		x = this.prSnap( x );
		y = this.prSnap( y );
		properties.put( \x, x );
		properties.put( \y, y );
		server.sendMsg( '/set', this.id, \knobX, x, \knobY, y );
	}
	
	setXYActive { arg x, y;
		this.setXY( x, y );
		this.doAction;
	}

	incrementY { ^this.y = this.y + this.bounds.height.reciprocal }
	decrementY { ^this.y = this.y - this.bounds.height.reciprocal }
	incrementX { ^this.x = this.x + this.bounds.width.reciprocal }
	decrementX { ^this.x = this.x - this.bounds.width.reciprocal }

	defaultKeyDownAction { arg char, modifiers, unicode,keycode;
		// standard keydown
		if (char == $r, { this.setXYActive( 1.0.rand, 1.0.rand ); ^this });
		if (char == $n, { this.setXYActive( 0.0, 0.0 ); ^this });
		if (char == $x, { this.setXYActive( 1.0, 1.0 ); ^this });
		if (char == $c, { this.setXYActive( 0.5, 0.5 ); ^this });
		if (unicode == 16rF700, { this.incrementY; this.doAction; ^this });
		if (unicode == 16rF703, { this.incrementX; this.doAction; ^this });
		if (unicode == 16rF701, { this.decrementY; this.doAction; ^this });
		if (unicode == 16rF702, { this.decrementX; this.doAction; ^this });
		^nil		// bubble if it's an invalid key	}

	defaultGetDrag { ^Point( this.x, this.y )}
	defaultCanReceiveDrag { ^currentDrag.isKindOf( Point )}
	defaultReceiveDrag { this.setXYActive( currentDrag.x, currentDrag.y )}

	prNeedsTransferHandler { ^true }

	prClose {
		acResp.remove;
		clpse.cancel;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					   [ '/free', "ac" ++ this.id ],
					   [ '/method', this.id, \dispose ]]);
	}

	prSCViewNew {
		properties.put( \x, 0.0 );
		properties.put( \y, 0.0 );
		properties.put( \step, 0.0 );
		jinsets	= Insets( 3, 3, 3, 3 );
		clpse	= Collapse({ this.doAction });
		acResp	= OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			var newX, newY;
		
			newX = msg[4];
			newY = msg[6];
			if( (newX != this.x) || (newY != this.y), {
				// don't call valueAction coz we'd create a loop
				properties.put( \x, newX );
				properties.put( \y, newY );
				clpse.instantaneous;
			});
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id,
				'[', '/new', "de.sciss.swingosc.Slider2D", ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, '[', '/array', \knobX, \knobY, ']', ']' ]
		]);
	}

	prSendProperty { arg key, value;

		key	= key.asSymbol;

		// fix keys
		case { key === \x }
		{
			key = \knobX;
		}
		{ key === \y }
		{
			key = \knobY;
		}
		{ key === \step }
		{
			key = \stepSize;
		};
		^super.prSendProperty( key, value );
	}
}

// JJJ : not yet working
JSC2DTabletSlider : JSC2DSlider {

//	var <>mouseDownAction,<>mouseUpAction;	
	mouseDown { arg x,y,pressure,tiltx,tilty,deviceID,
			 buttonNumber,clickCount,absoluteZ,rotation;
		mouseDownAction.value(this,x,y,pressure,tiltx,tilty,deviceID, 
			buttonNumber,clickCount,absoluteZ,rotation);
	}
	mouseUp { arg x,y,pressure,tiltx,tilty,deviceID, 
			buttonNumber,clickCount,absoluteZ,rotation;
		mouseUpAction.value(this,x,y,pressure,tiltx,tilty,deviceID, 
			buttonNumber,clickCount,absoluteZ,rotation);
	}
	doAction { arg x,y,pressure,tiltx,tilty,deviceID, 
			buttonNumber,clickCount,absoluteZ,rotation;
		action.value(this,x,y,pressure,tiltx,tilty,deviceID, 
			buttonNumber,clickCount,absoluteZ,rotation);
	}
}

JSCButton : JSCControlView {
	var <states;
	
	var acResp;	// OSCpathResponder for action listening

	*paletteExample { arg parent, bounds;
		var v;
		v = this.new(parent, bounds);
		v.states = [
			["Push", Color.black, Color.red],
			["Pop", Color.white, Color.blue]];
		^v
	}
	
	value {
		^this.getProperty(\value)
	}
	value_ { arg val;
		this.setProperty( \value, this.prFixValue( val ));
	}	
	valueAction_ { arg val;
		this.setPropertyWithAction( \value, this.prFixValue( val ));
	}	

	prFixValue { arg val;
		val = val.asInteger;
		// clip() would be better but SCButton resets to zero always
		if( (val < 0) || (val >= states.size), {
			val = 0;
		});
		^val;
	}
	
	doAction { arg modifiers;
		action.value( this, modifiers );
	}
	
	defaultKeyDownAction { arg char, modifiers, unicode;
// JJJ handled automatically by javax.swing.AbstractButton
//		if (char == $ , { this.valueAction = this.value + 1; ^this });
		if (char == $\r, { this.valueAction = this.value + 1; ^this });
		if (char == $\n, { this.valueAction = this.value + 1; ^this });
		if (char == 3.asAscii, { this.valueAction = this.value + 1; ^this });
		^nil		// bubble if it's an invalid key	}

	font { ^this.getProperty( \font )}

	font_ { arg argFont;
//		font = argFont;
		this.setProperty( \font, argFont );
	}

	states_ { arg array;
		states = array;
		this.setProperty(\states, states);
	}
	
	properties {
		^super.properties ++ #[\value, \font, \states]
	}
	
	defaultGetDrag { 
		^this.value
	}
	defaultCanReceiveDrag {
		^currentDrag.isNumber or: { currentDrag.isKindOf(Function) };
	}
	defaultReceiveDrag {
		if (currentDrag.isNumber) {
			this.valueAction = currentDrag;
		}{
			this.action = currentDrag;
		};
	}

	prNeedsTransferHandler { ^true }

	prClose {
		acResp.remove;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					   [ '/free', "ac" ++ this.id ]]);
	}

	prSCViewNew {
		properties.put( \value, 0 );
		jinsets = Insets( 3, 3, 3, 3 );
		acResp = OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			var value, modifiers;
			value	= msg[4];
			modifiers	= msg[6];
			// java->cocoa ; this translates shift (1), ctrl (2), cmd (4), alt (8)
//			modifiers		= ((modifiers & 3) << 17) |
//						  ((modifiers & 4) << 18) |
//						  ((modifiers & 8) << 16); // | plusMod;
			// don't call valueAction coz we'd create a loop
			properties.put( \value, msg[4] );
			{ this.doAction( modifiers )}.defer;
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.gui.MultiStateButton", ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, '[', '/array', \selectedIndex, \lastModifiers, ']', ']' ]
//				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, '[', '/array', \selectedIndex, ']', ']' ]
		]);
	}

	prSendProperty { arg key, value;
		var bndl, msg;

		key	= key.asSymbol;

		// fix keys
		case { key === \value }
		{
			key = \selectedIndex;
		}
		{ key === \states }
		{
			bndl	= List.new;
			bndl.add([ '/method', this.id, \removeAllItems ]);
			value.do({ arg state;
				msg = List.newUsing([ '/method', this.id, \addItem ]);
				msg.addAll( state[0].asSwingArg );
				msg.addAll( state[1].asSwingArg );
				msg.addAll( state[2].asSwingArg );
				bndl.add( msg.asArray );
			});
//("Sending '"++bndl++"'").postln;
			server.listSendBundle( nil, bndl );
			^nil;
		};
		^super.prSendProperty( key, value );
	}
}


JSCPopUpMenu : JSCControlView {
	var <items;
	
	var acResp;	// OSCpathResponder for action listening
	var <>allowsReselection = false;

	*paletteExample { arg parent, bounds;
		var v;
		v = this.new(parent, bounds);
		v.items = #["linear","exponential","sine","welch","squared","cubed"];
		^v
	}
		
	value {
		^this.getProperty(\value)
	}
	value_ { arg val;
		this.setProperty(\value, this.prFixValue( val ));
	}	
	valueAction_ { arg val;
		this.setPropertyWithAction(\value, this.prFixValue( val ));
	}
	
	prFixValue { arg val;
		^val.clip( 0, items.size - 1 );
	}

	defaultKeyDownAction { arg char, modifiers, unicode;
// JJJ used by lnf
//		if (char == $ , { this.valueAction = this.value + 1; ^this });
//		if (char == $\r, { this.valueAction = this.value + 1; ^this });
//		if (char == $\n, { this.valueAction = this.value + 1; ^this });
		if (char == 3.asAscii, { this.valueAction = this.value + 1; ^this });
//		if (unicode == 16rF700, { this.valueAction = this.value - 1; ^this });
		if (unicode == 16rF703, { this.valueAction = this.value + 1; ^this });
//		if (unicode == 16rF701, { this.valueAction = this.value + 1; ^this });
		if (unicode == 16rF702, { this.valueAction = this.value - 1; ^this });
		^nil		// bubble if it's an invalid key	}
	
	font { ^this.getProperty( \font )}
	
	font_ { arg argFont;
//		font = argFont;
		this.setProperty( \font, argFont );
	}
	
	items_ { arg array;
		items = array;
		this.setProperty(\items, items);
	}
	
	item { ^items[ this.value ]}

	stringColor {
		^this.getProperty(\stringColor, Color.new)
	}
	stringColor_ { arg color;
		this.setProperty(\stringColor, color)
	}
	
	properties {
		^super.properties ++ #[\value, \font, \items, \stringColor]
	}

	defaultGetDrag { 
		^this.value
	}
	defaultCanReceiveDrag {
		^currentDrag.isNumber;
	}
	defaultReceiveDrag {
		this.valueAction = currentDrag;
	}

	prNeedsTransferHandler { ^true }

	prClose {
		acResp.remove;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					   [ '/free', "ac" ++ this.id ]]);
	}

	prSCViewNew {
		properties.put( \value, 0 );
		acResp = OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			var newVal;
			
			newVal = msg[4];
			if( allowsReselection or: { newVal != this.value }, {
				// don't call valueAction coz we'd create a loop
				properties.put( \value, newVal );
				{ this.doAction; }.defer;
			});
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.PopUpView", ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, \selectedIndex, ']' ]
		]);
	}

	prSendProperty { arg key, value;

		key	= key.asSymbol;

		// fix keys
		case { key === \value }
		{
			key = \selectedIndex;
		}
		{ key === \items }
		{
			this.prSetItems( value.performUnaryOp( \asString ));
			^nil;
		};
		^super.prSendProperty( key, value );
	}

// XXX einstweilen...
//	prBoundsToJava { arg rect;
//		var pb;
//		
//		pb = parent.bounds;
//		if( rect.height < 26, {
//			rect			= Rect( rect.left - pb.left, rect.top - ((26 - rect.height) >> 1) - pb.top,
//							   rect.width, 26 );
//		}, {
//			rect	= rect.moveBy( pb.left.neg, pb.top.neg );
//		});
//		^rect;
//	}

// what shall we do ...
//	prBoundsFromJava { arg rect;
//		^rect;
//	}

	prSetItems { arg items;
		var sizes, dataSize, startIdx;

		sizes = items.collect({ arg item; ((item.size + 4) & -4) + 1 });
//("sum = "++sizes.sum).postln;
		if( (sizes.sum + 70) < server.options.oscBufSize, {
			server.listSendMsg([ '/method', this.id, \setListData, '[', '/array' ] ++ items ++
											[ ']', this.value ]);
		}, {	// need to split it up
			startIdx = 0;
			dataSize	= 45;
			server.sendMsg( '/method', this.id, \beginDataUpdate );
			sizes.do({ arg size, idx;
				if( (dataSize + size) > server.options.oscBufSize, {
//("sending : "++dataSize).postln;
					server.listSendMsg([ '/method', this.id, \addData, '[', '/array', ] ++
						items.copyRange( startIdx, idx - 1 ) ++ [ ']' ]);
					dataSize	= 45;
					startIdx	= idx;
				}, {
					dataSize = dataSize + size;
				});
			});
			server.listSendMsg([ '/method', this.id, \addData, '[', '/array', ] ++
					items.copyRange( startIdx, items.size - 1 ) ++ [ ']' ]);
			server.sendMsg( '/method', this.id, \endDataUpdate, this.value );
		});
	}
}



JSCStaticTextBase : JSCView {
	var <string, <object, <>setBoth=true;
	
	font { ^this.getProperty( \font )}
	
	font_ { arg argFont;
//		font = argFont;
		this.setProperty( \font, argFont );
	}
	
	string_ { arg argString;
		string = argString.asString;
		this.setProperty(\string, string)
	}
	align_ { arg align;
		this.setProperty(\align, align)
	}
	
	stringColor {
		^this.getProperty(\stringColor, Color.new)
	}
	stringColor_ { arg color;
		this.setProperty(\stringColor, color)
	}

	object_ { arg obj;
		object = obj;
		if (setBoth) { this.string = object.asString(80); };
	}
	
	properties {
		^super.properties ++ #[\string, \font, \stringColor]
	}

	prSendProperty { arg key, value;

		key	= key.asSymbol;

		// fix keys
		case { key === \stringColor }
		{
			key = \foreground;
		}
		{ key === \align }
		{
			key = \horizontalAlignment;
			case { value === \left }
			{
				value = 2;
			}
			{ value === \center }
			{
				value = 0;
			}
			{ value === \right }
			{
				value = 4;
			}
			// undocumented cocoa feature : -1 = left, 0 = center, 1 = right
			{ value.isKindOf( SimpleNumber )}
			{
				value = switch( value.sign, -1, 2, 0, 0, 1, 4 );
			};
		}
		{ key === \string }
		{
			key = \text;
//			value = value.asSwingArg;
// funktioniert nicht, weil BoundedRangeModel offensichtlich nochmal durch setText veraendert wird
//			server.sendBundle( nil,
//				[ '/set', this.id, \text, value ],	// make sure the text beginning is shown
//				[ "/methodr", [ '/method', this.id, \getHorizontalVisibility ], \setValue, 0 ]
//			);
//			^nil;
		};
		^super.prSendProperty( key, value );
	}
}

JSCStaticText : JSCStaticTextBase {
	*paletteExample { arg parent, bounds;
		var v;
		v = this.new(parent, bounds);
		v.string = "The lazy brown fox";
		^v
	}

	prSCViewNew {
		properties.put( \canFocus, false );
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.Label", ']' ]
		]);
	}
}

JSCListView : JSCControlView {
	var <items, <>enterKeyAction;
	var <allowsDeselection = false;
	
	var acResp;	// listens to list selection changes
	
	*paletteExample { arg parent, bounds;
		var v;
		v = this.new(parent, bounds);
		v.items = #["linear","exponential","sine","welch","squared","cubed"];
		^v
	}
	
	item {
		^items[this.value]
	}
	value {
		^this.getProperty( \value );
	}
	value_ { arg val;
		this.setProperty( \value, this.prFixValue( val ));
	}	
	valueAction_ { arg val;
		this.setPropertyWithAction( \value, this.prFixValue( val ));
	}
	
	allowsDeselection_ {Êarg bool;
		if( allowsDeselection != bool, {
			allowsDeselection = bool;
			if( allowsDeselection, {
				if( (this.value == 0) and: { items.size == 0 }, {
					this.valueAction_( nil );
				});
			}, {
				if( this.value.isNil, {
					this.valueAction_( 0 );
				});
			});
		});
	}

	prFixValue { arg val;
		if( allowsDeselection and: { val.isNil }, { ^nil });
		val = (val ? 0).asInteger;
		if( (val < 0) || (val >= items.size), {
			val = 0;
		});
		^val;
	}

	defaultKeyDownAction { arg char, modifiers, unicode;
		var index;
		if( this.value.notNil, {
			if( char == $ , { this.valueAction = this.value + 1; ^this });
			if( char == $\r, { this.enterKeyAction.value(this); ^this });
			if( char == $\n, { this.enterKeyAction.value(this); ^this });
			if( char == 3.asAscii, { this.enterKeyAction.value(this); ^this });
	// JJJ automatically handled by lnf
	//		if( unicode == 16rF700, { this.valueAction = this.value - 1; ^this });
			if( unicode == 16rF703, { this.valueAction = this.value + 1; ^this });
	//		if( unicode == 16rF701, { this.valueAction = this.value + 1; ^this });
			if( unicode == 16rF702, { this.valueAction = this.value - 1; ^this });
		});
		if (char.isAlpha, {
			char = char.toUpper;
			index = items.detectIndex({ arg item; item.asString.at(0).toUpper >= char });
			if( index.notNil, { this.valueAction = index });
			^this;		});		^nil;	// bubble if it's an invalid key	}
	
	font { ^this.getProperty( \font )}
	
	font_ { arg argFont;
//		font = argFont;
		this.setProperty( \font, argFont );
	}
	items_ { arg array;
		items = array;
		this.setProperty(\items, items);
	}
	stringColor {
		^this.getProperty(\stringColor, Color.new)
	}
	stringColor_ { arg color;
		this.setProperty(\stringColor, color)
	}
	
	selectedStringColor {
		^this.getProperty(\selectedStringColor, Color.new)
	}
	selectedStringColor_ { arg color;
		this.setProperty(\selectedStringColor, color)
	}
	
	hiliteColor {
		^this.getProperty(\hiliteColor, Color.new)
	}
	hiliteColor_ { arg color;
		this.setProperty(\hiliteColor, color)
	}
	
	properties {
		^super.properties ++ #[\value, \font, \items, \stringColor]
	}

	defaultGetDrag { 
		^this.value
	}
	defaultCanReceiveDrag {
		^currentDrag.isNumber;
	}
	defaultReceiveDrag {
		this.valueAction = currentDrag;
	}

	prNeedsTransferHandler { ^true }

	prClose {
		acResp.remove;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					   [ '/free', "ac" ++ this.id ]]);
	}

	prIsInsideContainer { ^true }

	prSCViewNew {
		properties.put( \value, 0 );
		
		acResp = OSCpathResponder( server.addr, [ '/list', this.id ], { arg time, resp, msg;
			var newVal;

			newVal = this.prFixValue( if( msg[4] >= 0, msg[4] ));
			if( newVal != this.value, {
				// don't call valueAction coz we'd create a loop
				properties.put( \value, newVal );
				{ this.doAction; }.defer;
			});
		}).add;
		^super.prSCViewNew([
			[ '/set', '[', '/local', this.id, '[', '/new', "de.sciss.swingosc.ListView", ']', ']',
				\selectionMode, 0 ],	// single selection only for compatibility
			[ '/local', "cn" ++ this.id, 	// bars : v=asNeeded, h=never
				'[', '/new', "javax.swing.JScrollPane", '[', '/ref', this.id, ']', 20, 31, ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ListResponder", this.id,
					'[', '/array', \selectedIndex, ']', ']' ] // , \valueIsAdjusting
		]);
	}

	prSendProperty { arg key, value;

		key	= key.asSymbol;

		// fix keys
		case { key === \value }
		{
			key = \selectedIndex;
			value = value ? -1;
			if( value >= 0, {
				value = value.asSwingArg;
				server.sendBundle( nil,
					[ '/set', this.id, \selectedIndex, value ],
					[ '/method', this.id, \ensureIndexIsVisible, value ]
				);
			}, {
				server.sendMsg( '/set', this.id, \selectedIndex, value );
			});
			^nil;
		}
		{ key === \stringColor }
		{
			key = \foreground;
		}
		{ key === \selectedStringColor }
		{
			key = \selectionForeground;
		}
		{ key === \hiliteColor }
		{
			key = \selectionBackground;
		}
		{ key === \items }
		{
			this.prSetItems( value.performUnaryOp( \asString ));
			^nil;
//		}
//		{ key === \bounds }
//		{
//			server.listSendMsg([ '/set', "cn" ++ this.id, key ] ++ this.prBoundsToJava( value ).asSwingArg );
//			^nil;
		};
		^super.prSendProperty( key, value );
	}

	prSetItems { arg items;
		var sizes, dataSize, startIdx;

		sizes = items.collect({ arg item; ((item.size + 4) & -4) + 1 });
//("sum = "++sizes.sum).postln;
		if( (sizes.sum + 70) < server.options.oscBufSize, {
			server.listSendMsg([ '/method', this.id, \setListData, '[', '/array' ] ++ items ++
											[ ']', this.value ? -1 ]);
		}, {	// need to split it up
			startIdx = 0;
			dataSize	= 45;
			server.sendMsg( '/method', this.id, \beginDataUpdate );
			sizes.do({ arg size, idx;
				if( (dataSize + size) > server.options.oscBufSize, {
//("sending : "++dataSize).postln;
					server.listSendMsg([ '/method', this.id, \addData, '[', '/array', ] ++
						items.copyRange( startIdx, idx - 1 ) ++ [ ']' ]);
					dataSize	= 45;
					startIdx	= idx;
				}, {
					dataSize = dataSize + size;
				});
			});
			server.listSendMsg([ '/method', this.id, \addData, '[', '/array', ] ++
					items.copyRange( startIdx, items.size - 1 ) ++ [ ']' ]);
			server.sendMsg( '/method', this.id, \endDataUpdate, this.value ? -1 );
		});
	}
}

JSCDragView : JSCStaticTextBase {
	var <>interpretDroppedStrings = true;
	
	*paletteExample { arg parent, bounds;
		var v;
		v = this.new(parent, bounds);
		v.object = \something;
		^v
	}
	defaultGetDrag { ^object }

	prNeedsTransferHandler { ^true }

	prImportDrag {
		if( interpretDroppedStrings, { JSCView.importDrag });
	}

	prSCViewNew { arg preMsg, postMsg;
		properties.put( \canFocus, false );
		jinsets = Insets( 3, 3, 3, 3 );
		^super.prSCViewNew( preMsg, postMsg );
	}
}

JSCDragSource : JSCDragView {
	prSCViewNew {
		^super.prSCViewNew([
//			[ '/set', '[', '/local', this.id, '[', '/new', "de.sciss.swingosc.Label", ']', ']',
//				\border, '[', '/method', "javax.swing.BorderFactory", \createRaisedBevelBorder, ']'
//			]
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.DragView", 1, ']',
			]
		]);
	}

	prGetDnDModifiers { ^0 } 	// no modifiers needed
}

JSCDragSink : JSCDragView {	
	defaultCanReceiveDrag { ^true;	}
	defaultReceiveDrag {
		this.object = currentDrag;
		this.doAction;
	}

	prSCViewNew {
		^super.prSCViewNew([
//			[ '/set', '[', '/local', this.id, '[', '/new', "de.sciss.swingosc.Label", ']', ']',
//				\border, '[', '/method', "javax.swing.BorderFactory", \createLoweredBevelBorder, ']'
//			]
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.DragView", 0, ']',
			]
		]);
	}

	prGetDnDModifiers { ^-1 }	// don't allow it to be drag source
}

JSCDragBoth : JSCDragView {		// JJJ not subclass of JSCDragSink
	defaultCanReceiveDrag { ^true;	}
	defaultReceiveDrag {
		this.object = currentDrag;
		this.doAction;
	}

	defaultGetDrag { ^object }

	prSCViewNew {
		^super.prSCViewNew([
//			[ '/set', '[', '/local', this.id, '[', '/new', "de.sciss.swingosc.Label", ']', ']',
//				\border, '[', '/method', "javax.swing.BorderFactory", \createCompoundBorder,
//					'[', '/method', "javax.swing.BorderFactory", \createRaisedBevelBorder, ']',
//					'[', '/method', "javax.swing.BorderFactory", \createLoweredBevelBorder, ']', ']'
//			]
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.DragView", 2, ']',
			]
		]);
	}

	prGetDnDModifiers { ^0 } 	// no modifiers needed
}


JSCAbstractUserView : JSCView {
	var <drawFunc;
	var <clearOnRefresh = true;
	var <>refreshOnFocus = true;
	var <relativeOrigin;

	var penID			= nil;

	draw {
		this.refresh;
	}

	refresh {
//		if( drawFunc.isNil, {
//			server.sendMsg( '/method', this.id, \repaint );
//		}, { });
		if( drawFunc.notNil, {
			JPen.protRefresh( drawFunc, this, server, penID, this.id );
		});
	}

	prHasFocus_ { arg focus;
		super.prHasFocus_( focus );
		// the user may wish to paint differently according to the focus
		if( refreshOnFocus and: { drawFunc.notNil }, { this.refresh });
	}

	prBoundsUpdated {
		if( drawFunc.notNil, { this.refresh });
	}

	clearOnRefresh_{ arg bool;
		clearOnRefresh = bool;
		this.setProperty( \clearOnRefresh, bool );
	}

	drawFunc_ { arg func;
		if( drawFunc.isNil, {
			if( func.notNil, {
				penID	= server.nextNodeID;
				server.sendBundle( nil,
					[ '/local', penID, '[', '/new', "de.sciss.swingosc.Pen", '[', '/ref', this.id, ']', relativeOrigin.not, ']' ],
					[ '/method', this.id, \setPen, '[', '/ref', penID, ']' ]
				);
				drawFunc = func;
				this.refresh;
			});
		}, {
			if( func.isNil, {
				server.sendBundle( nil,
					[ '/method', this.id, \setPen, '[', '/ref', \null, ']' ],
					[ '/method', penID, \dispose ],
					[ '/free', penID ]
				);
				penID = nil;
				drawFunc = nil;
			}, {
				drawFunc = func;
				this.refresh;
			});
		});
	}
	
	focusVisible { ^this.getProperty( \focusVisible, true )}
	focusVisible_ { arg visible; this.setProperty( \focusVisible, visible )}

	prClose {
		this.drawFunc_( nil );
		^super.prClose;
	}

	protDraw {
		if( drawFunc.notNil, {
			// cmpID == nil --> don't repaint, because this
			// will be done already by JSCWindow, and hence
			// would slow down refresh unnecessarily
			JPen.protRefresh( drawFunc, this, server, penID, nil );
		});
	}
}

JSCUserView : JSCAbstractUserView {	*paletteExample { arg parent, bounds;
		^this.new( parent, bounds ).refreshOnFocus_( false ).drawFunc_({ arg view;
			var b = view.bounds, min = min( b.width, b.height ), max = max( b.width, b.height ),
			    num = (max / min).asInteger;
			JPen.addRect( b );
			JPen.clip;
			JPen.translate( b.left, b.top );
			JPen.scale( min, min );
			num.do({ 	arg i;
				var rel = i / num;
				JPen.fillColor = Color.hsv( rel, 0.4, 0.6 );
				JPen.addWedge( (0.5 + i) @ 0.5, 0.4, rel * pi + 0.2, 1.5pi );
				JPen.fill;
			});
		});
	}

	relativeOrigin_ { arg bool;
		relativeOrigin = bool;
		this.setProperty( \relativeOrigin, bool );
	}

	prSCViewNew {
		relativeOrigin	= false;
		jinsets			= Insets( 3, 3, 3, 3 );
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.UserView", ']' ]
		]);
	}

	prSendProperty { arg key, value;

		key	= key.asSymbol;

		// fix keys
		case { key === \relativeOrigin }
		{
			if( penID.notNil, { server.sendMsg( '/set', penID, \absCoords, value.not )});
			^nil;
		};
		^super.prSendProperty( key, value );
	}
}

JSCTextView : JSCView {

	var <stringColor, <font, <editable = true;
	var <autohidesScrollers = false, <hasHorizontalScroller = false, <hasVerticalScroller = false;
	var <usesTabToFocusNextView = true, <enterInterpretsSelection = true;
//	var <textBounds;

	var txResp;
	
	var <string = "";
	var selStart = 0, selStop = 0;

//	mouseDown { arg clickPos;
////		this.focus(true);
//		mouseDownAction.value( this, clickPos );	
//	}	
	
//	string {
//		^this.getProperty( \string );
//	}

	string_ { arg str;
		^this.setString( str, -1 );
	}
		
	selectedString {
//		^this.getProperty( \selectedString );
		^string.copyRange( selStart, selStop - 1 );  // stupid inclusive ending
	}
	
	selectedString_{ arg str;
		this.setProperty( \selectedString, str );
		// XXX
	}
	
	selectionStart {
//		^this.getProperty( \selectedRangeLocation );
		^selStart;
	}
	
	selectionSize {
//		^this.getProperty( \selectedRange );
		^(selStop - selStart);
	}	
	
	stringColor_ {arg color;
		stringColor = color;
		this.setStringColor( color, -1, 0 );
	}
	
	setStringColor { arg color, rangeStart = -1, rangeSize = 0;
//		this.setProperty( \setTextColor, [ color, rangeStart, rangeSize ]);
		server.listSendMsg([ '/method', this.id, \setForeground, rangeStart, rangeSize ] ++ color.asSwingArg );
	}
	
	font_ { arg afont;
		font = afont;
		this.setFont( font, -1, 0 );
	}
	
	setFont { arg font, rangestart = -1, rangesize = 0;
//		this.setProperty( \setFont, [ font, rangestart, rangesize ]);
		server.listSendMsg([ '/method', this.id, \setFont, rangestart, rangesize ] ++ font.asSwingArg );
	}
	
	setString { arg string, rangestart = 0, rangesize = 0;
		var bndl, off, len, bndlSize;
	
//		string		= string.asString;
		
		// server.options.oscBufSize - sizeof([ '/method', 1234, \setString, 0, 1, "" ])
		if( string.size <= (server.options.oscBufSize - 44), {
			server.sendMsg( '/method', this.id, \setString, rangestart, rangesize, string );
		}, {
			bndl	= Array( 3 );
			off	= 0;
			// [ #bundle, [ '/method', 1234, \beginDataUpdate ],
			//            [ '/method', 1234, \addData, "GA" ],
			//            [ '/method', 1234, \endDataUpdate, 0, 1 ]
			bndlSize = 136;
			bndl.add([ '/method', this.id, \beginDataUpdate ]);
			while({ off < string.size }, {
				len = min( string.size - off, server.options.oscBufSize - bndlSize );
				bndl.add([ '/method', this.id, \addData, string.copyRange( off, off + len - 1 )]);				off = off + len;
				if( off < string.size, {
					server.listSendBundle( nil, bndl );
					bndl = Array( 2 );
					bndlSize = 100; // wie oben, jedoch ohne \beginDataUpdate
				});
			});
			bndl.add([ '/method', this.id, \endDataUpdate, rangestart, rangesize ]);
			server.listSendBundle( nil, bndl );
		});
	}
	
	editable_ { arg bool;
		editable = bool;
		server.sendMsg( '/set', this.id, \editable, bool );
	}
	
// JJJ begin comment
//	enabled_{|bool|
//		this.editable(bool);
//	}
// JJJ end comment

	usesTabToFocusNextView_ { arg bool;
		usesTabToFocusNextView = bool;
		this.setProperty( \usesTabToFocusNextView, bool );
	}
	
	enterInterpretsSelection_ { arg bool;
		enterInterpretsSelection = bool;
		this.setProperty( \enterExecutesSelection, bool );
	}
	
	autohidesScrollers_ { arg bool;
		autohidesScrollers = bool;
//		this.setProperty( \setAutohidesScrollers, bool );
		this.prUpdateScrollers;
	}
	
	hasHorizontalScroller_{ arg bool;
		hasHorizontalScroller = bool;
//		this.setProperty( \setHasHorizontalScroller, bool );
		this.prUpdateScrollers;
	}
	
	hasVerticalScroller_{ arg bool;
		hasVerticalScroller = bool;
//		this.setProperty( \setHasVerticalScroller, bool );
		this.prUpdateScrollers;
	}
	
// what's the point about this method??
//	textBounds_{ arg rect;
//		textBounds = rect;
//		this.setProperty(\textBounds, rect);
//	}

	caretColor { ^this.getProperty( \caretColor )}
	caretColor_ { arg color; this.setProperty( \caretColor, color )}

	openURL { arg url;
		server.sendMsg( '/method', this.id, \setPage, '[', '/new', "java.net.URL", url, ']' );
		// XXX update client send string rep.
	}

	prIsInsideContainer { ^true }

	prSCViewNew {
//		properties.put( \value, 0 );
		
		txResp = OSCpathResponder( server.addr, [ '/doc', this.id ], { arg time, resp, msg;
			var state, str;
			
			state = msg[2];
	
			case
			{ state === \insert }
			{
//				("insert at "++msg[3]++" len "++msg[4]++" text='"++msg[5]++"'").postln;
				str = msg[5].asString;
if( msg[4] != str.size, { ("Yukk. len is "++msg[4]++"; but string got "++str.size).postln });
				string = string.insert( msg[3], str );
				if( action.notNil, {{ action.value( this, state, msg[3], msg[4], str )}.defer });
			}
			{ state === \remove }
			{
//				("remove from "++msg[3]++" len "++msg[4]).postln;
				string = string.keep( msg[3] ) ++ string.drop( msg[3] + msg[4] );
				if( action.notNil, {{ action.value( this, state, msg[3], msg[4] )}.defer });
			}
			{ state === \caret }
			{
//				("caret now between "++msg[3]++" and "++msg[4]).postln;
				if( msg[3] < msg[4], {
					selStart	= msg[3];
					selStop	= msg[4];
				}, {
					selStart	= msg[4];
					selStop	= msg[3];
				});
				if( action.notNil, {{ action.value( this, state, msg[3], msg[4] )}.defer });
			};
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.TextView", ']',
				"cn" ++ this.id,				 	// bars : v=never, h=never
				'[', '/new', "javax.swing.JScrollPane", '[', '/ref', this.id, ']', 21, 31, ']',
				"tx" ++ this.id,
				'[', '/new', "de.sciss.swingosc.DocumentResponder", this.id, ']'
			]
		]);
	}

	prUpdateScrollers {
		server.sendMsg( '/set', "cn" ++ this.id,
			\horizontalScrollBarPolicy, hasHorizontalScroller.if( autohidesScrollers.if( 30, 32 ), 31 ),
			\verticalScrollBarPolicy, hasVerticalScroller.if( autohidesScrollers.if( 20, 22 ), 21 ));
	}

//	prSendProperty { arg key, value;
//		var bndl, string, rangeStart, rangeSize;
//
//		key	= key.asSymbol;
//
//		// fix keys
//		case
//		{ key === \setAutohidesScrollers or: { key === \setHasVerticalScroller or: { key === \setHasHorizontalScroller }} }
//		{
////("hasH "++hasHorizontalScroller++"; hasV "++hasVerticalScroller++"; autoHide "++autohidesScrollers).postln;
//			^nil;
//		};
//		^super.prSendProperty( key, value );
//	}

	prClose {
		txResp.remove;
		^super.prClose([[ '/method', "tx" ++ this.id, \remove ]])
	}

	defaultKeyDownAction { arg key, modifiers, unicode;
		// check for 'ctrl+enter' = interprete
		if( (unicode == 0x0D) and: { ((modifiers & 0x40000) != 0) && enterInterpretsSelection }, {
			if( selStop > selStart, {	// text is selected
				this.selectedString.interpretPrint;
			}, {
				this.prCurrentLine.interpretPrint;
			});
			^this;
		});
		^nil;
	}
	
	prCurrentLine {
		var startIdx, stopIdx;
		
		startIdx	= string.findBackwards( "\n", false, selStart - 1 ) ? 0;
		stopIdx	= string.find( "\n", false, selStart ) ?? { string.size };
		^string.copyRange( startIdx, stopIdx - 1 );
	}
}

JSCAbstractMultiSliderView : JSCView { 

	var <>metaAction;
	var <size = 0;
		
	properties {
		^super.properties ++ #[ \value, \strokeColor, \x, \y, \drawLines, \drawRects, \selectionSize, \step ]; // JJJ not thumbSize, thumbWidth, not absoluteX
	}
	
	step_ { arg stepSize; this.setPropertyWithAction( \step, stepSize )}
	
	step { ^this.getProperty( \step )}
	
	selectionSize { ^this.getProperty( \selectionSize )}

	selectionSize_ { arg aval; this.setProperty( \selectionSize, aval )}

	currentvalue { // returns value of selected index
		^this.getProperty( \y );
	}
	
	strokeColor_ { arg acolor; this.setProperty( \strokeColor, acolor )}

	currentvalue_ { arg iny; this.setProperty( \y, iny )}
	
	drawLines { arg abool; this.setProperty( \drawLines, abool )}

	drawLines_ { arg abool; this.drawLines( abool )}
	
	drawRects_ { arg abool; this.setProperty( \drawRects, abool )}

	defaultCanReceiveDrag {	^true }
			
	doMetaAction { 
		metaAction.value(this)
	} //on ctrl click

	prNeedsTransferHandler { ^true }
}

JSCMultiSliderView : JSCAbstractMultiSliderView { 

	var acResp;	// OSCpathResponder for action listening
	var vlResp;	// OSCpathResponder for value update listening
	var clpse;

	var <gap;
	var <editable = true;
	var <elasticMode = 0;
		
	properties {
		^super.properties ++ #[ \elasticResizeMode, \fillColor, \thumbWidth, \thumbHeight, \xOffset, \showIndex, \startIndex, \referenceValues, \isFilled, \readOnly ];	// JJJ not \thumbSize, but \thumbHeight, added \readOnly
	}
	
	elasticMode_{ arg mode;
		elasticMode = mode;
		this.setProperty( \elasticResizeMode, mode );
	}

	value { // returns array
		^this.getProperty( \value, Array.newClear( this.size ));
	}
	
	value_ { arg val;
		size = val.size;
		this.setProperty( \value, val.copy );
	}

	valueAction_ { arg val;
		this.size = val.size;	
		this.setPropertyWithAction( \value, val.copy );
	}
	
	reference { // returns array
		^this.getProperty( \referenceValues, Array.newClear( this.size ));
	}
	
	reference_ { arg val;
		// this.size = val.size;
		this.setProperty( \referenceValues, val );
	}
	
	index { // returns selected index
		^this.getProperty( \x );
	}
	
	index_ { arg inx;
		this.setProperty( \x, inx );
	}
	
	fillColor_ { arg acolor; this.setProperty( \fillColor, acolor )}

	colors_ { arg strokec, fillc;
		this.strokeColor_( strokec );
		this.fillColor_( fillc );
	}
	
	isFilled_ { arg abool;
		this.setProperty( \isFilled, abool );
	}
	
	xOffset_ { arg aval;
		this.setProperty( \xOffset, aval );
	}
	
	gap_ { arg inx;
		gap = inx;
		this.setProperty( \xOffset, inx );
	}
	
	startIndex_ { arg val; this.setProperty( \startIndex, val )}
	
	showIndex_ { arg abool; this.setProperty( \showIndex, abool )}
	
	// = thumb width
	indexThumbSize_ { arg val; this.setProperty( \thumbWidth, val )}

	// = thumb height
	valueThumbSize_ { arg val; this.setProperty( \thumbHeight, val )}

	indexIsHorizontal_ { arg val; this.setProperty( \isHorizontal, val )}
	
	thumbSize_ { arg val;
		properties.put( \thumbWidth, val );
		properties.put( \thumbHeight, val );
		server.sendMsg( '/set', this.id, \thumbSize, val );
	}
	
	readOnly_ { arg val;
		editable = val.not;
		this.setProperty( \readOnly, val );
	}
	
	editable_ { arg val;
		editable = val;
		this.setProperty( \readOnly, editable.not );
	}
	
	defaultReceiveDrag {
		if( currentDrag[ 0 ].isSequenceableCollection, { 
			this.value_( currentDrag[ 0 ]);
			this.reference_( currentDrag[ 1 ]);
		}, {
			this.value_( currentDrag );
		});
	}
	
	defaultGetDrag {
		var setsize, vals, rvals, outval;
		rvals = this.reference;
		vals = this.value;
		if( this.selectionSize > 1, {
			vals = vals.copyRange( this.index, this.selectionSize + this.index );
		});
		if( rvals.isNil, { 
			^vals; 
		}, {
			if( this.selectionSize > 1, {
				rvals = rvals.copyRange( this.index, this.selectionSize + this.index );
			});
			outval = outval.add( vals );
			outval = outval.add( rvals );
		});
		^outval;
	}
		
	defaultKeyDownAction { arg key, modifiers, unicode;
		//modifiers.postln; 16rF702
		if (unicode == 16rF703, { this.index = this.index + 1; ^this });
		if (unicode == 16rF702, { this.index = this.index - 1; ^this });
		if (unicode == 16rF700, { this.gap = this.gap + 1; ^this });
		if (unicode == 16rF701, { this.gap = this.gap - 1; ^this });
		^nil		// bubble if it's an invalid key
	}
	
	prClose {
		vlResp.remove;
		acResp.remove;
		clpse.cancel;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
		                [ '/free', "ac" ++ this.id ]]);
	}

	prSCViewNew {
		var initVal;
		initVal	= 0 ! 8;
		properties.put( \value, initVal );
		properties.put( \x, 0 );
		properties.put( \y, 0.0 );
		properties.put( \step, 0.0 );
		jinsets	= Insets( 3, 3, 3, 3 );
		clpse	= Collapse({ this.doAction });
		vlResp	= OSCpathResponder( server.addr, [ '/values', this.id ], { arg time, resp, msg;
			var dirtyIndex, dirtySize, vals, selectedIndex;

			vals			= properties[ \value ];
			dirtyIndex	= min( msg[ 2 ], vals.size );
			dirtySize		= min( msg[ 3 ], vals.size - dirtyIndex );
			
			dirtySize.do({ arg i;
				vals[ dirtyIndex + i ] = msg[ 4 + i ];
			});
			selectedIndex	= this.getProperty( \x, -1 );
//("selectedIndex = "++selectedIndex++"; vals = "++vals).inform;
			if( (selectedIndex >= dirtyIndex) and: { selectedIndex < (dirtyIndex + dirtySize) }, {
				properties.put( \y, vals[ selectedIndex ]);
			});
			if( dirtySize > 0, { clpse.instantaneous });
		}).add;
		acResp = OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			var dirtyIndex, dirtySize, vals, selectedIndex;

			selectedIndex	= msg[ 4 ];
			properties.put( \x, selectedIndex );
			properties.put( \selectionSize, msg[ 6 ]);
			dirtyIndex	= msg[ 8 ];
			dirtySize		= msg[ 10 ];

			if( dirtySize == 0, {
				vals = properties[ \value ];
				properties.put( \y, vals[ selectedIndex ]);
				clpse.instantaneous;
			});
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.MultiSlider", ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionMessenger",  // ActionResponder
					this.id, '[', '/array', \selectedIndex, \selectionSize, \dirtyIndex, \dirtySize, ']',
					\sendValuesAndClear, '[', '/array', this.id, ']', ']' ],
			[ '/set', this.id, \values ] ++ initVal.asSwingArg
		]);
	}

	prSendProperty { arg key, value;
		key	= key.asSymbol;

		// fix keys
		switch( key,
		\value, {
			key = \values;
			this.prFixValues;
		},
		\x, {
			key = \selectedIndex;
		},
		\isFilled, {
			key = \filled;
		},
		\isHorizontal, {
			key 		= \orientation;
			value	= if( value, 0, 1 );
		},
		\step, {
			key = \stepSize;
			this.prFixValues;
		});
		^super.prSendProperty( key, value );
	}
	
	prFixValues {
		var val, step;
		
		val	= properties[ \value ];
		step	= this.step;
		if( step > 0, {
			val.size.do({ arg i; val[ i ] = val[ i ].round( step ).clip( 0.0, 1.0 )});
		}, {
			val.size.do({ arg i; val[ i ] = val[ i ].clip( 0.0, 1.0 )});
		});
	}
}

JSCEnvelopeView : JSCAbstractMultiSliderView {
	var allConnections, selection;
	var items;
	var connectionsUsed = false;
	var idx = 0;	// the one that corresponds to select, x_ and y_
	
	var acResp;
	var vlResp;
	var clpse;

	*paletteExample { arg parent, bounds;
		^this.new( parent, bounds ).value_([ (0..4)/4, sqrt( (0..4)/4 )])
			.thumbSize_( 4 ).drawLines_( true ).selectionColor_( Color.red );
	}

	properties {
		^super.properties ++ #[ \font, \selectedIndex, \clipThumbs, \lockBounds, \horizontalEditMode ];  // \lastIndex
	}

// rather useless behaviour in SCEnvelopeView (using shift+click you can ignore it),
// so keep it out for now
//	var <fixedSelection = false;
	
	value_ { arg val;
		var oldSize, xvals, yvals, curves, valClip;
		
		oldSize	= size;
		xvals	= val[ 0 ];
		yvals	= val[ 1 ];
		curves	= val[ 2 ];
		if( xvals.size != yvals.size, {
			Error( "JSCEnvelopeView got mismatched times/levels arrays" ).throw;
		});
		size 	= xvals.size;
		case
		{ oldSize < size }
		{
			if( allConnections.notNil, {
				allConnections = allConnections.growClear( size );
			});
			if( items.notNil, {
				items = items.growClear( size );
			});
			selection = selection.growClear( size ).collect({ arg sel; if( sel.isNil, false, sel )});
		}
		{ oldSize > size }
		{
			if( allConnections.notNil, {
				allConnections = allConnections.copyFromStart( size - 1 );
			});
			if( items.notNil, {
				items = items.copyFromStart( size - 1 );
			});
			selection = selection.copyFromStart( size - 1 );
		};

		if( curves.isNil, {
			valClip = [ xvals.collect({ arg x; x.clip( 0.0, 1.0 )}), yvals.collect({ arg y; y.clip( 0.0, 1.0 )})];
		}, {
			valClip = [ xvals.collect({ arg x; x.clip( 0.0, 1.0 )}), yvals.collect({ arg y; y.clip( 0.0, 1.0 )}), curves ];
		});
		this.setProperty( \value, valClip );
	}
	
	setString { arg index, astring;
		if( items.isNil, {
			items = Array.newClear( size );
		});
		if( index < 0, {
			items.fill( astring );
		}, { if( index < size, {
			items[ index ] = astring;
		})});
		// items = items.add( astring );
//		this.setProperty( \string, [ index, astring ]);
		server.listSendMsg([ '/method', this.id, \setLabel, index ] ++ astring.asSwingArg );
	}

	strings_ { arg astrings;
		astrings.do({ arg str,i;
//			this.string_( i, str );
			this.setString( i, str );
		});
	}
	
	strings {
		^items.copy;	// nil.copy allowed
	}
	
//	items_ { arg items; ^this.strings_( items )}
	
	value {
//		var ax, ay, axy;
//		ax = Array.newClear( this.size );
//		ay = Array.newClear( this.size );
//		axy = Array.with( ax, ay );
//		^this.getProperty( \value, axy );
		^properties[ \value ].deepCopy;
	}
	
	selection {
		^selection.copy;
	}
	
	connections {
		var result;
		if( allConnections.isNil, { ^nil });
		
		result = Array( allConnections.size );
		allConnections.do({ arg cons; result.add( cons.copy )});
		^result;
	}
	
	setThumbHeight { arg index, height;
//		this.setProperty( \thumbHeight, [ index, height ]);
		server.sendMsg( '/method', this.id, \setThumbHeight, index, height );
	}
	
	thumbHeight_ { arg height; this.setThumbHeight( -1, height )}
	
	setThumbWidth { arg index, width;
//		this.setProperty( \thumbWidth, [ index, width ]);
		server.sendMsg( '/method', this.id, \setThumbWidth, index, width );
	}

	thumbWidth_ { arg width; this.setThumbWidth( -1, width )}

	setThumbSize { arg index, size;
//		this.setProperty(\thumbSize, [index, size]);
		server.sendMsg( '/method', this.id, \setThumbSize, index, size );
	}
	
	thumbSize_ { arg size; this.setThumbSize( -1, size )}

	setFillColor { arg index, color;
//		this.setProperty(\fillColor, [index, color]);
		server.listSendMsg([ '/method', this.id, \setFillColor, index ] ++ color.asSwingArg );
	}

	fillColor_ { arg color; this.setFillColor( -1, color )}

	colors_ { arg strokec, fillc;
		this.strokeColor_( strokec );
		this.fillColor_( fillc );
	}
	
	setCurve { arg index, curve = \lin;
		var shape, value, curves;
		value  = properties[ \value ];
		curves = value[ 2 ];
		if( index == -1, {
			if( curves.notNil, {
				curves.fill( curve );
			}, {
//				("AAA " ++ value).postln;
				properties[ \value ] = value ++ [(curve ! value[1].size)];
//				("AAA " ++ properties[ \value ]).postln;
			});
		}, { if( index < size, {
			if( curves.notNil, {
				curves[ index ] = curve;
			}, {
//				("BBB " ++ value).postln;
				properties[ \value ] = value ++ [(\lin ! value[1].size).put( index, curve )];
//				("BBB " ++ properties[ \value ]).postln;
			});
		})});
		if( curve.isFloat, {
			shape = 5;
		}, {
			shape = Env.shapeNames[ curve ];
			curve = 0.0;
		});
		server.sendMsg( '/method', this.id, \setShape, index, shape, curve );
	}
	
	curve_ { arg curve = \lin; this.setCurve( -1, curve )}

	lockBounds_ { arg val; this.setProperty( \lockBounds, val )}
	
	horizontalEditMode_ { arg val; this.setProperty( \horizontalEditMode, val )}
	
	connect { arg from, aconnections;
		var bndl, target, targetCons, fromCons;

		if( (from < 0) || (from >= size), { ^this });

		bndl			= Array( aconnections.size + 1 ); // max. number of messages needed
		fromCons		= Array( aconnections.size );

		if( connectionsUsed.not, {
			bndl.add([ '/set', this.id, \connectionsUsed, true ]);
			connectionsUsed	= true;
			allConnections	= Array.newClear( size );
		});

		aconnections.do({ arg target;
			target = target.asInteger;
			if( (target >= 0) && (target < size) && (target != from), {
				fromCons.add( target );
				targetCons = allConnections[ target ];
				if( targetCons.isNil or: { targetCons.includes( from ).not }, {
					targetCons = targetCons ++ [ from ];
					allConnections[ target ] = targetCons;
					// don't draw connections twice, so simply set only connections on the server whose target idx is greater than from idx
					bndl.add([ '/method', this.id, \setConnections, target ] ++ targetCons.reject({ arg idx; idx < target }).asSwingArg );
				});
			});
		});
		allConnections[ from ] = fromCons;
		bndl.add([ '/method', this.id, \setConnections, target ] ++ fromCons.reject({ arg idx; idx < from }).asSwingArg );
		server.listSendBundle( nil, bndl );
	}

	select { arg index; // this means no refresh;
		var vals;
//		this.setProperty(\setIndex, index);
		idx = index;
		if( (idx >= 0) && (idx < size), {
			vals = properties[ \value ];
			properties.put( \x, vals[ 0 ][ index ]);
			properties.put( \y, vals[ 1 ][ index ]);
		});
		server.sendMsg( '/set', this.id, \index, index );
	}
	
	selectIndex { arg index; // this means that the view will be refreshed
//		this.setProperty( \selectedIndex, index );
		properties.put( \selectedIndex, index );
		if( (idx >= 0) && (idx < size), {
			selection[ index ] = true;
		});
		server.sendMsg( '/method', this.id, \setSelected, index, true );
	}
	
	deselectIndex { arg index; // this means that the view will be refreshed
//		properties.put( \selectedIndex, index );
		if( (idx >= 0) && (idx < size), {
			selection[ index ] = false;
		});
		server.sendMsg( '/method', this.id, \setSelected, index, false );
	}
	
	x { ^this.getProperty( \x )}  // returns selected x
	y { ^this.getProperty( \y )}

	x_ { arg ax;
		ax = ax.round( this.step ).clip( 0.0, 1.0 );
		if( idx == -1, {
			properties[ \value ][ 0 ].fill( ax );
		}, { if( idx < size, {
			properties[ \value ][ 0 ][ idx ] = ax;
		})});
		this.setProperty( \x, ax );
	}

	y_ { arg ay;
		ay = ay.round( this.step ).clip( 0.0, 1.0 );
		if( idx == -1, {
			properties[ \value ][ 1 ].fill( ay );
		}, { if( idx < size, {
			properties[ \value ][ 1 ][ idx ] = ay;
		})});
		this.setProperty( \y, ay )
	}

	index { ^this.getProperty( \selectedIndex )}

//	lastIndex { ^this.getProperty( \lastIndex )}

	setEditable { arg index, boolean;
//		this.setProperty(\editable, [index,boolean]);
		server.sendMsg( '/method', this.id, \setReadOnly, index, boolean.not );
	}

	editable_{ arg boolean; this.setEditable( -1, boolean )}	
	selectionColor_ { arg acolor; this.setProperty( \selectionColor, acolor )}
	
	defaultGetDrag { ^this.value }

// currently broken in cocoa
/*
	defaultReceiveDrag {
		if( currentDrag.isString, {
			this.addValue;
//			items = items.insert( this.lastIndex + 1, currentDrag );
//			this.strings_( items );
			this.setString( this.lastIndex + 1, currentDrag );
		}, {
			this.value_( currentDrag );
		});
	}
*/
	defaultReceiveDrag { }
	
	defaultKeyDownAction { arg key, modifiers, unicode;
		var oldIdx, selIdx;

// gap is not working with envelope view!
//		if (unicode == 16rF700, { this.gap = this.gap + 1; ^this });
//		if (unicode == 16rF701, { this.gap = this.gap - 1; ^this });

		if( (unicode >= 16rF700) && (unicode <= 16rF703), {  // cursor
			selIdx	= this.index;
			oldIdx	= idx;
			if( (selIdx >= 0) and: { selIdx - 1 < this.size }, {
				case
				{ unicode == 16rF703 }	// cursor right
				{
					if( (modifiers & 524288) == 0, {	// test for alt
						this.select( selIdx );
						this.x = this.x + max( this.step, 0.015625 );
						this.select( oldIdx );
					}, { if( (selIdx + 1) < this.size, {
						this.deselectIndex( selIdx );
						this.selectIndex( selIdx + 1 );
					})});
				}
				{ unicode == 16rF702 }	// cursor left
				{
					if( (modifiers & 524288) == 0, {	// test for alt
						this.select( selIdx );
						this.x = this.x - max( this.step, 0.015625 );
						this.select( oldIdx );
					}, { if( selIdx > 0, {
						this.deselectIndex( selIdx );
						this.selectIndex( selIdx - 1 );
					})});
				}
				{ unicode == 16rF700 }	// cursor up
				{
					this.select( selIdx );
					this.y = this.y + max( this.step, 0.015625 );
					this.select( oldIdx );
				}
				{ unicode == 16rF701 }	// cursor down
				{
					this.select( selIdx );
					this.y = this.y - max( this.step, 0.015625 );
					this.select( oldIdx );
				};
			});
			^this;
		});
		^nil;		// bubble if it's an invalid key
	}

// currently broken in cocoa
/*
	addValue { arg xval, yval;
		var arr, arrx, arry, aindx;
		// XXX could use custom server method!!
		aindx = this.lastIndex;
//		aindx.postln;
		if( xval.isNil && yval.isNil, {
			arr = this.value;
			arrx = arr @ 0;
			arry = arr @ 1;
			xval = arrx[ aindx ] + 0.05;
			yval = arry[ aindx ];
		});
		if( aindx < (arrx.size - 1), {
			arrx = arrx.insert( aindx + 1, xval );
			arry = arry.insert( aindx + 1, yval );
		}, {
			arrx = arrx.add( xval );
			arry = arry.add( yval );
		});		
		this.value_([ arrx, arry ]);
	}
*/

// see comment for <fixedSelection	
//	fixedSelection_ { arg bool;
//		fixedSelection =  bool;
//		this.setProperty(\setFixedSelection, bool);
//	}

	prClose {
		vlResp.remove;
		acResp.remove;
		clpse	= Collapse({ this.doAction });
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
		                [ '/free', "ac" ++ this.id ]]);
	}

	prSCViewNew {
		var initVal;
//		initVal	= nil ! 8 ! 2;	// pretty stupid
		initVal	= [[],[]];
		properties.put( \value, initVal );
		properties.put( \index, -1 );
		properties.put( \x, 0.0 );
		properties.put( \y, 0.0 );
//		properties.put( \lastIndex, -1 );	// 0 in cocoa ...
		properties.put( \selectedIndex, -1 );
		properties.put( \step, 0.0 );
		properties.put( \clipThumbs, false );
		selection	= [];
		jinsets	= Insets( 3, 3, 3, 3 );
		clpse	= Collapse({ this.doAction });
//		items	= Array.new;
		vlResp	= OSCpathResponder( server.addr, [ '/values', this.id ], { arg time, resp, msg;
			var dirtySize, vals, xvals, yvals, action = false;

			vals			= properties[ \value ];
			xvals		= vals[ 0 ];
			yvals		= vals[ 1 ];
			dirtySize		= msg[ 2 ];
			msg.copyToEnd( 3 ).clump( 4 ).do({ arg entry; var idx, x, y, sel;
				#idx, x, y, sel = entry;
				if( idx < xvals.size, {
					xvals[ idx ]		= x;
					yvals[ idx ]		= y;
					selection[ idx ]	= sel != 0;
					action			= true;
				});
			});
			if( action, { clpse.instantaneous });
		}).add;
		acResp = OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			var lastIndex, dirtySize;

			lastIndex	= msg[ 4 ];
			if( lastIndex >= 0, { properties.put( \selectedIndex, lastIndex )}); // \lastIndex
			dirtySize	= msg[ 5 ];

//			if( dirtySize == 0, {
//				vals = properties[ \value ];
//				properties.put( \y, vals[ selectedIndex ]);
//				clpse.instantaneous;
//			});
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.EnvelopeView", false, ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionMessenger",  // ActionResponder
					this.id, '[', '/array', \lastIndex, \dirtySize, ']',
					\sendDirtyValuesAndClear, '[', '/array', this.id, ']', ']' ],
		]);
	}

	prSendProperty { arg key, value;
		var ival, shapes, curvesSC, curves;
		
		key	= key.asSymbol;

		// fix keys
		switch( key,
		\value, {
			this.prFixValues;
			if( value.size < 3, {
				server.listSendMsg([ '/method', this.id, \setValues ] ++ value[ 0 ].asSwingArg ++ value[ 1 ].asSwingArg );
			}, {
				curvesSC = value[ 2 ];
				if( curvesSC.isArray, {
					shapes = Array( curvesSC.size );
					curves = Array( curvesSC.size );
					curvesSC.do({ arg curve;
						if( curve.isFloat, {
							shapes.add( 5 );
							curves.add( curve );
						}, {
							shapes.add( Env.shapeNames[ curve ]);
							curves.add( 0.0 );
						});
					});
				}, {
					if( curvesSC.isFloat, {
						shapes = 5;
						curves = curvesSC;
					}, {
						shapes = Env.shapeNames[ curvesSC ];
						curves = 0.0;
					});
				});
				server.listSendMsg([ '/method', this.id, \setValues ] ++ value[ 0 ].asSwingArg ++ value[ 1 ].asSwingArg ++ shapes.asSwingArg ++ curves.asSwingArg );
			});
			^this;
		},
		\step, {
			key = \stepSize;
			this.prFixValues;
		},
		\horizontalEditMode, {
			ival = [ \free, \clamp, \relay ].indexOf( value );
			if( ival.isNil, {ÊError( "Illegal edit mode '" ++ value ++ "'" ).throw });
			value = ival;
		});
		^super.prSendProperty( key, value );
	}

	prFixValues {
		var val, step;
		
		val	= properties[ \value ];
		step	= this.step;
		if( step > 0, {
			2.do({ arg j; var xyvals = val[ j ]; xyvals.size.do({ arg i; xyvals[ i ] = xyvals[ i ].round( step ).clip( 0.0, 1.0 )})});
		}, {
			2.do({ arg j; var xyvals = val[ j ]; xyvals.size.do({ arg i; xyvals[ i ] = xyvals[ i ].clip( 0.0, 1.0 )})});
		});
	}

	font { ^this.getProperty( \font )}

	font_ { arg argFont;
		this.setProperty( \font, argFont );
	}
	
	clipThumbs { ^this.getProperty( \clipThumbs )}

	clipThumbs_ { arg bool; this.setProperty( \clipThumbs, bool )}
}
