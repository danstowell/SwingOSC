/**
 *	@version	0.61, 17-Apr-09
 *	@author	Hanns Holger Rutz
 */
// THIS IS EXPERIMENTAL AND SUBJECT TO CHANGES!!!
JSCImage : JavaObject {

// this polymorphous constructor sucks, we don't support it
//	*new { arg multiple, height = nil, server;
//		
//		if( multiple.isKindOf( Point ), {
//			Error( "JImage.new( <Point> ): not yet supported" ).throw;
//		});
//		
//		if( multiple.isKindOf( Number ), {
//			Error( "JImage.new( <Number> ): not yet supported" ).throw;
//		});
//		
//		^if( multiple.isKindOf( String ), {
//			if( multiple.beginsWith( "http://" ).not
//			    and: { multiple.beginsWith( "file://" ).not }
//			    and: { multiple.beginsWith( "ftp://" ).not  }, {
//								
//				this.open( multiple, server );
//			}, {
//				this.openURL( multiple, server );
//			});
//		});
//	}
	
	*openURL { arg url, server;
		var jURL, img;
		server	= server ?? { SwingOSC.default };
		jURL		= JavaObject( "java.net.URL", server, url );
		img		= this.newFrom( JavaObject.basicNew( \toolkit, server ), \createImage, jURL );
		jURL.destroy;
		^img;
	}

	*open { arg path, server;
		var jFile, img;
		server	= server ?? { SwingOSC.default };
		jFile	= JavaObject( "java.io.File", server, path );
		img		= this.newFrom( JavaObject.basicNew( \toolkit, server ), \createImage, jFile );
		jFile.destroy;
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