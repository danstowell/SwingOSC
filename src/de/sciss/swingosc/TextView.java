/*
 *  TextView.java
 *  SwingOSC
 *
 *  Copyright (c) 2005-2009 Hanns Holger Rutz. All rights reserved.
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
 *  Changelog:
 *		08-Feb-07	created
 */
package de.sciss.swingosc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *	Extends <code>JTextPane</code> by a buffering
 *	mechanism for data updates and utility methods for styling.
 *
 *	@author		Hanns Holger Rutz
 *	@version	0.63, 21-Dec-09
 */
public class TextView
extends JTextPane
{
	private final StringBuffer updateData = new StringBuffer();
	protected final List collDocListeners = new ArrayList();

	public TextView()
	{
		super();
//		addHyperlinkListener( new HyperlinkListener() {
//			public void hyperlinkUpdate( HyperlinkEvent e )
//			{
//				System.out.println( "RECEIVED : " + e.getEventType() );
//			}
//		});
		
		// this automatically moves document listeners to
		// a new doc
		addPropertyChangeListener( "document", new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent pce )
			{
//System.out.println( "propertyChange : doc" );
				
				// unregister old
				final Document oldDoc = (Document) pce.getOldValue();
				if( oldDoc != null ) {
					for( int i = 0; i < collDocListeners.size(); i++ ) {
						oldDoc.removeDocumentListener( (DocumentListener) collDocListeners.get( i ));
//System.out.println( "remove " + collDocListeners.get( i ));
					}
					if( oldDoc instanceof AbstractDocument ) {
						final AbstractDocument adoc = (AbstractDocument) oldDoc;
						final DocumentEvent de =
							adoc.new DefaultDocumentEvent( oldDoc.getStartPosition().getOffset(),
							                               oldDoc.getLength(),
							                               DocumentEvent.EventType.REMOVE );
						// simulate clear
						for( int i = 0; i < collDocListeners.size(); i++ ) {
							final DocumentListener l = (DocumentListener) collDocListeners.get( i );
							l.removeUpdate( de );
						}
					}
				}
				// unregister new
				final Document newDoc = (Document) pce.getNewValue();
				if( oldDoc != null ) {
					for( int i = 0; i < collDocListeners.size(); i++ ) {
						newDoc.addDocumentListener( (DocumentListener) collDocListeners.get( i ));
//System.out.println( "add " + collDocListeners.get( i ));
					}
					if( newDoc instanceof AbstractDocument ) {
						final AbstractDocument adoc = (AbstractDocument) newDoc;
						final DocumentEvent de =
							adoc.new DefaultDocumentEvent( newDoc.getStartPosition().getOffset(),
							                               newDoc.getLength(),
							                               DocumentEvent.EventType.INSERT );
						// simulate clear
						for( int i = 0; i < collDocListeners.size(); i++ ) {
							final DocumentListener l = (DocumentListener) collDocListeners.get( i );
							l.insertUpdate( de );
						}
					}
				}
			}
		});
	}

	public void beginDataUpdate()
	{
		updateData.setLength( 0 );
	}
	
	public void addData( String update )
	{
		updateData.append( update );
	}
	
	public void endDataUpdate( int insertPos, int replaceLen )
	throws BadLocationException
	{
		setString( insertPos, replaceLen, updateData.toString() );
		updateData.setLength( 0 );
	}
	
	public void read( String path )
	throws IOException
	{
		readURL( new File( path ).toURL() );
	}
	
//	public void readURL( URL url )
//	throws IOException
//	{
//		
//	}
	
	// e.g. 'text/html; charset=utf-8'
	private String extractType( String fullType )
	{
		final int i = fullType.indexOf( ';' );
		return i < 0 ? fullType : fullType.substring( 0, i );
	}
	
