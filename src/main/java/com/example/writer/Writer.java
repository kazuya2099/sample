package com.example.writer;

public interface Writer {
	void write(String data);
	void write(String data, String publisher, int timeout);
}
