/**
 *	This is the Java/Swing(OSC) framework GUI implementation.
 *	It can be accessed from the GUI
 *	class using GUI.swing, GUI.fromID( \swing ) or GUI.get( \swing ).
 *
 *	@version	0.57, 18-Dec-07
 */
SwingGUI {
	classvar extraClasses;

	*initClass {
		Class.initClassTree( Event );
		extraClasses = Event.new;
		Class.initClassTree( GUI );
		if( GUI.respondsTo( \add ), { GUI.add( this )});
	}

	*id { ^\swing }
	
	*put { arg key, object;
		extraClasses.put( key, object );
	}

	*doesNotUnderstand { arg selector ... args;
		^extraClasses.perform( selector, *args );
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
}