//	private String extractCharset( String fullType )
//	{
//		final int i = fullType.indexOf( "charset=" );
//		if( i >= 0 ) {
//			final int j = fullType.indexOf( ";", i + 8 );
//			return fullType.substring( i + 8, j >= 0 ? j : fullType.length() );
//		} else {
//			return null;
//		}
//	}

	public EditorKit getEditorKitForContentType( String type ) {
		final EditorKit kit = super.getEditorKitForContentType( overrideContentType == null ? type : overrideContentType );
//		System.out.println( "getEditorKitForContentType( " + type + " ) => " + kit );
		return kit;
	}
	
	private String overrideContentType = null;
	
	public void readURL( URL url )
	throws IOException
	{
		final URLConnection con = url.openConnection();
//		final String ctyp = con.getContentType();
		final String ctyp = extractType( con.getContentType() );
		
//		final String mime;
		if( ctyp == null || ctyp.equals( "content/unknown" )) {
			final String path = url.getPath();
			final int i = path.lastIndexOf( '.' ) + 1;
			final String ext = path.substring( i );
			final String mime;
			if( ext.equals( "htm" ) ||
				ext.equals( "html" )) {
				mime = "text/html";
			} else if( ext.equals( "rtf" )) {
				mime = "text/rtf"; 
			} else {
				mime = "text/plain";
			}
			// tricky shit to get RTF to work...
			try {
				overrideContentType = mime;
				setPage( url );
			}
			finally {
				overrideContentType = null;
			}
		} else {
			setPage( url );
//			mime = ctyp;
		}

//		final EditorKit kit = JEditorPane.createEditorKitForContentType( mime );
//System.out.println( "mime is '" + mime + "'; kit is '" + kit + "'" );
//		if( kit == null ) {
//			System.out.println( "Cannot create editor kit for type '" + mime + "'" );
//			return;
//		}
//
//		setEditorKit( kit );
//		setPage( url );
		
/*		
		try {
			removeAllDocListeners();
			setEditorKit( kit );
			final Document doc = getDocument();
			// we need to make sure to set the base URL
			// otherwise relative links (and images) are broken
System.out.println( "--1" );
			if( doc instanceof HTMLDocument ) {
System.out.println( "--2" );
				final HTMLDocument htmlDoc = (HTMLDocument) doc;
				final String f = url.getFile();
				final int i = f.indexOf( '?' );
				final String baseFile = i < 0 ? f : f.substring(  0, i );
				final URL baseURL = new URL( url.getProtocol(), url.getHost(),
				                             url.getPort(), baseFile );
				htmlDoc.setBase( baseURL );
			}
			final InputStream is = con.getInputStream();
			try {
				this.read( is, doc );
			}
			finally {
				try { is.close(); } catch( IOException e2 ) {
				 	// ignore
				}
			}
		}
		finally {
			addAllDocListeners();
		}
*/
		
/*
		boolean retry  = false;
		boolean didRetry;
		String charset = extractCharset( con.getContentType() );
		try {
			removeAllDocListeners();
			setEditorKit( kit );
			do {
				didRetry = retry;
				final InputStream is = con.getInputStream();
				final InputStreamReader isr = (charset == null) ?
					new InputStreamReader( is ) :
					new InputStreamReader( is, charset );
				try {
					kit.read( isr, getDocument(), 0 );
				}
				catch( ChangedCharSetException e2 ) {
					charset = extractCharset( e2.getCharSetSpec() );
					if( charset != null ) {
						System.out.println( "charset is '" + charset + "'" );
						retry = true;
					}
				}
			} while( retry && !didRetry );
		}
		catch( BadLocationException e1 ) {
			e1.printStackTrace(); // should not happen with pos == 0
		}
		finally {
			addAllDocListeners();
		}
*/
	}
	
//	public void setPage( String url )
//	throws IOException
//	{
//		removeAllDocListeners();
//		try {
//			super.setPage( url );
//		}
//		finally {
//			addAllDocListeners();
//		}
////System.out.println( "now we've got " + this.getHyperlinkListeners().length + " listeners" );
////System.out.println( "EditorKit is " + this.getEditorKit().getContentType() );
//	}
//	
//	public void setPage( URL url )
//	throws IOException
//	{
//		removeAllDocListeners();
//		try {
//			super.setPage( url );
//		}
//		finally {
//			addAllDocListeners();
//		}
////System.out.println( "now we've got " + this.getHyperlinkListeners().length + " listeners" );		
////System.out.println( "EditorKit is " + this.getEditorKit().getContentType() );
//	}
	
