/**
 *	JSCTabletView
 *	(SwingOSC classes for SuperCollider)
 *
 *	@version		0.57, 12-Dec-07
 *	@author		Hanns Holger Rutz
 */
JSCTabletView : JSCAbstractUserView {
	var <>proximityAction;

	var tabletResp, cocoaBorder;
	
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
		
		relativeOrigin	= true;
		cocoaBorder		= if( parent.prGetWindow.border, 20, -2 );
		jinsets			= Insets( 3, 3, 3, 3 );
		bndl				= List.new;
		bndl.add([ '/local', this.id, '[', '/new', "de.sciss.swingosc.TabletView", ']' ]);
		this.prCreateTabletResponder( bndl );
		^super.prSCViewNew( bndl );
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
			    buttonMask, clickCount, buttonNumber, bounds, entering, systemTabletID, tabletID, pointingDeviceType,
			    uniqueID, pointingDeviceID;
		
			state 		= msg[2];
			
			if( state === \proximity, {
				deviceID			= msg[3];
				entering			= msg[4] != 0;
				systemTabletID	= msg[5];
				tabletID			= msg[6];
				pointingDeviceType	= msg[7];
				uniqueID			= msg[8];
				pointingDeviceID	= msg[9];

				proximityAction.value( this, entering, deviceID, pointingDeviceType, systemTabletID, pointingDeviceID, tabletID, uniqueID );

			}, {	// from tabletEvent
				bounds		= this.bounds;
				deviceID		= msg[3];
				x			= msg[4] - bounds.left;
				y			= bounds.bottom - msg[5] + cocoaBorder; // sucky cocoa
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
		msg = [ '/local', "tab" ++ this.id, '[', '/new', "de.sciss.swingosc.TabletResponder", this.id, parent.prGetWindow.id, ']' ];
		if( bndl.notNil, {
			bndl.add( msg );
		}, {
			server.sendBundle( nil, msg );
		});
	}
}