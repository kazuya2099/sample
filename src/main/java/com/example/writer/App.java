package com.example.writer;

import java.util.Locale;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		App app = new App();
		app.execute();
	}
	
	public void execute() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
		DocumentWriter documentWriter = (DocumentWriter) applicationContext.getBean("documentWriter");
		documentWriter.write("あいうえお");
		System.out.println();
		
		String message = applicationContext.getMessage("message.test", null, Locale.ENGLISH);
		System.out.println(message);
		
		String path = applicationContext.getEnvironment().getProperty("Path");
		System.out.println(path);
		
	}
}
