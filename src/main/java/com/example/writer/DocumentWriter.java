package com.example.writer;

public class DocumentWriter {
	
	private Writer writer;
	
	public void setWriter(Writer writer) {
		this.writer = writer;
	}

	public void write(String data) {
		writer.write(data);
	}
}
