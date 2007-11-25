/*
 *	JSCScope
 *	(SwingOSC classes for SuperCollider)
 *
 *	Replacement for the (Cocoa) SCScope class.
 *
 *	@author		SuperCollider Developers
 *	@author		Hanns Holger Rutz
 *	@version		0.53, 29-Apr-07
 */
JSCScope : JSCView {

	var audioServer;

	bufnum {
		^this.getProperty(\bufnum)
	}
	bufnum_ { arg num;
		this.setProperty(\bufnum, num);
	}	
	x {
		^this.getProperty(\x)
	}
	x_ { arg val;
		this.setProperty(\x, val);
	}	
	y {
		^this.getProperty(\y)
	}
	y_ { arg val;
		this.setProperty(\y, val);
	}	
	xZoom {
		^this.getProperty(\xZoom)
	}
	xZoom_ { arg val;
		this.setProperty(\xZoom, val);
	}	
	yZoom {
		^this.getProperty(\yZoom)
	}
	yZoom_ { arg val;
		this.setProperty(\yZoom, val);
	}	

	gridColor {
		^this.getProperty(\gridColor)
	}
	gridColor_ { arg color;
		this.setProperty(\gridColor, color);
	}	

	waveColors {
		^this.getProperty(\waveColors)
	}
	waveColors_ { arg arrayOfColors;
		this.setProperty(\waveColors, arrayOfColors);
	}
	
	style_ { arg val;
		this.setProperty(\style, val);
		// 0 = vertically spaced
		// 1 = overlapped
		// 2 = x/y
	}
	
	properties {
//		^super.properties ++ #[\bufnum, \x, \y, \xZoom, \yZoom, \gridColor, \waveColors, \style, \antiAliasing ]
		^super.properties ++ #[\bufnum, \x, \y, \xZoom, \yZoom, \gridColor, \waveColors, \style ]
	}

//	// JJJ begin
//	antiAliasing {
//		^this.getProperty( \antiAliasing );
//	}
//	antiAliasing_ { arg onOff;
//		this.setProperty( \antiAliasing, onOff );
//	}
//	// JJJ end

	prSCViewNew {
		var addr;
		
		audioServer	= Server.default;
		addr 		= audioServer.addr;
	
		properties.put( \bufnum, 0 );
		properties.put( \x, 0.0 );
		properties.put( \y, 0.0 );
		properties.put( \xZoom, 1.0 );
		properties.put( \yZoom, 1.0 );
		properties.put( \style, 0 );
//		properties.put( \antiAliasing, true );
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.ScopeView", ']' ],
			[ '/method', this.id, \setServer, addr.hostname, addr.port, audioServer.options.protocol ],
			[ '/method', this.id, \startListening ]
		]);
	}

	prClose {
		^super.prClose([[ '/method', this.id, \stopListening ]]);
	}

	prSendProperty { arg key, value;
//		var bndl, bufNum, numFrames, numChannels, sampleRate, resp, cond, timeOut;

		key	= key.asSymbol;

		switch( key,
			\bufnum, {
				key		= \bufNum;	// let the java class handle the /b_query stuff
//				cond		= Condition.new;
//				bufNum	= value.asInteger;
//				resp		= OSCpatResponder( audioServer.addr, [ '/b_info', bufnum ], { arg time, resp, msg;
//					// [ "/b_info", <bufNum>, <numFrames>, <numChannels>, <sampleRate> ]
//						timeOut.stop;
//						resp.remove;
//						numFrames		= msg[ 2 ].asInteger;
//						numChannels	= msg[ 3 ].asInteger;
//						sampleRate	= msg[ 4 ].asFloat;
//						cond.test		= true;
//						cond.signal;
//				});
//				timeOut	= Routine({
//					4.0.wait;
//					resp.remove;
//					cond.unhang;
//				}).play;
//				Routine({
//					cond.wait;
//					if( cond.test, {
//						server.sendMsg( "/method", this.id, \setBuffer, bufNum, numFrames, numChannels, sampleRate );
//					}, {
//						"JScopeView : timeout while changing buffer".error;
//					});
//				}).play;
//				resp.add;
//				audioServer.sendMsg( "/b_query", bufNum );
//				^nil;
			},
			\waveColors, {
				key = \objWaveColors;
			}
		);
		^super.prSendProperty( key, value );
	}
}
