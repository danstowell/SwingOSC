/*
 *	Helper class extensions for SwingOSC communication
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

/**
 *	@author	Hanns Holger Rutz
 *	@version	0.57, 12-Jan-07
 */
//+ Object {
//	asSwingArg {
//		^this;
//	}
//}

+ SimpleNumber {
	asSwingArg {
		^this;
	}
}

+ ArrayedCollection {
	asSwingArg {
		^([ '[', '/array' ] ++ this.performUnaryOp( \asSwingArg ).flatten ++ ']');
	}
}

+ List {
	asSwingArg {
		^([ '[', '/method', "java.util.Arrays", \asList ] ++ this.asArray.asSwingArg ++ [ ']' ]);
	}
}

+ Nil {
	asSwingArg {
		^([ '[', '/ref', \null, ']' ]);
	}
}

+ String {
	// String is a subclass of ArrayedCollection!!
	asSwingArg {
		case { this.size !== 1 }
		{
			^[ this ];
		}
		{ this == "[" }	// must be escaped
		{
			^([ '[', '/ref', "brko", ']' ]);
		}
		{ this == "]" }	// must be escaped
		{
			^([ '[', '/ref', "brkc", ']' ]);
		}
		{
			^[ this ];
		};
	}
}

+ Symbol {
	asSwingArg {
		^this.asString.asSwingArg;
	}
}

+ Color {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Color', this.red.asFloat, this.green.asFloat, this.blue.asFloat, this.alpha.asFloat, ']' ]);
	}
}

+ JFont {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Font', this.name, this.style, this.size, ']' ]);
	}
}

+ Point {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Point', this.x, this.y, ']' ]);
	}
}

+ Rect {
	asSwingArg {
		^([ '[', '/new', 'java.awt.Rectangle', this.left, this.top, this.width, this.height, ']' ]);
	}
}

// Note: Gradient Paining doesn't work
// , at least with Aqua lnf the panels are not painted properly
+ Gradient {
	asSwingArg {
		^([ '[', '/new', 'java.awt.GradientPaint', 0.0, 0.0 ] ++ color1.asSwingArg ++ [
			if( direction == \h, 1.0, 0.0 ), if( direction == \h, 0.0, 1.0 )] ++ color2.asSwingArg ++ [ ']' ]);
	}
}

+ HiliteGradient {
	asSwingArg {
		^([ '[', '/new', 'java.awt.GradientPaint', 0.0, 0.0 ] ++ color1.asSwingArg ++ [
			if( direction == \h, frac, 0.0 ), if( direction == \h, 0.0, frac )] ++ color2.asSwingArg ++ [ ']' ]);
	}
}

+ Server {
	/**
	 *	There is a bug in unixCmd when running on MacOS X 10.3.9
	 *	which blocks successive unixCmd calls. This seems to
	 *	fix it (ONLY ONCE THOUGH). so call this method once after
	 *	you launching a second server
	 */
	unblockPipe {
		this.sendMsg( '/n_trace', 0 );
	}
}

// don't blame me for this hackery
+ SCViewHolder {
//	prIsInsideContainer {Ê^false }
	prSetScBounds {}
	prInvalidateChildBounds {}
	protDraw {}
	id { ^nil }	// this is detected by JSCContainerView!
}