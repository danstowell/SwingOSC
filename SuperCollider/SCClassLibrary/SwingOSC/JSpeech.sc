/**
 *	Replacement for (Cocoa) Speech
 *
 *	@version	0.53, 29-Apr-07
 *	@author	Hanns Holger Rutz
 */
JSpeech {
	classvar <>wordAction, <>doneAction;
	classvar <>initialized = false;
	
	classvar <>server;
	
	classvar svolume = 1.0, srate = 120, spchrange = 10, spch = 80;

	*setSpeechVoice { arg chan, voice;
		"JSpeech.setSpeechVoice : not yet implemented".error;
//		_SetSpeechVoice
	}

	*setSpeechRate { arg chan, rate;
//		rate = (rate * 2).div( 3 );
//		rate = (rate.pow( 0.785 ) + 48.9).round( 0.1 );
//		rate = (rate.pow( 0.897 ) + 10.5).round( 0.1 );
		srate = ((rate - 20) / 1.75 + 25.72).round( 0.1 ); // hmmm, seems to be similar to cocoa speed this way
		if( initialized, { server.sendMsg( '/set', \speech, \rate, srate )});
	}

	*setSpeechPitch { arg chan, pitch;
		spch = pitch.midicps;
		if( initialized, { server.sendMsg( '/set', \speech, \pitch, spch )});
	}

	*setSpeechPitchMod { arg chan, pitchMod;
		spchrange = pitchMod / 10; // hmmm...
		if( initialized, { server.sendMsg( '/set', \speech, \pitchRange, spchrange )});
	}

	*setSpeechVolume { arg chan, volume;
		svolume = volume.ampdb.max(-96)/96+1; // hmmm, seems to be similar to cocoa linear volume this way
		if( initialized, { server.sendMsg( '/set', \speech, \volume, svolume )});
	}

	*pause { arg chan, paused = 0;
		if( initialized, {
			switch( paused.asInteger,
			0, {
				server.sendMsg( '/methodr', '[', \method, \speech, \getAudioPlayer, ']', \resume );
			},
			1, {
				server.sendMsg( '/methodr', '[', \method, \speech, \getAudioPlayer, ']', \pause );
			}, {
				("JSpeed.pause : illegal argument (paused = " ++ paused ++ ")").error;
			});
		});
	}

	*speak { arg text, voice;
		var result = initialized;
		if( result.not, { result = this.init });		
		if( result, {
			server.sendMsg( '/method', \speech, \speak, text );
		});
	}

	// private
	*init { arg num = 1;
		if( server.isNil, { server = SwingOSC.default });
		if( server.serverRunning.not, {
			"JSpeech.init : SwingOSC server is not running".error;
			^false;
		});
		if( initialized.not, {
			initialized = true;
			UI.registerForShutdown({ if( server.notNil and: { server.serverRunning }, {
				server.sendBundle( nil, [ '/method', \speech, \deallocate ], [ '/free', \speech ]);
			})});
		});
		server.sendBundle( nil,
			[ '/local', \speech, '[', '/methodr', '[', '/method', 'com.sun.speech.freetts.VoiceManager', \getInstance, ']',
				\getVoice, "kevin16", ']' ],
			[ '/method', \speech, \allocate ],
			[ '/set', \speech, \volume, svolume, \rate, srate, \pitchRange, spchrange, \pitch, spch ]
		);
		^true;
	}

	*doWordAction { arg chan;
		wordAction.value( chan );
	}

	*doSpeechDoneAction { arg chan;
		doneAction.value( chan );
	}
}