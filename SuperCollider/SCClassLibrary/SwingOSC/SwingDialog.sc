/*
 *	SwingDialog
 *	(SwingOSC classes for SuperCollider)
 *
 *	A replacement for CocoaDialog.
 *
 *	Not yet working:
 *	- multiple input file selection
 *
 *	Different behaviour
 *
 *	Added features
 *
 *	@author		SuperCollider Developers
 *	@author		Hanns Holger Rutz
 *	@version		0.50, 09-Feb-07
 */
SwingDialog {
	classvar result, ok, cancel;

	*initClass {
		UI.registerForReset({ this.clear });
	}
	
	*getPaths { arg okFunc, cancelFunc, maxSize=20;
		if(result.notNil,{
			"A SwingDialog is already in progress.  do: [SwingDialog.clear]".warn;
			^nil
		});
		
		result = Array.new(maxSize);
		ok = okFunc;
		cancel = cancelFunc;
		this.prGetPathsDialog(result);
	}
	
	*prGetPathsDialog { arg argResult;
		this.prShowDialog( "Open", 0 );
	}

	*prShowDialog { arg title, mode;
		var server, swing, dlg, frame, file, dir, isOk, visible, wResp, wJResp, cResp, cJResp, fDone, err;

		server	= SwingOSC.default;
		frame	= JavaObject( 'java.awt.Frame', server );
		dlg 		= JavaObject( 'java.awt.FileDialog', server, frame, title, mode );
//		dlg.setModal( false );

		fDone	= { arg time, resp, msg;
			wResp.remove;
			wJResp.remove;
			wJResp.destroy;
			cResp.remove;
			cJResp.remove;
			cJResp.destroy;
			fork {
				swing 	= JavaObject.getClass( 'de.sciss.swingosc.SwingOSC', server );
				file		= JavaObject.newFrom( dlg, \getFile );
				isOk		= swing.notNull_( file );
				swing.destroy;
//					("isOk == "++isOk).inform;
				try {
					if( isOk.notNil and: { isOk != 0 }, {
						file.destroy;
						file	= dlg.getFile_;
						dir	= dlg.getDirectory_;
						if( mode === 0, {
							result.add( dir.asString ++ file.asString );
						}, {
							result = dir.asString ++ file.asString;
						});
						this.ok;
					}, {
						this.cancel;
					});
				}
				{ arg error;
					err = error;
				};
				dlg.destroy;
				frame.dispose;
				frame.destroy;
				this.clear;
				if( err.notNil, { err.throw; });
			};
		};

// XXX warning, OSCpathResponder still buggy, don't use [ '/window', dlg.id, \closed ]
// because it conflicts with the regular window responders created by JSCWindow !!!
// NOTE: on linux, window-closed is not send!! we have to check for component hidden instead!!
		wResp	= OSCpathResponder( server.addr, [ '/window', dlg.id ], { arg time, resp, msg;
			if( msg[2] == \closed, fDone );
		});
		cResp	= OSCpathResponder( server.addr, [ '/component', dlg.id ], { arg time, resp, msg;
			if( msg[2] == \hidden, fDone );
		});
		wResp.add;
		cResp.add;
		wJResp	= JavaObject( 'de.sciss.swingosc.WindowResponder', server, dlg.id );
		cJResp	= JavaObject( 'de.sciss.swingosc.ComponentResponder', server, dlg.id );
		dlg.setVisible( true );
	}
	
	*savePanel { arg okFunc,cancelFunc;
		if(result.notNil,{
			"A SwingDialog is already in progress.  do: [SwingDialog.clear]".warn;
			^nil
		});
		result = String.new(512);
		ok = okFunc;
		cancel = cancelFunc;
		this.prSavePanel(result);
	}
	*prSavePanel { arg argResult;
		this.prShowDialog( "Save", 1 );
	}
			
	*ok {
		var res;
		res = result;
		cancel = result = nil;
		ok.value(res);
		ok = nil;
	}
	*cancel {
		var res;
		res = result;
		ok = result = nil;
		cancel.value(res);
		cancel = nil;
	}
	*error {
		this.clear;
		"An error has occured during a SwingDialog".error;
	}
	*clear { // in case of errors, invalidate any previous dialogs
		ok = cancel = result = nil;
	}
}

//Swing {
//
//	*getPathsInDirectory { arg directoryPath,extension,maxItems=1000;
//		^this.prGetPathsInDirectory(directoryPath,extension,Array.new(maxItems));
//		//throws an index out of range if more than maxItems items are in the directory
//		
//		//extension matching not yet implemented
//	}
//	*prGetPathsInDirectory { arg dir,ext,arr;
//		_Swing_GetPathsInDirectory;
//		^this.primitiveFailed
//	}
//}
