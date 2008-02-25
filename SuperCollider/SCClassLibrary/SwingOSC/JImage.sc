/**
 *	@version	0.60, 25-Feb-08
 *	@author	Hanns Holger Rutz
 */
// THIS IS EXPERIMENTAL AND SUBJECT TO CHANGES!!!
JImage : JavaObject {
	*load { arg path, server;
		var img;
		img		= JavaObject.newFrom( JavaObject.basicNew( \toolkit, server ?? { SwingOSC.default }), \createImage, path );
		^img;
	}
	
	destroy {
		server.sendBundle( nil, [ '/method', id, \flush ], [ '/free', id ]);
		allObjects.remove( this );
	}
	
	doesNotUnderstand { arg selector ... args;
		DoesNotUnderstandError( this, selector, args ).throw;
	}
}