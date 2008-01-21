/*
 *	JSCViews collection 2
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
 *	@author		Hanns Holger Rutz
 *	@version		0.58, 10-Dec-07
 */
JEZSlider 
{
	var <>labelView, <>sliderView, <>numberView, <>controlSpec, <>action, <value;
	var <round = 0.001;
	
	// ----------------- constructor -----------------

	*new { arg window, dimensions, label, controlSpec, action, initVal, 
			initAction = false, labelWidth = 80, numberWidth = 80;
		^super.new.init( window, dimensions, label, controlSpec, action, initVal, 
			initAction, labelWidth, numberWidth );
	}
	
	// ----------------- public instance methods -----------------

	value_ { arg value; numberView.valueAction = value }
	
	set { arg label, spec, argAction, initVal, initAction = false;
		labelView.string	= label;
		controlSpec		= spec.asSpec;
		action			= argAction;
		initVal			= initVal ? controlSpec.default;
		if( initAction, {
			this.value		= initVal;
		}, {
			value			= initVal;
			sliderView.value	= controlSpec.unmap(value);
			numberView.value	= value.round(round);
		});
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

	visible_ { arg bool; [ labelView, sliderView, numberView ].do( _.visible_( bool ))}
	
	remove { [ labelView, sliderView, numberView ].do( _.remove )}

	// ----------------- private instance methods -----------------

	init { arg window, dimensions, label, argControlSpec, argAction, initVal, 
			initAction, labelWidth, numberWidth;
		var	decorator = window.asView.tryPerform( \decorator ),			gap = decorator.tryPerform( \gap );				gap.notNil.if({			(dimensions = dimensions.copy).x_( dimensions.x - (2 * gap.x) );		});		labelView			= JSCStaticText( window, labelWidth @ dimensions.y );
		labelView.string	= label;
		labelView.align	= \right;
		
		controlSpec		= argControlSpec.asSpec;
		initVal			= initVal ? controlSpec.default;
		action			= argAction;
		
		sliderView		= JSCSlider( window, (dimensions.x - labelWidth - numberWidth) @ dimensions.y );
		sliderView.action	= {
			value			= controlSpec.map( sliderView.value );
			numberView.value	= value.round( round );
			action.value( this );
		};
// JJJ
//		if (controlSpec.step != 0) {
//			sliderView.step = (controlSpec.step / (controlSpec.maxval - controlSpec.minval));
//		};

		sliderView.receiveDragHandler = { arg slider;
			slider.valueAction = controlSpec.unmap( JSCView.currentDrag );
		};
		
		sliderView.beginDragAction = { arg slider;
			controlSpec.map( slider.value );
		};

		numberView		= JSCNumberBox( window, numberWidth @ dimensions.y );
		numberView.action	= {
			numberView.value 	= value = controlSpec.constrain( numberView.value );
			sliderView.value	= controlSpec.unmap( value );
			action.value( this );
		};
		// JJJ
		numberView.maxDecimals = 3;	// for default round of 0.001

		// JJJ this fires a value change for
		// the slider, therefore numverView.action must have
		// been assigned before!!!
		if( controlSpec.step != 0, {
			sliderView.step = controlSpec.step / (controlSpec.maxval - controlSpec.minval);
		});
		
		if( initAction, {
			this.value		= initVal;
		}, {
			value			= initVal;
			sliderView.value	= controlSpec.unmap(value);
			numberView.value	= value.round(round);
		});
	}
}

JEZNumber
{
	var <>labelView, <>numberView, <>controlSpec, <>action, <value;
	var <round = 0.001;
	
	// ----------------- constructor -----------------

	*new { arg window, dimensions, label, controlSpec, action, initVal, 
			initAction=false, labelWidth=80, numberWidth = 80;
		^super.new.init(window, dimensions, label, controlSpec, action, initVal, 
			initAction, labelWidth, numberWidth);
	}

	// ----------------- public instance methods -----------------

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

	visible_ { |bool|
		[labelView, numberView].do(_.visible_(bool))
	}

	remove { [labelView, numberView].do(_.remove) }

	// ----------------- private instance methods -----------------
	
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
}