/*
 *	JSCViews2
 *	(SwingOSC classes for SuperCollider)
 *
 *	Replacements for the basic (Cocoa) views.
 *
 *	@author		SuperCollider Developers
 *	@author		Hanns Holger Rutz
 *	@version		0.45, 30-Jan-07
 */
 
// n.y. working
//JSCTabletView : JSCView {
//
////	var <>mouseDownAction,<>mouseUpAction;//	
//	mouseDown { arg x,y,pressure,tiltx,tilty,deviceID, buttonNumber,clickCount,absoluteZ,rotation;
//		mouseDownAction.value(this,x,y,pressure,tiltx,tilty,deviceID, buttonNumber,clickCount,absoluteZ,rotation);
//	}
//	mouseUp { arg x,y,pressure,tiltx,tilty,deviceID, buttonNumber,clickCount,absoluteZ,rotation;
//		mouseUpAction.value(this,x,y,pressure,tiltx,tilty,deviceID, buttonNumber,clickCount,absoluteZ,rotation);
//	}
//	doAction { arg x,y,pressure,tiltx,tilty,deviceID, buttonNumber,clickCount,absoluteZ,rotation;
//		action.value(this,x,y,pressure,tiltx,tilty,deviceID, buttonNumber,clickCount,absoluteZ,rotation);
//	}
//}



JEZSlider 
{
	var <>labelView, <>sliderView, <>numberView, <>controlSpec, <>action, <value;
	var <round = 0.001;
	
	*new { arg window, dimensions, label, controlSpec, action, initVal, 
			initAction=false, labelWidth=80, numberWidth = 80;
		^super.new.init(window, dimensions, label, controlSpec, action, initVal, 
			initAction, labelWidth, numberWidth);
	}
	init { arg window, dimensions, label, argControlSpec, argAction, initVal, 
			initAction, labelWidth, numberWidth;
		var	decorator = window.asView.tryPerform(\decorator),			gap = decorator.tryPerform(\gap);				gap.notNil.if({			(dimensions = dimensions.copy).x_(dimensions.x - (2*gap.x));		});		labelView = JSCStaticText(window, labelWidth @ dimensions.y);
		labelView.string = label;
		labelView.align = \right;
		
		controlSpec = argControlSpec.asSpec;
		initVal = initVal ? controlSpec.default;
		action = argAction;
		
		sliderView = JSCSlider(window, (dimensions.x - labelWidth - numberWidth) @ dimensions.y);
		sliderView.action = {
			value = controlSpec.map(sliderView.value);
			numberView.value = value.round(round);
			action.value(this);
		};
// JJJ
//		if (controlSpec.step != 0) {
//			sliderView.step = (controlSpec.step / (controlSpec.maxval - controlSpec.minval));
//		};

		numberView = JSCNumberBox(window, numberWidth @ dimensions.y);
		numberView.action = {
			numberView.value = value = controlSpec.constrain(numberView.value);
			sliderView.value = controlSpec.unmap(value);
			action.value(this);
		};
		// JJJ
		numberView.maxDecimals = 3;	// for default round of 0.001

// JJJ this fires a value change for
// the slider, therefore numverView.action must have
// been assigned before!!!
if (controlSpec.step != 0) {
	sliderView.step = (controlSpec.step / (controlSpec.maxval - controlSpec.minval));
};
		
		if (initAction) {
			this.value = initVal;
		}{
			value = initVal;
			sliderView.value = controlSpec.unmap(value);
			numberView.value = value.round(round);
		};
	}
	value_ { arg value; numberView.valueAction = value }
	set { arg label, spec, argAction, initVal, initAction=false;
		labelView.string = label;
		controlSpec = spec.asSpec;
		action = argAction;
		initVal = initVal ? controlSpec.default;
		if (initAction) {
			this.value = initVal;
		}{
			value = initVal;
			sliderView.value = controlSpec.unmap(value);
			numberView.value = value.round(round);
		};
	}
	
	// JJJ begin
	round_ { arg argRound;
		var str;
	
		round = argRound;
		
		if( round == 0, {
			numberView.maxDecimals = 8;
		}, {
			str = round.asString;
			numberView.maxDecimals = str.size - (str.indexOf( $. ) ?? 0) - 1;
		});
	}
	// JJJ end

	visible_ { |bool|		[labelView, sliderView, numberView].do(_.visible_(bool))	}

	remove { [labelView, sliderView, numberView].do(_.remove) }
}



JEZNumber
{
	var <>labelView, <>numberView, <>controlSpec, <>action, <value;
	var <round = 0.001;
	
	*new { arg window, dimensions, label, controlSpec, action, initVal, 
			initAction=false, labelWidth=80, numberWidth = 80;
		^super.new.init(window, dimensions, label, controlSpec, action, initVal, 
			initAction, labelWidth, numberWidth);
	}
	init { arg window, dimensions, label, argControlSpec, argAction, initVal, 
			initAction, labelWidth, numberWidth;
		labelView = JSCStaticText(window, labelWidth @ dimensions.y);
		labelView.string = label;
		labelView.align = \right;
		
		controlSpec = argControlSpec.asSpec;
		initVal = initVal ? controlSpec.default;
		action = argAction;
		
		numberView = JSCNumberBox(window, numberWidth @ dimensions.y);
		numberView.action = {
			numberView.value = value = controlSpec.constrain(numberView.value);
			action.value(this);
		};
		// JJJ
		numberView.maxDecimals = 3;	// for default round of 0.001
		
		if (initAction) {
			this.value = initVal;
		}{
			value = initVal;
			numberView.value = value.round(round);
		};
	}
	value_ { arg value; numberView.valueAction = value }
	set { arg label, spec, argAction, initVal, initAction=false;
		labelView.string = label;
		controlSpec = spec.asSpec;
		action = argAction;
		initVal = initVal ? controlSpec.default;
		if (initAction) {
			this.value = initVal;
		}{
			value = initVal;
			numberView.value = value.round(round);
		};
	}

	// JJJ begin
	round_ { arg argRound;
		var str;
	
		round = argRound;
		
		if( round == 0, {
			numberView.maxDecimals = 8;
		}, {
			str = round.asString;
			numberView.maxDecimals = str.size - (str.indexOf( $. ) ?? 0) - 1;
		});
	}
	// JJJ end

	visible_ { |bool|		[labelView, numberView].do(_.visible_(bool))	}

	remove { [labelView, numberView].do(_.remove) }
}