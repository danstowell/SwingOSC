/*
 *	JSCPlugView
 *	(SwingOSC classes for SuperCollider)
 *
 *	Simple classes for integrating any subclass of
 *	JComponent (using JSCPlugView) or JPanel (using JSCPlugContainerView)
 *	with other JSCView classes.
 *
 *	@version		0.55, 03-Aug-07
 *	@author		Hanns Holger Rutz
 */
JSCPlugView : JSCView {
	*new { arg parent, bounds, javaObject;
		var basic;
		
		basic = super.prBasicNew;
		^basic.init( parent, bounds, javaObject.id );
	}
	
//	prSCViewNew {
//		^super.prSCViewNew([
//			[ '/local', this.id, '[', '/new', javaClass, ']' ]
//		]);
//	}

	// from JavaObject.sc

	doesNotUnderstand { arg selector ... args;
		server.sendMsg( *this.prMethodCall( selector, args ));
	}

	prMethodCall { arg selector, args;
		var listMsg = List.new;
		listMsg.add( '/method' );
		listMsg.add( this.id );
		listMsg.add( selector );
		args.do({ arg x;
			if( x.respondsTo( \id ), {
				listMsg.addAll([ '[', '/ref', x.id, ']' ]);
			}, {
				listMsg.addAll( x.asSwingArg );
			});
		});
		^listMsg;
	}
}

JSCPlugContainerView : JSCContainerView {
	*new { arg parent, bounds, javaObject;
		var basic;
		
		basic = super.prBasicNew;
//		basic.prSetJavaClass( javaClass );
		^basic.init( parent, bounds, javaObject.id );
	}
	
//	prSetJavaClass { arg class;
//		javaClass = class;
//	}

//	prSCViewNew {
//		^super.prSCViewNew([
//			[ '/local', this.id, '[', '/new', javaClass, ']' ]
//		]);
//	}

	// from JavaObject.sc

	doesNotUnderstand { arg selector ... args;
		server.sendMsg( *this.prMethodCall( selector, args ));
	}

	prMethodCall { arg selector, args;
		var listMsg = List.new;
		listMsg.add( '/method' );
		listMsg.add( this.id );
		listMsg.add( selector );
		args.do({ arg x;
			if( x.respondsTo( \id ), {
				listMsg.addAll([ '[', '/ref', x.id, ']' ]);
			}, {
				listMsg.addAll( x.asSwingArg );
			});
		});
		^listMsg;
	}
}