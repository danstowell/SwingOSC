/*
 *	JSCViews collection 5
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
 */

/**
 *	@author	Hanns Holger Rutz
 *	@version	0.61, 16-Oct-08
 */
JSCPanel : JSCContainerView {
	// ----------------- quasi-interface methods : crucial-lib support -----------------

//	asFlowView { ... }

	// ----------------- private instance methods -----------------

	prChildOrder { arg child; ^child.protCmpLayout ?? "" }

	prInitView {
		jinsets = Insets( 3, 3, 3, 3 );  // so focus borders of children are not clipped
		^this.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.Panel", '[', '/new', "net.miginfocom.swing.MigLayout", ']', ']' ]
		]);
	}
	
	layout_ { arg constraints;
		server.sendMsg( '/set', this.id, \layoutConstraints, constraints );
	}

//	add { arg child;
//		var bndl, vpID;
//		
//		children = children.add( child );
////		if( decorator.notNil, { decorator.place( child )});
//
//		if( child.id.notNil, {
//			vpID = this.prViewPortID;
//			bndl = Array( 4 );
//			bndl.add([ '/method', vpID, \add, '[', '/ref', child.prContainerID, ']', child.protCmpLayout ? "" ]);
//			if( this.prAllVisible, {
//				if( this.id != vpID, {
//					bndl.add([ '/method', vpID, \validate ]);
//				});
//				bndl.add([ '/method', this.id, \revalidate ]);
//				bndl.add([ '/method', child.id, \repaint ]);
//				pendingValidation = false;
//			}, {
//				pendingValidation = true;
//			});
//			server.listSendBundle( nil, bndl );
//		});
//	}
}
