package com.example.writer;

import java.io.FileOutputStream;
import java.io.IOException;

public class PdfWriter implements Writer {
	
	@Override
	public void write(String data) {
		try(FileOutputStream osw = new FileOutputStream("sample.pdf")) {
			osw.write(data.getBytes());
			System.out.println("-- PdfWriter --");
			System.out.println(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void write(String data, String publisher, int timeout) {
		data +=  "publisher : " + publisher + ", timeout : " + timeout;
		try(FileOutputStream osw = new FileOutputStream("sample.pdf")) {
			osw.write(data.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
