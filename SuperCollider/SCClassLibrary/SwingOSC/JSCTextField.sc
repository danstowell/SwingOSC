/*
 *	JSCTextField
 *	(SwingOSC classes for SuperCollider)
 *
 *	Replacements for the (Cocoa) SCTextField.
 *
 *	@author		SuperCollider Developers, Hanns Holger Rutz
 *	@version		0.51, 02-Mar-07
 *
 *	@todo		check if drag-n-drop is actually working ?
 */
JSCTextField : JSCTextEditBase {   // not a child class of JSCNumberBox

// JJJ
//	*viewClass { ^SCNumberBox }
	
	var acResp;	// OSCpathResponder for action listening
	var txResp;
	var serverString = "";	// necessary coz we immediately store client-side on string_ !
	

//	defaultKeyDownAction { arg key, modifiers, unicode;
//		if(unicode == 0,{ ^this });
//		// standard keydown
//		if ((key == 3.asAscii) || (key == $\r) || (key == $\n), { // enter key
//			if (keyString.notNil,{ // no error on repeated enter
//				this.valueAction_(string);
//				keyString = nil;// restart editing
//			});
//			^this
//		});
//		if (key == 127.asAscii, { // delete key
//			if(keyString.notNil,{
//				if(keyString.size > 1,{
//					keyString = keyString.copyRange(0,keyString.size - 2);
//				},{
//					keyString = String.new;
//				});
//				this.string = keyString;
//				this.stringColor = typingColor;
//			},{
//				keyString = String.new;
//				this.string = keyString;
//				this.stringColor = typingColor;
//			});
//			^this
//		});
//		if (keyString.isNil, { 
//			keyString = this.string;
//			this.stringColor = typingColor;
//		});
//		keyString = keyString.add(key);
//		this.string = keyString;
//	}

	string_ { arg s; super.string = s.as(String); }

	prClose {
		acResp.remove;
		txResp.remove;
		^super.prClose([[ '/method', "ac" ++ this.id, \remove ],
					  [ '/method', "tx" ++ this.id, \remove ],
					   [ '/free', "ac" ++ this.id, "tx" ++ this.id ]]);
	}

	prSCViewNew {
		acResp = OSCpathResponder( server.addr, [ '/action', this.id ], { arg time, resp, msg;
			// don't call valueAction coz we'd create a loop
			object = msg[4].asString;
			string = object;
			properties.put( \string, object );
			{ this.doAction; }.defer;
		}).add;
		txResp = OSCpathResponder( server.addr, [ '/doc', this.id ], { arg time, resp, msg;
			var state, str;
			
			state = msg[2];
	
			case
			{ state === \insert }
			{
				str = msg[5].asString;
if( msg[4] != str.size, { ("Yukk. len is "++msg[4]++"; but string got "++str.size).postln });
				string = serverString = serverString.insert( msg[3], str );
			}
			{ state === \remove }
			{
				string = serverString = serverString.keep( msg[3] ) ++ serverString.drop( msg[3] + msg[4] );
			};
		}).add;
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.TextField", ']',
				"ac" ++ this.id,
				'[', '/new', "de.sciss.swingosc.ActionResponder", this.id, \text, ']',
				"tx" ++ this.id,
				'[', '/new', "de.sciss.swingosc.DocumentResponder", this.id, ']' ]
		]);
	}
}