/*
 *	JSCTextEditBase
 *	(SwingOSC classes for SuperCollider)
 *
 *	Replacements for the (Cocoa) SCNumberBox.
 *
 *	@author		SuperCollider Developers
 *	@author		Hanns Holger Rutz
 *	@version		0.56, 25-Sep-07
 */
JSCTextEditBase : JSCStaticTextBase {

	var <>keyString;
	var	<>typingColor, <>normalColor;

	// ----------------- public instance methods -----------------

	caretColor { ^this.getProperty( \caretColor )}
	caretColor_ { arg color; this.setProperty( \caretColor, color )}

	value { ^object }
	value_ { arg val;
		keyString = nil;
		this.stringColor = normalColor;
		object = val;
		this.string = object.asString;
	}	
	valueAction_ { arg val;
		var prev;
		prev = object;
		this.value = val;
		if (object != prev, { this.doAction });
	}	
	boxColor {
		^this.getProperty(\boxColor, Color.new)
	}
	boxColor_ { arg color;
		this.setProperty(\boxColor, color)
	}

	properties {
		^super.properties ++ #[\boxColor]
	}

	// ----------------- private instance methods -----------------

	init { arg argParent, argBounds, id;
		typingColor = Color.red;
		normalColor = Color.black;
		parent = argParent.asView; // actual view
// cocoa does parent.asView once more. too cryptic IMO ?
//		this.prInit( parent, argBounds.asRect, this.class.viewClass, parent.server, id );
		this.prInit( parent.asView, argBounds.asRect, this.class.viewClass, parent.server, id );
		argParent.add( this );//maybe window or viewadapter
	}

	prSendProperty { arg key, value;
		key	= key.asSymbol;

		// fix keys
		case { key === \boxColor }
		{
			key = \background;
			if( value == Color.clear, {
				value = nil;
			});
		};
		^super.prSendProperty( key, value );
	}

// XXX TEST SWINGOSC 0.55
//	prNeedsTransferHandler { ^true }
}

JSCNumberBox : JSCTextEditBase {

	var <>step=1;

	var acResp;	// OSCpathResponder for action listening
	var txResp;
	var serverString = "";	// necessary coz we immediately store client-side on string_ !
	
	// ----------------- public class methods -----------------

	*paletteExample { arg parent, bounds;
		^this.new( parent, bounds ).value_( 123.456 ).maxDecimals_( 4 );
	}

	// ----------------- public instance methods -----------------

	increment { this.valueAction = this.value + step; }
	decrement { this.valueAction = this.value - step; }
	
	defaultKeyDownAction { arg char, modifiers, unicode;
		// standard chardown
		if (unicode == 16rF700, { this.increment; ^this });
// JJJ mostly handled by java
//		if (unicode == 16rF703, { this.increment; ^this });
		if (unicode == 16rF701, { this.decrement; ^this });
//		if (unicode == 16rF702, { this.decrement; ^this });
//		if ((char == 3.asAscii) || (char == $\r) || (char == $\n), { // enter key
//			if (keyString.notNil,{ // no error on repeated enter
//				this.valueAction_(keyString.asFloat);
//			});
//			^this
//		});
//		if (char == 127.asAscii, { // delete key
//			keyString = nil;
//			this.string = object.asString;
//			this.stringColor = normalColor;
//			^this
//		});
//		if (char.isDecDigit || "+-.eE".includes(char), {
//			if (keyString.isNil, { 
//				keyString = String.new;
//				this.stringColor = typingColor;
//			});
//			keyString = keyString.add(char);
//			this.string = keyString;
//			^this
//		});
		^nil		// bubble if it's an invalid key	}

	defaultGetDrag { 
		^object.asFloat
	}
	defaultCanReceiveDrag {
		^currentDrag.isNumber;
	}
	defaultReceiveDrag {
		this.valueAction = currentDrag;	
	}

	maxDecimals {
		^this.getProperty( \maxDecimals, 8 );
	}
	maxDecimals_ { arg val;
		val = max( 0, val.asInteger );
		if( val < this.minDecimals, {
			this.minDecimals_( val );
		});
		this.setProperty( \maxDecimals, val );
	}
	minDecimals {
		^this.getProperty( \minDecimals, 0 );
	}
	minDecimals_ { arg val;
		val = max( 0, val.asInteger );
		if( val > this.maxDecimals, {
			this.maxDecimals_( val );
		});
		this.setProperty( \minDecimals, val );
	}
	properties {
		^super.properties ++ #[ \minDecimals, \maxDecimals ];
	}

	// ----------------- private instance methods -----------------

	prClose {
		acResp.remove;
		txResp.remove;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					  [ '/method', "tx" ++ this.id, \remove ],
					   [ '/free', "ac" ++ this.id, "tx" ++ this.id ]]);
	}

	prSCViewNew {
		properties.put( \minDecimals, 0 );
		properties.put( \maxDecimals, 8 );
		acResp = OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			// don't call valueAction coz we'd create a loop
			object = msg[4];
			properties.put( \string, msg[4].asString );
			{ this.doAction; }.defer;
		}).add;
		txResp = OSCpathResponder( server.addr, [ '/doc', this.id ], { arg time, resp, msg;
			var state, str;
			
			state = msg[2];
	
			case
			{ state === \insert }
			{
				str = msg[5].asString;
// doesn't work for UTF-8 chars, therefore don't print the warning at the moment ...
//if( msg[4] != str.size, { ("JSCNumberBox. len is "++msg[4]++"; but string got "++str.size).postln });
				serverString = serverString.insert( msg[3], str );
				object = serverString.asFloat;
			}
			{ state === \remove }
			{
				serverString = serverString.keep( msg[3] ) ++ serverString.drop( msg[3] + msg[4] );
				object = serverString.asFloat;
			};
		}).add;
		^super.prSCViewNew([
			[ '/set', '[', '/local', this.id,
				'[', '/new', "de.sciss.swingosc.NumberField", ']', ']',
				\space, '[', '/new', "de.sciss.util.NumberSpace", inf, -inf, 0.0, 0, 8, ']' ],
			[ '/method', parent.id, \add, '[', "/ref", this.id, ']' ],
			[ '/local', "ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, \number, ']',
				"tx" ++ this.id,
				'[', '/new', "de.sciss.swingosc.DocumentResponder", this.id, ']' ]
		]);
	}

	prSendProperty { arg key, value;
		key	= key.asSymbol;

		// fix keys
		case { key === \string }
		{
			key 		= \number;
			value	= object; // .asFloat;
		}
		{ key === \minDecimals }
		{
			// send directly here because the array would
			// be distorted in super.prSendProperty by calling asSwingArg !!
			server.sendMsg( "/set", this.id, \space,
				'[', "/new", "de.sciss.util.NumberSpace", inf, -inf, 0.0, value, this.maxDecimals, ']'
			);
			^nil;
		}
		{ key === \maxDecimals }
		{
			// send directly here because the array would
			// be distorted in super.prSendProperty by calling asSwingArg !!
			server.sendMsg( "/set", this.id, \space,
				'[', "/new", "de.sciss.util.NumberSpace", inf, -inf, 0.0, this.minDecimals, value, ']'
			);
			^nil;
		};
		^super.prSendProperty( key, value );
	}
}