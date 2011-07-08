/*
 * TestingXML.java
 *
 * Code was donated by Michael Kröz
 * Adapted by dzan
 */

package com.fsck.k9.parser;

import android.test.AndroidTestCase;
import android.util.Log;
import com.fsck.k9.helper.configxmlparser.ConfigurationXMLHandler;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class TestingXML extends AndroidTestCase {
	private final static String TAG = "PARSERTEST";
	private final String baseAutoconfigURL = "http://live.mozillamessaging.com/autoconfig/";

	public void testParseAutoconfigURLs() throws SAXException, IOException,
			ParserConfigurationException {

		ArrayList<String> configurations = getAutoconfigURLS();
        XMLReader xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        xr.setContentHandler(new ConfigurationXMLHandler());

		for( String url: configurations ) {
			Log.d( TAG, "Parse autoconfig URL: " + url );
            URLConnection conn = new URL(url).openConnection();
		    InputStream in = conn.getInputStream();
			xr.parse(new InputSource(in));
		}
	}

	private ArrayList<String> getAutoconfigURLS() throws IOException, SAXException, ParserConfigurationException  {

		URL url = new URL(baseAutoconfigURL);
		URLConnection connection = url.openConnection();
		InputStream is = connection.getInputStream();
		final Parser parser = new Parser();
			
		Sax2Dom sax2dom = null;
		try {
			sax2dom = new Sax2Dom();
			parser.setContentHandler(sax2dom);
			parser.setFeature(Parser.namespacesFeature, false);
			parser.parse(new InputSource( is ));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Document document = (Document)sax2dom.getDOM();		
		
		NodeList nl = document.getElementsByTagName("a");

		ArrayList<String> configurations = new ArrayList<String>();
		int index;
		for (index = 0; index < nl.getLength(); index++) {
			Node n = nl.item(index);
			NamedNodeMap attributes = n.getAttributes();
			String link = attributes.getNamedItem( "href" ).getNodeValue();
			if ( link.startsWith( "?" ) || link.startsWith( "/" ))
				continue;
			String realLink = baseAutoconfigURL + link;
			configurations.add( realLink );
		}

		return configurations;
	}

}
