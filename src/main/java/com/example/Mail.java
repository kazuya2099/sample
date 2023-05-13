package com.example;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mail {
	private static final String APP_PW ="pprbyuszscaqfgbd";
	
	public static void main(String[] args) {
		try {
			Properties property = new Properties();
			property.put("mail.smtp.host", "smtp.gmail.com");
			property.put("mail.smtp.auth", "true");
			property.put("mail.smtp.starttls.enable", "true");
			property.put("mail.smtp.host", "smtp.gmail.com");
			property.put("mail.smtp.port", "587");
			property.put("mail.smtp.debug", "true");
			property.put("mail.smtp.ssl.protocols", "TLSv1.2");
			property.put("mail.smtp.ssl.trust", "smtp.gmail.com");

			Session session = Session.getInstance(property, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication("mhamada01@gmail.com", APP_PW);
				}
			});
			
			MimeMessage mimeMessage = new MimeMessage(session);
			InternetAddress toAddress = new InternetAddress("mhamada01@outlook.jp", "mhamada01@outlook.jp");
			mimeMessage.setRecipient(Message.RecipientType.TO, toAddress);
			InternetAddress fromAddress = new InternetAddress("mhamada01@gmail.com", "mhamada01@gmail.com");
			mimeMessage.setFrom(fromAddress);
			mimeMessage.setSubject("title", "ISO-2022-JP");
			mimeMessage.setText("message", "ISO-2022-JP");
			Transport.send(mimeMessage);
			System.out.println("メール送信が完了しました。");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
