/*
 *	SwingOSC
 *	(SwingOSC classes for SuperCollider)
 *
 *	The client side representation of a SwingOSC server
 *
 *	Changelog
 *	- 06-Mar-06	added fixes by AdC
 *	- 11-Jun-06	removed slowOSC stuff (fixed in SC)
 *	- 01-Oct-06	added SwingOptions and TCP mode
 *
 *	@author		Hanns Holger Rutz
 *	@version		0.56, 10-Oct-07
 */
SwingOptions
{
	classvar	<>default;

	var <>protocol 	= \tcp;
	var <>loopBack	= true;
	var <>initGUI		= true;
	var <>oscBufSize	= 65536;

	*initClass {
		default = this.new;
	}
	
	*new {
		default.notNil.if({
			^default.copy;
		}, {
			^super.new;
		});
	}

	// session-password

	asOptionsString { arg port = 57111;
		var o;
		o = if( protocol === \tcp, " -t ", " -u ");
		o = o ++ port;
		
		if( loopBack, { 
			o = o ++ " -L";
		});
		if( initGUI, { 
			o = o ++ " -i";
		});
		if( oscBufSize != 65536, {
			o = o ++ " -b " ++ oscBufSize;
		});
		^o
	}
}

SwingOSC : Model
{
	classvar <>local, <>default, <>named, <>set, <>program, <>clock;

	// WARNING: this field might be removed in a future version
	var <>useDoubles = false;

	// note this is the SC class lib version, not necessarily the
	// server version (reflected by the instance variable serverVersion)
	classvar <version = 0.56;

	var <name, <addr, <clientID = 0;
	var <isLocal;
	var <serverRunning = false, <serverBooting = false, <serverVersion;
	var <dumpMode = 0, <dumpModeR = 0;

	var <>options; // , <>latency = 0.2, <notified=true;
	var <nodeAllocator;

	var <screenWidth, <screenHeight;
	
	// not used now
	var alive = false, booting = false, aliveThread, statusWatcher;

	var helloResp;

	*initClass {
		Class.initClassTree( NetAddr );
		Class.initClassTree( SwingOptions );
		Class.initClassTree( OSCresponder );
		Class.initClassTree( AppClock );
		Class.initClassTree( JFont );	// because we read JFont.default in initTree !
		named		= IdentityDictionary.new;
		set			= Set.new;
		clock		= AppClock;
		default		= local = SwingOSC.new( \localhost, NetAddr( "127.0.0.1", 57111 ));
		program 		= "java -jar SwingOSC.jar"; // -Dapple.awt.brushMetalLook=true if you like
//		CmdPeriod.add( this );
	}

	/*
	*	@param	name			a unique name for the server
	 *	@param	addr			a NetAddr object specifying the SwingOSC
	 *						server address
	 *	@param	options		currently nil
	 *	@param	clientID		integer between 0 and 31
	 */
	*new { arg name, addr, options, clientID = 0;
		^super.new.init( name, addr, options, clientID );
	}
	
	init { arg argName, argAddr, argOptions, argClientID;
		name			= argName;
		addr			= argAddr;
		clientID		= argClientID;
		options 		= argOptions ? SwingOptions.new;
		if( addr.isNil, { addr = NetAddr( "127.0.0.1", 57111 )});
		isLocal		= addr.addr == 2130706433;
//		serverRunning	= false;
		named.put( name, this );
		set.add( this );
		this.newAllocators;

		screenWidth	= 640;	// will be updated
		screenHeight	= 480;
		
		// note: this can fail when sclang doesn't get port 57120
		// which unfortunately happens from time to time. it would
		// be better to use rendezvous
		helloResp		= OSCpathResponder( nil, [ '/swing', \hello ], { arg time, resp, msg;
			var pingAddr, protocol;
			pingAddr = NetAddr( msg[ 2 ].asString, msg[ 3 ].asInteger );
			protocol	= msg[ 4 ];
//("pingAddr : "++pingAddr++"; req.addr "++addr).postln;
			if( pingAddr == addr and: { protocol == options.protocol }, {
//				this.initTree;
				this.serverRunning_( false );
				this.serverRunning_( true );	// invokes initTree
			});
		}).add;
		
		this.initTree({ serverRunning = true; this.changed( \serverRunning )});
	}
	
	connect {
		if( options.protocol === \tcp, {
			addr.connect;
		});
	}
	
	disconnect {
		if( options.protocol === \tcp, {
			addr.disconnect;
		});
	}
	
	initTree { arg onComplete, onFailure;
		var result;
		this.newAllocators;
		try {
			this.connect;
			{
				this.listSendMsg([ '/local', \font ] ++ JFont.default.asSwingArg );
				this.prRetrieveScreenBounds;
				this.dumpOSC( dumpMode, dumpModeR );
//						this.sendMsg( '/query', \status, '[', '/field', 'de.sciss.swingosc.SwingOSC', \VERSION, ']' );
				result = this.sendMsgSync([ '/query', \version, '[', '/field', 'de.sciss.swingosc.SwingOSC', \VERSION, ']' ],
					[ '/info', \version ]);
				if( result.notNil, {
					serverVersion = result[2];
					if( serverVersion != version, {
						("SwingOSC version mismatch: client is v" ++ version ++ ", server is v" ++ serverVersion ++ "!" ).warn;
					});
					onComplete.value;
				}, onFailure );
			}.fork( clock );
		}
		{ arg error;	// throws when TCP server not available
		
		}
	}
	
	addClasses { arg ... urls;
		this.sendMsg( '/classes', \add, *urls );
	}
	
	removeClasses { arg ... urls;
		this.sendMsg( '/classes', \remove, *urls );
	}
	
	updateClasses { arg ... urls;
		this.sendMsg( '/classes', \update, *urls );
	}
	
	sync { arg condition, bundles, latency, timeout = 4.0;
		var resp, cancel, id, queryMsg, result = true;

		if( condition.isNil, { condition = Condition.new };);

		id				= UniqueID.next;
		condition.test	= false;
		queryMsg			= [ '/query', id, 0 ];

		resp				= OSCpathResponder( addr, [ '/info', id ], { arg time, resp, msg;
			if( cancel.notNil, { cancel.stop; });
			resp.remove;
			condition.test = true;
			condition.signal;
		});
		resp.add;
		
		if( timeout > 0.0, {
			cancel = {
				timeout.wait;
				resp.remove;
				result			= false;
				condition.test	= true;
				condition.signal;
			
			}.fork( clock );
		});

		if( bundles.isNil, {
			// in SwingOSC, all messages are processed stricly after another, so a simple query
			// can be used as sync!
			addr.sendBundle( latency, queryMsg );
		}, {
			addr.sendBundle( latency, *(bundles ++ [ queryMsg ]));
		});

		condition.wait;
		^result;
	}
	
	retrieveScreenBounds {
		{
			this.prRetrieveScreenBounds;
		}.fork( clock );
	}
	
	// needs to be called inside a routine!
	prRetrieveScreenBounds {
		var reply = this.sendMsgSync(
			[ '/get', '[', '/local', \toolkit, '[', '/method', 'java.awt.Toolkit', \getDefaultToolkit, ']', ']',
					 'screenSize.width', 'screenSize.height' ], [ '/set', \toolkit ]
		);
		if( reply.notNil, {
			reply.copyToEnd( 2 ).pairsDo({ arg key, value;
				switch( key.asString,
					"screenSize.width", { screenWidth = value.asInt; },
					"screenSize.height", { screenHeight = value.asInt; }
				);
			});
		});			
	}

	*retrieveScreenBounds {
		this.default.retrieveScreenBounds;
	}
		
	newAllocators {
		nodeAllocator	= NodeIDAllocator( clientID );
	}

	nextNodeID {
		^nodeAllocator.alloc;
	}
	
	dispose {
		if( helloResp.notNil, {
			helloResp.remove;
			helloResp = nil;
		});
		addr = nil;

//		// slowOSC
//		dispatcher.stop;
//		dispatcher = nil;
	}
	
	sendMsg { arg ... msg;
//		if( slowOSC, {
//			dispPending.add( msg );
//			dispCond.test = true;
//			dispCond.signal;
//		}, {
			addr.sendMsg( *msg );
//		});
	}

	sendBundle { arg time ... msgs;
//		if( slowOSC, {
//			dispPending.addAll( msgs );	// no timetags for now
//			dispCond.test = true;
//			dispCond.signal;
//		}, {
			addr.sendBundle( time, *msgs )
//		});
	}
	
	/**
	 *	There is a bug in unixCmd when running on MacOS X 10.3.9
	 *	which blocks successive unixCmd calls. This seems to
	 *	fix it (ONLY ONCE THOUGH). so call this method once after
	 *	you started launching a second server
	 */
	unblockPipe {
		this.sendMsg( '/methodr', '[', '/field', 'java.lang.System', \out, ']', \println );
	}
	
	// needs to be called inside a routine ;(
	listSendMsgAndWait { arg msg, match;
		var cmdName, resp, condition, result;

		condition	= Condition.new;
		result	= nil;
		// XXX could better use an OSCpathResponder here
		resp = OSCresponderNode( addr, match[ 0 ], { arg time, resp, msg;
			if( match.every({ arg item, i; msg[ i ].asSymbol == item.asSymbol }), {
				resp.remove;
				condition.test = true;
				condition.signal;
				result	= msg;
			});
		}).add;
		condition.test = false;
		this.listSendMsg( msg );
		condition.wait;
		^result;
	}
	
	listSendMsg { arg msg;
//		if( slowOSC, {
//			dispPending.add( msg );
//			dispCond.test = true;
//			dispCond.signal;
//		}, {
			addr.sendMsg( *msg );
//		});
	}

 	listSendBundle { arg time, msgs;
//		if( slowOSC, {
//			dispPending.addAll( msgs );	// no timetags for now
//			dispCond.test = true;
//			dispCond.signal;
//		}, {
			addr.sendBundle( time, *msgs )
//		});
	}

	/**
	 *	@warning	asynchronous; needs to be called inside a Routine
	 */
	sendMsgSync { arg msg, successMsg, failedMsg, timeout = 4.0, condition;
		var respDone, respFailed, cancel, result;

		if( condition.isNil ) { condition = Condition.new; };

		successMsg = successMsg.asArray;
		respDone	= OSCresponderNode( addr, successMsg[ 0 ], { arg time, resp, msg;
			var ok = true;
		
			// string reply message args are always returned as symbols
			successMsg.do({ arg c, i;
				if( msg[ i ].isKindOf( Symbol ), {
					if( msg[ i ] !== c.asSymbol, { ok = false; });
				}, {
					if( msg[ i ] != c, { ok = false; });
				});
			});

			if( ok, {
				if( cancel.notNil, { cancel.stop; });
				resp.remove;
				result			= msg;
				condition.test	= true;
				condition.signal;
			});
		});
		respDone.add;
		if( failedMsg.notNil, {
			failedMsg = failedMsg.asArray;
			respFailed = OSCresponderNode( addr, failedMsg[ 0 ], { arg time, resp, msg;
				var ok = true;
			
				failedMsg.do({ arg c, i;
					if( msg[ i ] != c, { ok = false; });
				});
				
				if( ok, {
					if( cancel.notNil, { cancel.stop; });
					resp.remove;
					result			= msg;
					condition.test	= true;
					condition.signal;
				});
			});
			respFailed.add;
		});
		condition.test = false;
		if( timeout > 0.0, {
			cancel = {
				timeout.wait;
				respDone.remove;
				if( respFailed.notNil, { respFailed.remove; });
				result			= nil;
				condition.test	= true;
				condition.signal;
			
			}.fork( clock );
		});
		condition.test = false;
		this.listSendMsg( msg );
		condition.wait;
		^result;
	}

	serverRunning_ { arg val;
		if( val != serverRunning, {
			serverRunning = val;
			if( serverRunning, {
				this.initTree({
					this.changed( \serverRunning );
				}, {
					"SwingOSC.initTree : timeout".error;
				});			
			}, {
				{ this.changed( \serverRunning ); }.defer;
			});
		});
	}
	
	wait { arg responseName;
		var resp, routine;

		routine	= thisThread;
		resp		= OSCresponderNode( addr, responseName, { 
			resp.remove; routine.resume( true ); 
		});
		resp.add;
	}
	
	waitForBoot { arg onComplete, timeout = 20.0;
		if( serverBooting, { ^this.doWhenBooted( onComplete, timeout )});
		if( serverRunning.not, { 
			this.boot;
			this.doWhenBooted( onComplete, timeout );
		}, onComplete );
	}

	doWhenBooted { arg onComplete, timeout = 20.0;
		var cancel, upd;
		
		if( serverRunning.not, {
			upd		= Updater( this, {
				if( serverRunning, {
					cancel.stop;
					upd.remove;
					onComplete.value;
				});
			});
			cancel	= {
				timeout.wait;
				upd.remove;
				"SwingOSC server failed to start".error;
				serverBooting = false;
			}.fork( clock );
		}, onComplete );
	}

	bootSync { arg condition;
		condition.test = false;
		this.waitForBoot({
			// Setting func to true indicates that our condition has become true and we can go when signaled.
			condition.test = true;
			condition.signal
		});
		condition.wait;
	}

	startAliveThread { arg delay = 4.0, period = 0.7;
		^aliveThread ?? {
			this.addStatusWatcher;
			aliveThread = {
				// this thread polls the server to see if it is alive
				delay.wait;
				loop {
					if( serverBooting and: { (options.protocol === \tcp) and: { addr.isConnected.not; }; }, {
						try { this.connect; };
					}, {
						this.status;
					});
					period.wait;
					this.serverRunning = alive;
					alive = false;
				};
			}.fork( clock );
			aliveThread;
		};
	}
	
	stopAliveThread {
		if( aliveThread.notNil, { 
			aliveThread.stop; 
			aliveThread = nil;
		});
		if( statusWatcher.notNil, { 
			statusWatcher.remove;
			statusWatcher = nil;
		});
	}
	
	*resumeThreads {
		"SwingOSC.resumeThreads...".postln;
		set.do({ arg server;
			server.stopAliveThread;
			server.startAliveThread( 0.7 );
		});
	}
	
	boot { arg startAliveThread = true;
		var resp;
		
		{
			this.status;
			0.5.wait;
			block { arg break;
				if( serverRunning, { "SwingOSC server already running".inform; break.value; });
				if( serverBooting, { "SwingOSC server already booting".inform; break.value; });
				
				serverBooting = true;
				if( startAliveThread, { this.startAliveThread });
	//			this.newAllocators;		// will be done in initTree !
	//			this.resetBufferAutoInfo;	// not applicable to SwingOSC
				this.doWhenBooted({
	// there is no notification system at the moment
	//				if( notified, { 
	//					this.notify;
	//					"notification is on".inform;
	//				}, { 
	//					"notification is off".inform; 
	//				});
					serverBooting = false;
	//				serverRunning = true;
	//				this.initTree;
				});
				if( isLocal.not, { 
					"You will have to manually boot remote server.".inform;
				},{
					this.bootServerApp;
				});
			};
		}.fork( clock );
	}
	
	bootServerApp {
		var cmd;
		// note : the -h option is only necessary when booting the server
		// independant of sclang (as a cheap "bonjour" means). when booting
		// the server using the boot method, the aliveThread will take
		// care of recognizing when the server is responding and will
		// invoke the doWhenBooted commands, hence forcing the
		// necessary call of initTree!
//		cmd = program + "-u" + addr.port + "-i" + "-L"; // + "-h 127.0.0.1:57120";
		
		cmd = program ++ options.asOptionsString( addr.port );
		unixCmd( cmd );
		("booting " ++ cmd).inform;
	}
	
	reboot { arg func; // func is evaluated when server is off
		if( isLocal.not, { "C an't reboot a remote server".inform; ^this });
		if( serverRunning, {
			Routine.run {
				this.quit;
				this.wait( \done );
				0.1.wait;
				func.value;
				this.boot;
			}
		}, {
			func.value;
			this.boot;
		});
	}
	
	status {
		this.sendMsg( '/query', \status, 0 );
	}
	
	addStatusWatcher {
		statusWatcher = 
// OSCpathResponder is broken!
//			OSCpathResponder( addr, [ "/info", "status" ], { arg time, resp, msg;
//				var cmd, one;
//				alive = true;
//				this.serverRunning_( true );
//			//	"SwingOSC is alive!".postln;
			OSCpathResponder( addr, [ '/info', \status ], { arg time, resp, msg;
				alive = true;
				this.serverRunning_( true );
			}).add;	
	}
	
// n.y.a.
//	notify { arg flag = true;
//		notified = flag;
//		this.sendMsg( "/notify", flag.binaryValue );
//	}
	
	dumpOSC { arg code = 1, reply;
		/*
			0 - turn dumping OFF.
			1 - print the parsed contents of the message.
			2 - print the contents in hexadecimal.
			3 - print both the parsed and hexadecimal representations of the contents.
		*/
		dumpMode	= code;
		if( reply.isNil, {
			this.sendMsg( '/dumpOSC', code );
		}, {
			dumpModeR	= reply;
			this.sendMsg( '/dumpOSC', code, reply );
		});
	}
	
	debugDumpLocals {
		this.sendMsg( '/methodr', '[', '/methodr', '[', '/method', "de.sciss.swingosc.SwingOSC", \getInstance, ']', \getCurrentClient, ']', \debugDumpLocals );
	}

	quit {
		this.sendMsg( '/quit' );
		this.disconnect;	// try to prevent SC crash, this might be too late though ...
		"/quit sent\n".inform;
		alive			= false;
//		dumpMode 			= 0;
		serverBooting 	= false;
		this.serverRunning	= false;
//		RootNode(this).freeAll;
//		this.newAllocators;
	}

	*quitAll {
		set.do({ arg server; if( server.isLocal, { server.quit }); });
	}
	
// n.y.a.
//	freeAll {
//		this.sendMsg( '/g_freeAll', 0 );	// XXX
////		this.sendMsg( "/clearSched" );
//		this.initTree;
//	}
//	
//	*freeAll {
//		set.do({ arg server;
//			if( server.isLocal, { // debatable ?
//				server.freeAll;
//			});
//		});
//	}
//	
//	// bundling support
//	openBundle { arg bundle;  // pass in a bundle that you want to continue adding to, or nil for a new bundle.
//		addr = BundleNetAddr.copyFrom( addr, bundle );
//	}
//	
//	closeBundle { arg time; // set time to false if you don't want to send.
//		var bundle;
//		bundle	= addr.bundle;
//		addr		= addr.saveAddr;
//		if( time != false, { this.listSendBundle( time, bundle ); });
//		^bundle;
//	}
//	
//	makeBundle { arg time, func, bundle;
//		this.openBundle( bundle );
//		try {
//			func.value( this );
//			bundle = this.closeBundle( time );
//		}{ |error|
//			addr = addr.saveAddr; // on error restore the normal NetAddr
//			error.throw;
//		}
//		^bundle;
//	}
}