//	private void addAllDocListeners()
//	{
//		final Document doc = getDocument();
//		for( int i = 0; i < collDocListeners.size(); i++ ) {
//			doc.addDocumentListener( (DocumentListener) collDocListeners.get( i ));
//		}
//	}
//	
//	private void removeAllDocListeners()
//	{
//		final Document doc = getDocument();
//		for( int i = 0; i < collDocListeners.size(); i++ ) {
//			doc.removeDocumentListener( (DocumentListener) collDocListeners.get( i ));
//		}
//	}

	public void setString( int insertPos, int replaceLen, String str )
	throws BadLocationException
	{
		final Document doc = getDocument();
		if( insertPos == -1 ) {
			insertPos = 0;
			replaceLen = doc.getLength();
		} else {
			insertPos 	= Math.max( 0, Math.min( insertPos, doc.getLength () ));
			replaceLen	= Math.max( 0, Math.min( replaceLen, doc.getLength() - insertPos ));
		}
		doc.remove( insertPos, replaceLen );
		doc.insertString( insertPos, str, null );
	}
	
	public void setFont( int rangeStart, int len, Font f )
	{
		if( rangeStart == -1 ) {
			// fixes a bug with empty documents where
			// the font is not properly applied through setCharacterAttributes...
			setFont( f );
// NOTE: don't return, because we still need to execute the below
// with HTML pages!
//			return;
		}
		final MutableAttributeSet as = new SimpleAttributeSet();
		StyleConstants.setFontFamily( as, f.getFamily() );
		StyleConstants.setFontSize( as, f.getSize() );
		StyleConstants.setBold( as, f.isBold() );
		StyleConstants.setItalic( as, f.isItalic() );
		applyAttr( rangeStart, len, as );
	}
	
	public void setForeground( int rangeStart, int len, Color c )
	{
		if( rangeStart == -1 ) {
			// fixes a bug with empty documents where
			// the colour is not properly applied through setCharacterAttributes...
			setForeground( c );
// NOTE: don't return, because we still need to execute the below
// with HTML pages!
//			return;
		}
		final MutableAttributeSet as = new SimpleAttributeSet();
		StyleConstants.setForeground( as, c );
		applyAttr( rangeStart, len, as );
	}
	
	// this is here to make DocumentResponder less complex
	// (because now it can connect both Caret and Document listeners to the same object)
	// ; this just forwards the request to the Document.
	// it takes care of removing and adding the listeners
	// automatically if the document changes
	public void addDocumentListener( DocumentListener l )
	{
		collDocListeners.add( l );
		getDocument().addDocumentListener( l );
	}
	
//	public void addHyperlinkListener( HyperlinkListener l )
//	{
//		System.out.println( "addHyperlinkListener" );
//		super.addHyperlinkListener( l );
//	}

	public void removeDocumentListener( DocumentListener l )
	{
		collDocListeners.remove( l );
		getDocument().removeDocumentListener( l );
	}

	private void applyAttr( int rangeStart, int len, AttributeSet as )
	{
		final StyledDocument doc = getStyledDocument();
		if( rangeStart == -1 ) {
			rangeStart	= 0;
			len			= doc.getLength();
		}
		doc.setCharacterAttributes( rangeStart, len, as, false );
	}

	public void paintComponent( Graphics g )
	{
		final Color colrBg	= getBackground();

		if( (colrBg != null) && (colrBg.getAlpha() > 0) ) {
			g.setColor( colrBg );
			g.fillRect( 0, 0, getWidth(), getHeight() );
		}
		super.paintComponent( g );
	}

	public void setBackground( Color c )
	{
		setOpaque( (c != null) && (c.getAlpha() == 0xFF) );
		super.setBackground( c );
	}
}
