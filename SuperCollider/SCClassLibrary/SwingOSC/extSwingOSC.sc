/**
 *	(C)opyright 2006-2008 Hanns Holger Rutz. All rights reserved.
 *	Distributed under the GNU General Public License (GPL).
 *
 *	@author	SuperCollider Developers
 *	@author	Hanns Holger Rutz
 *	@version	0.57, 10-Dec-07
 */
+ Object {
	asSwingArg {
		^this;
	}

//	jinspect { "Object.jinspect : deprecated!".warn; ^this.jinspectorClass.new( this )}
//	jinspectorClass { ^JObjectInspector }
//	jinspector { 
//		// finds the inspector for this object, if any.
//		^JInspector.inspectorFor(this) 
//	}
}

//+ Class {
//	jinspectorClass { ^JClassInspector }
//}

+ ArrayedCollection {
	asSwingArg {
		^([ '[', '/array' ] ++ this.performUnaryOp( \asSwingArg ).flatten ++ ']');
	}

//	jplot { arg name, bounds, discrete=false, numChannels = 1;
//		var plotter, txt, chanArray, unlaced, val, minval, maxval, window, thumbsize, zoom, width, 
//			layout, write=false;
//		"ArrayedCollection.jplot : deprecated!".warn; 
//		bounds = bounds ?  Rect(200 , 140, 705, 410);
//		
//		width = bounds.width-8;
//		zoom = (width / (this.size / numChannels));
//		
//		if(discrete) {
//			thumbsize = max(1.0, zoom);
//		}{
//			thumbsize = 1;
//		};
//		
//		name = name ? "plot";
//		minval = this.minItem;
//		maxval = this.maxItem;
//		unlaced = this.unlace(numChannels);
//		chanArray = Array.newClear(numChannels);
//		unlaced.do({ |chan, j|
//			val = Array.newClear(width);
//			width.do { arg i;
//				var x;
//				x = chan.blendAt(i / zoom);
//				val[i] = x.linlin(minval, maxval, 0.0, 1.0);
//			};
//			chanArray[j] = val;
//		});
//		window = JSCWindow(name, bounds);
//		txt = JSCStaticText(window, Rect(8, 0, width, 18))
//				.string_("index: 0, value: " ++ this[0].asString);
//		layout = JSCVLayoutView(window, Rect(4, txt.bounds.height, width, 
//			bounds.height - 30 - txt.bounds.height)).resize_(5);
//		numChannels.do({ |i|
//			plotter = JSCMultiSliderView(layout, Rect(0, 0, 
//					layout.bounds.width,layout.bounds.height))
//				.readOnly_(true)
//				.drawLines_(discrete.not)
//				.drawRects_(discrete)
//				.thumbSize_(thumbsize) 
//				.valueThumbSize_(1)
//				.background_(Color.white)
//				.colors_(Color.black, Color.blue(1.0,1.0))
//				.action_({|v| 
//					var curval;
//					curval = v.currentvalue.linlin(0.0, 1.0, minval, maxval);
//					
//					txt.string_("index: " ++ (v.index / zoom).roundUp(0.01).asString ++ 
//					", value: " ++ curval);
//					if(write) { this[(v.index / zoom).asInteger]  = curval };
//				})
//				.keyDownAction_({ |v, char|
//					if(char === $l) { write = write.not; v.readOnly = write.not;  };
//				})
//				.value_(chanArray[i])
//				.resize_(5)
//				.elasticMode_(1);
//				
//		});
//		
//		^window.front;
//		
//	}
}

+ List {
	asSwingArg {
		^([ '[', '/method', "java.util.Arrays", \asList ] ++ this.asArray.asSwingArg ++ [ ']' ]);
	}
}

+ Nil {
	asSwingArg {
		^([ '[', '/ref', \null, ']' ]);
	}
}

//+ Int8Array {
//	asSwingArg { ^this; }
//}

+ String {
	// String is a subclass of ArrayedCollection!!
	asSwingArg {
		case { this.size !== 1 }
		{
			^[ this ];
		}
		{ this == "[" }	// must be escaped
		{
			^([ '[', "/ref", "brko", ']' ]);
		}
		{ this == "]" }	// must be escaped
		{
			^([ '[', "/ref", "brkc", ']' ]);
		}
		{
			^[ this ];
		};
	}

//	jinspectorClass { ^JStringInspector }
//
//	jspeak { arg channel = 0, force = false;
//		"String.jspeak : deprecated!".warn;
//		GUI.useID( \swing, { this.speak( channel, force )});
//	}
}

+ Symbol {
	asSwingArg {
		^this.asString.asSwingArg;
	}
}

+ Color {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Color', this.red.asFloat, this.green.asFloat, this.blue.asFloat, this.alpha.asFloat, ']' ]);
	}
}

