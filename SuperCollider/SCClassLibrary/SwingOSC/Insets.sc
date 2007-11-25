/**
 *	Helper class like java.awt.Insets, but unmutable.
 *	An Insets object is a representation of the borders of a container.
 *	It specifies the space that a container must leave at each of its edges. 
 *
 *	@version	0.45, 03-Feb-07
 */
Insets {
	var <top, <left, <bottom, <right;
	var allZero;
	
	*new { arg top = 0, left = 0, bottom = 0, right = 0;
		^super.newCopyArgs( top, left, bottom, right ).prInit;
	}
	
	prInit {
		allZero = (top == 0) and: (left == 0) and: (right == 0) and: (bottom == 0);
	}
	
	addTo { arg rect;
		^if( allZero, rect, { rect.insetAll( left, top, right, bottom )});
	}
	
	subtractFrom { arg rect;
		^if( allZero, rect, { rect.insetAll( left.neg, top.neg, right.neg, bottom.neg )});
	}
}