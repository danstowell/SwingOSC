package de.sciss.swingosc;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import de.sciss.gui.PeakMeter;
import de.sciss.gui.PeakMeterCaption;

public class PeakMeterGroup
extends JPanel
{
	private PeakMeter[]			meters;
	private PeakMeterCaption	caption;
	private int					numChannels	= 0;
	
	public PeakMeterGroup()
	{
		super();
		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ));
		setBackground( Color.black );
		setBorder( new RecessedBorder() );
	}
	
	public void setNumChannels( int numChannels )
	{
		if( numChannels !=  this.numChannels ) {
			this.numChannels = numChannels;
			rebuildMeters();
		}
	}

	private void rebuildMeters()
	{
		removeAll();
		
		meters	= new PeakMeter[ numChannels ];
		caption	= new PeakMeterCaption();
//		caption.setForeground( Color.white ); // ( new Color( 0xFF, 0xFF, 0xFF, 0x7F ));
		caption.setBorder( BorderFactory.createEmptyBorder( 5, 1, 4, 0 ));
		add( caption );
		for( int ch = 0; ch < meters.length; ch++ ) {
			meters[ ch ] = new PeakMeter();
			meters[ ch ].setRefreshParent( true );
			meters[ ch ].setBorder( BorderFactory.createEmptyBorder( 5, 1, 4, 1 ));
			meters[ ch ].setTicks( 101 );
			add( meters[ ch ]);
		}
//		masterMeters = meters;
//		lmm.setMeters( masterMeters );
	}
}
