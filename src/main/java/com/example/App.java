package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;

public class App {
	
	public static void main(String[] args) throws Exception {
		App app = new App();
		app.unmarshal();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void unmarshal() throws ParserConfigurationException, SAXException, IOException {
		try (InputStream is = this.getClass().getResourceAsStream("/test.xml")) {
			XStream xstream = new XStream();
			xstream.alias("map", Map.class);
			Map map = (Map) xstream.fromXML(is);
			map.forEach((key, value) -> {
				System.out.println(key + " : " + value);
			});
		}
	}
}