+ JFont {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Font', this.name, this.style, this.size, ']' ]);
	}
}

+ Point {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Point', this.x, this.y, ']' ]);
	}
}

+ Rect {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Rectangle', this.left, this.top, this.width, this.height, ']' ]);
	}
}

// Note: Gradient Paining doesn't work
// , at least with Aqua lnf the panels are not painted properly
+ Gradient {
	asSwingArg {
		^([ '[', '/new', 'java.awt.GradientPaint', 0.0, 0.0 ] ++ color1.asSwingArg ++ [
			if( direction == \h, 1.0, 0.0 ), if( direction == \h, 0.0, 1.0 )] ++ color2.asSwingArg ++ [ ']' ]);
	}
}

+ HiliteGradient {
	asSwingArg {
		^([ '[', '/new', 'java.awt.GradientPaint', 0.0, 0.0 ] ++ color1.asSwingArg ++ [
			if( direction == \h, frac, 0.0 ), if( direction == \h, 0.0, frac )] ++ color2.asSwingArg ++ [ ']' ]);
	}
}

// a bit stupid, this is the original code with SCWindow, SCButton and SCStaticText
// replaced. it would be better to query the GUI classes instead and not duplicate
// all methods
+ Server {
//	jmakeWindow { arg w;
//		var active, booter, killer, makeDefault, running, booting, stopped;
//		var recorder, scoper;
//		var countsViews, ctlr;
//		var dumping=false;
//		"Server.jmakeWindow : deprecated!".warn; 
//		
//		if (window.notNil, { ^window.front });
//		
//		if(w.isNil,{
//			w = window = JSCWindow(name.asString ++ " server", 
//						Rect(10, named.values.indexOf(this) * 120 + 10, 306, 92));
//			w.view.decorator = FlowLayout(w.view.bounds);
//		});
//		
//		if(isLocal,{
//			booter = JSCButton(w, Rect(0,0, 48, 24));
//			booter.states = [[ "Boot" ],
//						   [ "Quit" ]];
//			
//			booter.action = { arg view; 
//				if(view.value == 1, {
//					booting.value;
//					this.boot;
//				});
//				if(view.value == 0,{
//					this.quit;
//				});
//			};
//			booter.setProperty(\value,serverRunning.binaryValue);
//			
//			killer = JSCButton(w, Rect(0,0, 24, 24));
//			killer.states = [[ "K" ]];
//			
//			killer.action = { Server.killAll };	
//		});
//		
//		active = JSCStaticText(w, Rect(0,0, 78, 24));
//		active.string = this.name.asString;
//		active.align = \center;
//		active.font = JFont("Helvetica-Bold", 16);
//		active.background = Color.black;
//		if(serverRunning,running,stopped);		
//
//		makeDefault = JSCButton(w, Rect(0,0, 60, 24));
//		makeDefault.states = [[ "-> default" ]];
//		makeDefault.action = {
//			thisProcess.interpreter.s = this;
//			Server.default = this;
//		};
//
//		//w.view.decorator.nextLine;
//		
//		recorder = JSCButton(w, Rect(0,0, 72, 24));
//		recorder.states = [
//			[ "prepare rec" ],
//			["record >", Color.red, Color.gray(0.1)],
//			["stop []", Color.black, Color.red]
//		];
//		recorder.action = {
//			if (recorder.value == 1) {
//				this.prepareForRecord;
//			}{
//				if (recorder.value == 2) { this.record } { this.stopRecording };
//			};
//		};
//		recorder.enabled = false;
//		
//		w.view.keyDownAction = { arg ascii, char;
//			var startDump, stopDump, stillRunning;
//			
//			case 
//			{char === $n} { this.queryAllNodes }
//			{char === $ } { if(serverRunning.not) { this.boot } }
//// JJJ
////			{char === $s and: {this.inProcess}} { this.scope }
//			{char === $s and: {this.inProcess.not}} { this.jscope }
//			{char == $d} {
//				if(this.isLocal or: { this.inProcess }) {//					stillRunning = {//						SystemClock.sched(0.2, { this.stopAliveThread });//					};//					startDump = { //						this.dumpOSC(1);//						this.stopAliveThread;//						dumping = true;//						CmdPeriod.add(stillRunning);//					};//					stopDump = {//						this.dumpOSC(0);//						this.startAliveThread;//						dumping = false;//						CmdPeriod.remove(stillRunning);//					};//					if(dumping, stopDump, startDump)//				} {//					"cannot dump a remote server's messages".inform//				}//
//
//			};
//		};
//		
//		if (isLocal, {
//			
//			running = {
//				active.stringColor_(Color.red);
//				booter.setProperty(\value,1);
//				recorder.enabled = true;
//			};
//			stopped = {
//				active.stringColor_(Color.grey(0.3));
//				booter.setProperty(\value,0);
//				recorder.setProperty(\value,0);
//				recorder.enabled = false;
//			};
//			booting = {
//				active.stringColor_(Color.yellow(0.9));
//				//booter.setProperty(\value,0);
//			};
//			
//			w.onClose = {
//				//OSCresponder.removeAddr(addr);
//				//this.stopAliveThread;
//				//this.quit;
//				window = nil;
//				ctlr.remove;
//			};
//		},{	
//			running = {
//				active.background = Color.red;
//				recorder.enabled = true;
//			};
//			stopped = {
//				active.background = Color.black;
//				recorder.setProperty(\value,0);
//				recorder.enabled = false;
//
//			};
//			booting = {
//				active.background = Color.yellow;
//			};
//			w.onClose = {
//				// but do not remove other responders
//				this.stopAliveThread;
//				ctlr.remove;
//			};
//		});
//		if(serverRunning,running,stopped);
//			
//		w.view.decorator.nextLine;
//
//		countsViews = 
//		#[
//			"Avg CPU :", "Peak CPU :", 
//			"UGens :", "Synths :", "Groups :", "SynthDefs :"
//		].collect({ arg name, i;
//			var label,numView, pctView;
//			label = JSCStaticText(w, Rect(0,0, 80, 14));
//			label.string = name;
//			label.align = \right;
//		
//			if (i < 2, { 
//				numView = JSCStaticText(w, Rect(0,0, 38, 14));
//				numView.string = "?";
//				numView.align = \left;
//			
//				pctView = JSCStaticText(w, Rect(0,0, 12, 14));
//				pctView.string = "%";
//				pctView.align = \left;
//			},{
//				numView = JSCStaticText(w, Rect(0,0, 50, 14));
//				numView.string = "?";
//				numView.align = \left;
//			});
//			
//			numView
//		});
//		
//		w.front;
//
//		ctlr = SimpleController(this)
//			.put(\serverRunning, {	if(serverRunning,running,stopped) })
//			.put(\counts,{
//				countsViews.at(0).string = avgCPU.round(0.1);
//				countsViews.at(1).string = peakCPU.round(0.1);
//				countsViews.at(2).string = numUGens;
//				countsViews.at(3).string = numSynths;
//				countsViews.at(4).string = numGroups;
//				countsViews.at(5).string = numSynthDefs;
//			})
//			.put(\cmdPeriod,{
//				recorder.setProperty(\value,0);
//			});	
//		this.startAliveThread;
//	}
//
//	jscope { arg numChannels, index, bufsize = 4096, zoom, rate;
//		"Server.jscope : deprecated!".warn; 
//		numChannels = numChannels ? 2;
//		if(scopeWindow.isNil) {
//			scopeWindow = 
//				JStethoscope.new(this, numChannels, index, bufsize, zoom, rate, nil, 
//					this.options.numBuffers); 
//					// prevent buffer conflicts by using reserved bufnum
//			CmdPeriod.add(this);
//		} {
//			scopeWindow.setProperties(numChannels, index, bufsize, zoom, rate);
//			scopeWindow.run;
//			scopeWindow.window.front;
//		};
//		^scopeWindow
//	}
	
	/**
	 *	There is a bug in unixCmd when running on MacOS X 10.3.9
	 *	which blocks successive unixCmd calls. This seems to
	 *	fix it (ONLY ONCE THOUGH). so call this method once after
	 *	you launching a second server
	 */
	unblockPipe {
		this.sendMsg( '/n_trace', 0 );
	}
}

