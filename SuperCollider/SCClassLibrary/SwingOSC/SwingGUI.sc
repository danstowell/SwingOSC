/*
 *	SwingGUI
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
 *	This is the Java/Swing(OSC) framework GUI implementation.
 *	It can be accessed from the GUI
 *	class using GUI.swing, GUI.fromID( \swing ) or GUI.get( \swing ).
 *
 *	@author		Hanns Holger Rutz
 *	@version		0.57, 12-Jan-08
 */
SwingGUI {
	classvar extraClasses;

	*initClass {
		Class.initClassTree( Event );
		extraClasses = Event.new;
		Class.initClassTree( GUI );
		if( GUI.respondsTo( \add ), { GUI.add( this )});
	}

	// ----------------- public class methods -----------------

	*id { ^\swing }
	
	*put { arg key, object;
		extraClasses.put( key, object );
	}

	///////////////// Common -> GUI -> Base /////////////////

	*view { ^JSCView }
	*window { ^JSCWindow }
	*compositeView { ^JSCCompositeView }
	*hLayoutView { ^JSCHLayoutView }
	*vLayoutView { ^JSCVLayoutView }
	*slider { ^JSCSlider }
	*rangeSlider { ^JSCRangeSlider }
	*slider2D { ^JSC2DSlider }
//	*tabletSlider2D { ^JSC2DTabletSlider }
	*button { ^JSCButton }
	*popUpMenu { ^JSCPopUpMenu }
	*staticText { ^JSCStaticText }
	*listView { ^JSCListView }
	*dragSource { ^JSCDragSource }
	*dragSink { ^JSCDragSink }
	*dragBoth { ^JSCDragBoth }
	*numberBox { ^JSCNumberBox }
	*textField { ^JSCTextField }
	*userView { ^JSCUserView }
	*multiSliderView { ^JSCMultiSliderView }
	*envelopeView { ^JSCEnvelopeView }
	*tabletView { ^JSCTabletView }
	*soundFileView { ^JSCSoundFileView }
	*movieView { ^JSCMovieView }
	*textView { ^JSCTextView }
//	*quartzComposerView { ^JSCQuartzComposerView }
	*scopeView { ^JSCScope }
	*freqScope { ^JFreqScope }
	*freqScopeView { ^JSCFreqScope }
	*ezSlider { ^JEZSlider }
	*ezNumber { ^JEZNumber }
	*stethoscope { ^JStethoscope }
	
	*font { ^JFont }
	*pen { ^JPen }
			
	///////////////// Common -> Audio /////////////////

	*mouseX { ^JMouseX }
	*mouseY { ^JMouseY }
	*mouseButton { ^JMouseButton }
	*keyState { ^JKeyState }
			
	///////////////// Common -> OSX /////////////////

	*dialog { ^SwingDialog }
	*speech { ^JSpeech }

	///////////////// extras /////////////////
			
	*checkBox { ^JSCCheckBox }
	*tabbedPane { ^JSCTabbedPane }
	*scrollBar { ^JSCScrollBar }
	*peakMeterView { ^JSCPeakMeterView }

	///////////////// crucial /////////////////
//	*startRow { ^JStartRow }

	// ----------------- private class methods -----------------

	*doesNotUnderstand { arg selector ... args;
		^extraClasses.perform( selector, *args );
	}
}