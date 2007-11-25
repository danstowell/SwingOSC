/*
 *	JSCMovieView
 *	(SwingOSC classes for SuperCollider)
 *
 *	Replacement for the (Cocoa) SCMovieView class by Jan Truetzschler.
 *
 *	@author		Hanns Holger Rutz
 *	@version		0.45, 04-Feb-07
 */
JSCMovieView : JSCView{
	var <rate, <loopMode, <muted, <path, <editable;
	var fixedAspectRatio = false;
	
	/*
	loop modes
	0 Playback runs forward and backward between both endpoints.
	1 Restarts playback at beginning when end is reached.
	2 Playback stops when end is reached.		
	*/
	
	start{
		this.setProperty(\start);		
	}
	
	stop{
		this.setProperty(\stop);		
	}
	
	path_{|moviePath|
		path = moviePath;
		this.setProperty(\setMovie, moviePath);	
	}
	
	muted_{|bool|
		muted = bool;
		this.setProperty(\setMuted, bool);		
	}
	
	playSelectionOnly_{|bool|
		this.setProperty(\setPlaysSelectionOnly, bool);
	}
	
	rate_{|ratein|
		rate = ratein;
		this.setProperty(\setRate, ratein);
	}
	
	loopMode_{|mode|
		loopMode = mode;
		this.setProperty(\setLoopMode, mode);
	}	
	
	gotoEnd{
		this.setProperty(\gotoEnd);
	}
	stepForward{
		this.setProperty(\stepForward);
	}
	
	stepBack{
		this.setProperty(\stepBack);	
	}
	
	gotoBeginning{
		this.setProperty(\gotoBeginning);
	}	
	
	currentTime_{|time|
		this.setProperty(\setCurrentTime, time);
	}	

	currentTime{
		"JSCMovieView-currentTime is not implemented".warn;
//		^this.getProperty(\getCurrentTime);
	}	
		
	editable_{|bool|
		editable = bool;
		this.setProperty(\setEditable, bool);		
	}
	
	showControllerAndAdjustSize{|show, adjust|
		this.setProperty(\showControllerAndAdjustSize, [show, adjust]);
	}
	
	resizeWithMagnification{|size|
		this.setProperty(\resizeWithMagnification, size)
	}
	
	fixedAspectRatio_ { arg bool;
		fixedAspectRatio = bool;
		this.setProperty( \fixedAspectRatio, bool );
	}
	
	copy{
		this.setProperty(\copy);
	}
	clear{
		this.setProperty(\clear);
	}
	cut{
		this.setProperty(\cut);
	}			
	paste{
		this.setProperty(\paste);
	}	

	// JJJ begin
	prSCViewNew {
//		properties.put( \bufnum, 0 );
//		properties.put( \x, 0.0 );
//		properties.put( \y, 0.0 );
//		properties.put( \xZoom, 1.0 );
//		properties.put( \yZoom, 1.0 );
//		properties.put( \style, 0 );
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.MovieView", ']' ]		]);
	}
	// JJJ end
	
	// JJJ begin
	prClose {
		^super.prClose([[ '/method', this.id, \dispose ]]);
	}
	// JJJ end
	
	// JJJ begin
	skipFrames { arg numFrames;
		server.sendMsg( '/method', this.id, \skip, numFrames );
	}
	
	frame_ { arg frameIdx;
		server.sendMsg( '/method', this.id, \setCurrentFrame, frameIdx );
	}
	// JJJ end

	// JJJ begin
	prSendProperty { arg key, value;
		key	= key.asSymbol;

		switch( key,
			\start, {
				server.sendMsg( '/method', this.id, \start );
				^this;
			},
			\stop, {
				server.sendMsg( '/method', this.id, \stop );
				^this;
			},
			\setCurrentTime, {
				server.sendMsg( '/method', this.id, \setCurrentTime, value );
				^this;
			},
			\setMovie, {
				key = \movie;
			},
			\setMuted, {
				key = \muted;
			},
			\setRate, {
				key = \rate;
			},
			\setLoopMode, {
				key 		= \loopMode;
				value	= value == 1;
			},
			\gotoEnd, {
				server.sendMsg( '/method', this.id, \goToEnd );
				^this;
			},
			\gotoBeginning, {
				server.sendMsg( '/method', this.id, \goToBeginning );
				^this;
			},
			\stepForward, {
				server.sendMsg( '/method', this.id, \stepForward );
				^this;
			},
			\stepBack, {
				server.sendMsg( '/method', this.id, \stepBack );
				^this;
			},
			\resizeWithMagnification, {
				server.sendMsg( '/method', this.id, \setToPreferredSize, value );
				^this;
			},
			\showControllerAndAdjustSize, {
				server.sendMsg( '/method', this.id, \setControlPanelVisible, value.first );
				// ???
				^this;
			}
		);
		^super.prSendProperty( key, value );
	}
	// JJJ end
}