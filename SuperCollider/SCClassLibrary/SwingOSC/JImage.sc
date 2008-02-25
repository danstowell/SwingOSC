/**
 *	@version	0.60, 25-Feb-08
 *	@author	Hanns Holger Rutz
 */
// THIS IS EXPERIMENTAL AND SUBJECT TO CHANGES!!!
JImage : JavaObject {
	*load { arg url, server;
		var jURL, img;
		server	= server ?? { SwingOSC.default };
		jURL		= JavaObject( "java.net.URL", server, url );
		img		= this.newFrom( JavaObject.basicNew( \toolkit, server ), \createImage, jURL );
		jURL.destroy;
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