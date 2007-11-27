/**
 *	JSCTabletView
 *	(SwingOSC classes for SuperCollider)
 *
 *	@version		0.57, 27-Nov-07
 *	@author		Hanns Holger Rutz
 */
JSCTabletView : JSCView {

//	var <>mouseDownAction,<>mouseUpAction;

	var tabletResp;
	
	mouseDown { arg x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
	                absoluteX, absoluteY, buttonMask, tanPressure;
		mouseDownAction.value( this, x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
		                       absoluteX, absoluteY, buttonMask, tanPressure );
	}

	mouseUp { arg x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
	              absoluteX, absoluteY, buttonMask, tanPressure;
		mouseUpAction.value( this, x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
		                     absoluteX, absoluteY, buttonMask, tanPressure );
	}

	doAction { arg x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
	               absoluteX, absoluteY, buttonMask, tanPressure;
		action.value( this, x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
		              absoluteX, absoluteY, buttonMask, tanPressure );
		mouseMoveAction.value( this, x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
		                       absoluteX, absoluteY, buttonMask, tanPressure );
	}

	prClose {
		tabletResp.remove;
		^super.prClose([[ '/method', "tab" ++ this.id, \remove ],
					   [ '/free', "tab" ++ this.id ]]);
	}

	prSCViewNew {
		var bndl;
		
		bndl	= List.new;
		this.prCreateTabletResponder( bndl );
		^super.prSCViewNew([
			[ '/local', this.id, '[', '/new', "de.sciss.swingosc.TabletView", ']' ]
		], bndl );
	}

	prCreateMouseResponder {} // overridden to not create one

	prCreateTabletResponder { arg bndl;
		var msg, win;
	
		if( tabletResp.notNil, {
			"JSCTabletView.prCreateTabletResponder : already created!".warn;
			^nil;
		});
		clpseMouseMove	= Collapse({ arg x, y, modifiers; this.mouseOver( x, y, modifiers )});
		clpseMouseDrag	= Collapse({ arg x, y, modifiers; this.mouseMove( x, y, modifiers )});
	// [ "/tablet", <componentID>, <state>, <deviceID>, <localX>, <localY>, <pressure>,
	//   <tiltX>, <tiltY>, <rota>, <tanPressure>, <absX>, <absY>, <absZ>,
	//   <buttonMask>, <clickCount>
		tabletResp		= OSCpathResponder( server.addr, [ '/tablet', this.id ], { arg time, resp, msg;
			var state, deviceID, x, y, pressure, tiltx, tilty, rotation, tanPressure, absoluteX, absoluteY, absoluteZ,
			    buttonMask, clickCount, buttonNumber, bounds;
		
			state 		= msg[2];
			
			if( state === \proximity, {
			
			}, {	// from tabletEvent
				bounds		= this.bounds;
				deviceID		= msg[3];
				x			= msg[4] - bounds.left;
				y			= msg[5] - bounds.top;
				pressure		= msg[6];
				tiltx		= msg[7];
				tilty		= msg[8];
				rotation		= msg[9];
				tanPressure	= msg[10];
				absoluteX		= msg[11];
				absoluteY		= msg[12];
				absoluteZ		= msg[13];
				buttonMask	= msg[14];
				clickCount	= msg[15];
				
				buttonNumber	= (buttonMask & 2) >> 1;  // hmmm...
	
				case { state === \pressed }
				{
					{ this.mouseDown( x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
			                           absoluteX, absoluteY, buttonMask, tanPressure )}.defer;
				}
				{ state === \released }
				{
					{ this.mouseUp( x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
			                         absoluteX, absoluteY, buttonMask, tanPressure )}.defer;
				}
				{ state === \moved }
				{
	//				{ this.mouseMoved( x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
	//		                            absoluteX, absoluteY, buttonMask, tanPressure )}.defer;
				}
				{ state === \dragged }
				{
					{ this.doAction( x, y, pressure, tiltx, tilty, deviceID, buttonNumber, clickCount, absoluteZ, rotation,
			                          absoluteX, absoluteY, buttonMask, tanPressure )}.defer;
				};
	// note: entered is followed by moved with equal coordinates
	// so we can just ignore it
	//			{ state === \entered }
	//			{
	//				{ this.mouseOver( x, y, modifiers )}.defer;
	//			};
			});
		});
		tabletResp.add;
		msg = [ '/local', "tab" ++ this.id, '[', '/new', "de.sciss.swingosc.TabletResponder", this.id, ']' ];
		if( bndl.notNil, {
			bndl.add( msg );
		}, {
			server.sendBundle( nil, msg );
		});
	}
}