//// as in UGen-scope, only Stethoscope replaced by JStethoscope, SCWindow by JSCWindow etc.
//+ UGen {
//	jscope { arg name = "UGen Scope", bufsize = 4096, zoom;
//		"UGen.jscope : deprecated!".warn; 
//		^SynthDef.wrap({ var bus, numChannels, rate, scope, screenBounds, x, y, right;
//			numChannels = this.numChannels;
//			rate = this.rate;
//// JJJ
////			bus = Bus.perform(rate, Server.internal, numChannels);
//			bus = Bus.perform(rate, Server.default, numChannels);
//			switch(rate, 
//				\audio, {Out.ar(bus.index, this)},
//				\control, {Out.kr(bus.index, this)}
//			);
//			
//			{
//				screenBounds = JSCWindow.screenBounds;
//				x = 544 + (JStethoscope.ugenScopes.size * 222);
//				right = x + 212;
//				y = floor(right / screenBounds.width) * 242 + 10;
//				if(right > screenBounds.right, {x = floor(right%screenBounds.width / 222) 
//					* 222; }); 
//// JJJ
////				scope = JStethoscope.new(Server.internal, numChannels, bus.index, bufsize, zoom, 
////					rate);
//				scope = JStethoscope.new(Server.default, numChannels, bus.index, bufsize, zoom, 
//					rate);
//				scope.window.name_(name.asString).bounds_(Rect(x, y, 212, 212));
//				JStethoscope.ugenScopes.add(scope);
//				scope.window.onClose = { scope.free; bus.free; 
//					JStethoscope.ugenScopes.remove(scope)};
//				CmdPeriod.doOnce({ {scope.window.close}.defer });
//			}.defer(0.001);
//			this;
//		})
//	}
//}
//
//+ Array {
//	jscope { arg name = "UGen Scope", bufsize = 4096, zoom;
//		"Array.jscope : deprecated!".warn; 
//		^SynthDef.wrap({ var bus, numChannels, rate, scope, screenBounds, x, y, right;
//			numChannels = this.numChannels;
//			rate = this.rate;
//// JJJ
////			bus = Bus.perform(rate, Server.internal, numChannels);
//			bus = Bus.perform(rate, Server.default, numChannels);
//			switch(rate, 
//				\audio, {Out.ar(bus.index, this)},
//				\control, {Out.kr(bus.index, this)}
//			);
//			
//			{
//				screenBounds = JSCWindow.screenBounds;
//				x = 544 + (JStethoscope.ugenScopes.size * 222);
//				right = x + 212;
//				y = floor(right / screenBounds.width) * 242 + 10;
//				if(right > screenBounds.right, {x = floor(right%screenBounds.width / 222) 
//					* 222; }); 
//// JJJ
////				scope = JStethoscope.new(Server.internal, numChannels, bus.index, bufsize, zoom, 
////					rate);
//				scope = JStethoscope.new(Server.default, numChannels, bus.index, bufsize, zoom, 
//					rate);
//				scope.window.name_(name.asString).bounds_(Rect(x, y, 212, 212));
//				JStethoscope.ugenScopes.add(scope);
//				scope.window.onClose = { scope.free; bus.free; 					JStethoscope.ugenScopes.remove(scope)};
//				CmdPeriod.doOnce({ {scope.window.close}.defer });
//			}.defer(0.001);
//			this;
//		})
//	}
//}
//
//+ Bus {
//	jscope { arg bufsize = 4096, zoom;
//		"Bus.jscope : deprecated!".warn; 
//		^server.jscope(numChannels, index, bufsize, zoom, rate)
//	}
//}
//
//+ Function {
//	jscope { arg numChannels, outbus = 0, fadeTime = 0.05, bufsize = 4096, zoom;
//		var synth, synthDef, bytes, synthMsg, outUGen, server;
//		"Function.jscope : deprecated!".warn; 
//// JJJ
////		server = Server.internal;
////		if(server.serverRunning.not) { "internal server not running!".postln; ^nil };
//		server = Server.default;
//		if(server.serverRunning.not) { "default server not running!".postln; ^nil };
//		def = this.asSynthDef(fadeTime:fadeTime);
//		outUGen = def.children.detect { |ugen| ugen.class === Out };
//		
//		numChannels = numChannels ?? { if(outUGen.notNil) { (outUGen.inputs.size - 1) } { 1 } };
//		synth = Synth.basicNew(def.name, server);
//		bytes = def.asBytes;
//		synthMsg = synth.newMsg(server, [\i_out, outbus, \out, outbus], \addToHead);
//		server.sendMsg("/d_recv", bytes, synthMsg);
//		server.jscope(numChannels, outbus, bufsize, zoom, outUGen.rate);
//		^synth
//	}
//
//	jplot { arg duration  = 0.01, server, bounds;
//		"Function.jplot : deprecated!".warn; 
//		this.loadToFloatArray(duration, server, { |array, buf|
//			var numChan;
//			numChan = buf.numChannels;
//			{
//				array.jplot(bounds: bounds, numChannels: numChan) 
//			}.defer;
//		})
//	}
//}
//
//+ Buffer {
//	jplot { arg name, bounds;
//		"Buffer.jplot : deprecated!".warn; 
//		this.loadToFloatArray(action: { |array, buf| {array.jplot(name, bounds, 
//			numChannels: buf.numChannels) }.defer;});
//	}
//}
//
//+ Env {
//	jplot { arg size = 400;
//		"Env.jplot : deprecated!".warn; 
//		this.asSignal(size).jplot;
//	}
//}
//
//+ Wavetable {
//	jplot { arg name, bounds;
//		"Wavetable.jplot : deprecated!".warn; 
//		^this.asSignal.jplot;
//	}
//}

// don't blame me for this hackery
+ SCViewHolder {
//	prIsInsideContainer {Ê^false }
	prSetScBounds {}
	prInvalidateChildBounds {}
	protDraw {}
	id { ^nil }	// this is detected by JSCContainerView!
}