/*
 *	JSCPlugView
 *	(SwingOSC classes for SuperCollider)
 *
 *	Copyright (c) 2005-2008 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *	Changelog:
 */

/*
 *	Simple classes for integrating any subclass of
 *	JComponent (using JSCPlugView) or JPanel (using JSCPlugContainerView)
 *	with other JSCView classes.
 *
 *	@version		0.57, 12-Jan-08
 *	@author		Hanns Holger Rutz
 */
JSCPlugView : JSCView {
	// ----------------- constructor -----------------

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

	// ----------------- private instance methods -----------------

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
	// ----------------- constructor -----------------

	*new { arg parent, bounds, javaObject;
		var basic;
		
		basic = super.prBasicNew;
//		basic.prSetJavaClass( javaClass );
		^basic.init( parent, bounds, javaObject.id );
	}
	
	// ----------------- private instance methods -----------------

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