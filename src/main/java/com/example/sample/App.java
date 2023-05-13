package com.example.sample;

import java.beans.Introspector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;

import lombok.Data;


/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		App app = new App();
		app.validation();
		app.introspector("Introspector");
		app.componentType(String[].class);
		app.jackson2("{\"data\":\"あいうえお\"}");
	}
	
	private void validation() {
		boolean isKanji = "太郎".matches("^[一-鿕]*$");
		boolean isHiragana = "濱田".matches("^[ぁ-ゖ]*$");
		boolean isKatakana = "濱田".matches("^[ァ-ヺ]*$");
		// CJK用記号 　-〜
		// 一般句読点‐-…
		//全角系！-／０-９：-＠Ａ-Ｚ［-｀ａ-ｚ｛-～
		boolean isZenkaku = "　〜「」！＃＄％＆（）＝～｜＠；：＋＊｝｛｝＜＞？＿”‘’".matches("^[　-〜‐-…！-／０-９：-＠Ａ-Ｚ［-｀ａ-ｚ｛-～]*$");
		System.out.println(isKanji);
		System.out.println(isHiragana);
		System.out.println(isKatakana);
		System.out.println(isZenkaku);
	}
	
	private void introspector(String target) {
		String result = Introspector.decapitalize(target);
		System.out.println(result);
	}
	
	private void componentType(Class<?> clazz) {
		String result = clazz.getComponentType().getName();
		System.out.println(result);
	}
	
	private void jackson2(String input) {
		InputStream body = new ByteArrayInputStream(input.getBytes());
		HttpHeaders headers = new HttpHeaders();
		HttpInputMessage httpInputMessage = new MappingJacksonInputMessage(body, headers);
		
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		try {
			InputDto inputDto = (InputDto) converter.read(InputDto.class, httpInputMessage);
			System.out.println("インプット：" + inputDto.getData());
			
			OutputStream outputStream = new ByteArrayOutputStream();
			HttpOutputMessage httpOutputMessage = new HttpOutputMessage() {
				@Override
				public OutputStream getBody() {
					return outputStream;
				}
				@Override
				public HttpHeaders getHeaders() {
					return headers;
				}
			};
			converter.write(inputDto, InputDto.class, new MediaType("content-type"), httpOutputMessage);
			System.out.println("アウトプット：" + outputStream.toString());
		} catch (HttpMessageNotReadableException | IOException e) {
			e.printStackTrace();
		}
	}
	
	@Data
	private static class InputDto {
		private String data;
	}
}
