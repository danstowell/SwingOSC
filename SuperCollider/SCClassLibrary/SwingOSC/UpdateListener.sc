/**
 *	(C)opyright 2006-2008 Hanns Holger Rutz. All rights reserved.
 *	Distributed under the GNU General Public License (GPL).
 *
 *	Useful adapter for descendants/update mechanism
 *
 *	Class dependancies: none
 *
 *	@author	Hanns Holger Rutz
 *	@version	0.14, 23-Dec-07
 */
UpdateListener // interface
{
	var <>verbose = false;

	// adapter implementation
	var funcUpdate, objects, filter;

	// interface definition
	*names {
		^[ \update ];
	}

	*new { arg update, what;
		^super.new.prInit( update, what );
	}
	
	*newFor {Êarg object, update, what;
		^this.new( update, what ).addTo( object );
	}
	
	addTo { arg object;
		object.addDependant( this );
		if( objects.isNil, {
			objects = IdentitySet[ object ];
		}, {
			if( objects.includes( object ), {
				MethodError( "Cannot attach to the same object more than once", thisMethod ).throw;
			});
			objects.add( object );
		});
	}

	removeFrom { arg object;
		object.removeDependant( this );
		if( objects.includes( object ).not, {
			MethodError( "Was not attached to this object", thisMethod ).throw;
		});
		objects.remove( object );
	}
	
	removeFromAll {
		objects.do({ arg object;
			object.removeDependant( this );
		});
		objects = nil;
	}
	
	// same as removeFromAll ; makes transition from Updater easier
	remove {
		^this.removeFromAll;
	}
	
	isListening {
		^(objects.size > 0);
	}
		
	isListeningTo { arg object;
		^objects.includes( object );
	}
		
	update { arg object, what ... args;
		if( verbose, {
			("UpdateListener.update : object = "++object++"; status = "++what).postln;
		});
		if( filter.isNil, {
			funcUpdate.value( this, object, what, *args );
		}, {
			if( what === filter, {
				funcUpdate.value( this, object, *args );
			});
		});
	}

	// ------------ private ------------

	prInit { arg update, what;
		funcUpdate	= update;
		filter		= what;
	}
}