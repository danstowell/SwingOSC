/**
 *	(C)opyright 2006-2008 Hanns Holger Rutz. All rights reserved.
 *	Distributed under the GNU General Public License (GPL).
 *
 *	you can use this extension so that
 *	cocoa gui behaves exactly as swingOSC gui,
 *	at least has some more graceful fallbacks...
 *
 *	@version	0.56, 31-Oct-07
 *	@author	Hanns Holger Rutz
 */
//+ Pen {
//	*font_ { arg font;
//		CocoaCompat.penFont = font;
//	}
//
//	*string { arg str;
//		^str.drawAtPoint( Point( 0, 0 ), CocoaCompat.penFont ?? Font.default, CocoaCompat.penFillColor ?? Color.black );
//	}
//	
//	*stringAtPoint { arg str, point;
//		^str.drawAtPoint( point, CocoaCompat.penFont ?? Font.default, CocoaCompat.penFillColor ?? Color.black );
//	}
//	
//	*stringInRect { arg str, rect;
//		^str.drawInRect( rect, CocoaCompat.penFont ?? Font.default, CocoaCompat.penFillColor ?? Color.black );
//	}
//	
//	*stringCenteredIn { arg str, inRect;
//		^str.drawCenteredIn( inRect, CocoaCompat.penFont ?? Font.default, CocoaCompat.penFillColor ?? Color.black );
//	}
//	
//	*stringLeftJustIn { arg str, inRect;
//		^str.drawLeftJustIn( inRect, CocoaCompat.penFont ?? Font.default, CocoaCompat.penFillColor ?? Color.black );
//	}
//	
//	*stringRightJustIn { arg str, inRect;
//		^str.drawRightJustIn( inRect, CocoaCompat.penFont ?? Font.default, CocoaCompat.penFillColor ?? Color.black );
//	}
//
//	*strokeColor_ { arg color;
//		CocoaCompat.penStrokeColor	= color;
//		color.setStroke;
//	}
//
//	*fillColor_ { arg color;
//		CocoaCompat.penFillColor 	= color;
//		color.setFill;
//	}
//	
//	*color_ { arg color;
//		CocoaCompat.penFillColor	= color;
//		CocoaCompat.penStrokeColor	= color;
//		color.set;
//	}
//}

//+ SC2DSlider {
//	setXY { arg x, y;
//		this.x = x;
//		this.y = y;
//	}
//	
//	setXYActive { arg x, y;
//		this.setXY( x, y );
//		this.doAction;
//	}
//}

+ SCDragView {
	interpretDroppedStrings {Ê^true }
	
	interpretDroppedStrings_ { arg bool;
		"SCDragView.interpretDroppedStrings_ : not yet working".error;
	}
}

+ SCEnvelopeView {
	font { ^nil }
	font_ { arg argFont;
		"SCEnvelopeView.font_ : not yet working".error;
	}

	clipThumbs { ^false }
	clipThumbs_ { arg bool;
		"SCEnvelopeView.clipThumbs_ : not yet working".error;
	}
	
	strings {
		"SCEnvelopeView.strings : not yet working".error;
		^nil;
	}

	connections {
		"SCEnvelopeView.connections : not yet working".error;
		^nil;
	}

	selection {
		"SCEnvelopeView.selection : not yet working".error;
		^(nil ! this.value.first.size);
	}

	deselectIndex { arg index;
		"SCEnvelopeView.deselectIndex : not yet working".error;
	}

	curve_ { arg curve = \lin;
		"SCEnvelopeView.curve_ : not yet working".error;
	}

	lockBounds_ { arg val;
		"SCEnvelopeView.lockBounds_ : not yet working".error;
	}
	
	horizontalEditMode_ { arg val;
		"SCEnvelopeView.horizontalEditMode_ : not yet working".error;
	}
}

+ SCMovieView {
	skipFrames { arg numFrames;
		"SCMovieView.skipFrames : not yet working".error;
	}

	frame_ { arg frameIdx;
		"SCMovieView.frame_ : not yet working".error;
	}

	fixedAspectRatio_ { arg bool;
		"SCMovieView.fixedAspectRatio_ : not yet working".error;
	}
}

//+ SCMultiSliderView {
//	startIndex_ { arg val; this.setProperty( \startIndex, val )}
//}

+ SCNumberBox {
	minDecimals { ^0 }
	maxDecimals { ^8 }
	
	minDecimals_ { arg val;
		"SCNumberBox.minDecimals_ : not yet working".error;
	}
	
	maxDecimals_ { arg val;
		"SCNumberBox.maxDecimals_ : not yet working".error;
	}
}

+ SCPopUpMenu {
	allowsReselection { ^false }
	
	allowsReselection_ { arg bool;
		"SCPopUpMenu.allowsReselection_ : not yet working".error;
	}
}

//+ SCRangeSlider {
//	setSpan { arg lo, hi;
//		this.lo = lo;
//		this.hi = hi;
//	}
//	
//	setSpanActive { arg lo, hi;
//		this.setSpan( lo, hi );
//		this.doAction;
//	}
//}

+ SCSoundFileView {
	*cacheFolder { ^nil }
	*cacheFolder_ { arg path;
		"SCSoundFileView.cacheFolder_ : not yet working".error;
	}
	
	*cacheCapacity { ^50 }
	*cacheCapacity_ { arg megaBytes;
		"SCSoundFileView.cacheCapacity_ : not yet working".error;
	}
	
	*cacheActive { ^false }
	*cacheActive_ { arg bool;
		"SCSoundFileView.cacheActive_ : not yet working".error;
	}
}

+ SCTextView {
	caretColor { ^nil }
	caretColor_ { arg color;
		"SCTextView.caretColor_ : not yet working".error;
	}

	openURL { arg url;
		"SCTextView.openURL : not yet working".error;
	}
}

+ SCUserView {
	focusVisible { ^true }
	focusVisible_ { arg visible;
		"SCUserView.focusVisible_ : not yet working".error;
	}

	refreshOnFocus { ^true }
	refreshOnFocus_ { arg bool;
		"SCUserView.focusVisible_ : not yet working".error;
	}
}

+ SCWindow {
	unminimize {
		"SCWindow.unminimize : not yet working".error;
	}

	visible_ { arg boo;
		"SCWindow.visible_ : not yet working".error;
	}
}
