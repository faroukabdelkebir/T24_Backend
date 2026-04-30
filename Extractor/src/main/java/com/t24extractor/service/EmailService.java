//package com.t24extractor.service;
//
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import java.time.LocalDate;
//import java.util.List;
//
//import org.apache.commons.lang.StringUtils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.messaging.MessagingException;
//import org.springframework.stereotype.Service;
//
//import com.t24extractor.model.Job;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Slf4j
//public class EmailService {
//
//	private final JavaMailSender javaMailSender;
//
//	@Value("${users}")
//	private String users;
//
//	public EmailService(JavaMailSender javaMailSender) {
//		this.javaMailSender = javaMailSender;
//	}
//
//	public void sendErrorEmail(String tableName, String errorMessage)
//			throws MessagingException, jakarta.mail.MessagingException {
//		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
//		helper.setSubject("Erreur Chargement table " + tableName);
//
//		String html = "<!doctype html>\n" + "<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"\n"
//				+ "      xmlns:th=\"http://www.thymeleaf.org\">\n" + "<head>\n" + "    <meta charset=\"UTF-8\">\n"
//				+ "    <meta name=\"viewport\"\n"
//				+ "          content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">\n"
//				+ "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" + "    <title>Email</title>\n"
//				+ "</head>\n" + "<body>\n" + "<div>Bonjour, </div>\n" + "</br>\n" + "\n" + "</div>\n"
//				+ "<div>Une erreur s'est produite lors du chargement de la table <b>" + tableName + ".</b></div>\n"
//				+ "</br>\n" + "<div>La description de l'erreur est comme suit:</div>\n" + "<div>" + errorMessage
//				+ "</div>\n" + "<div></div>\n" + "</br>\n" + "<div>Cordialement,</div>\n" + "</body>\n" + "</html>\n";
//		helper.setText(html, true);
//		String[] recipientList = users.split(";");
//		InternetAddress[] recipientAddress = new InternetAddress[recipientList.length];
//		int counter = 0;
//		for (String recipient : recipientList) {
//			recipientAddress[counter] = new InternetAddress(recipient.trim());
//			counter++;
//		}
//		helper.setFrom("T24.Extractor@banquezitouna.com");
//		helper.setTo(recipientAddress);
//		javaMailSender.send(mimeMessage);
//	}
//
//	public void sendFinalEmail(List<Job> parameterList) throws MessagingException {
//		try {
//			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//
//			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
//			helper.setSubject("Finalisation de l'extraction T24 " + LocalDate.now());
//
//			String html = "<!doctype html>\n" + "<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"\n"
//					+ "      xmlns:th=\"http://www.thymeleaf.org\">\n" + "<head>\n" + "    <meta charset=\"UTF-8\">\n"
//					+ "    <meta name=\"viewport\"\n"
//					+ "          content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">\n"
//					+ "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" + "    <title>Email</title>\n"
//					+ "</head>\n" + "<body>\n" + "<div>Bonjour, </div>\n" + "</br>\n" + "\n" + "</div>\n"
//					+ "<div></div>\n" + "</br>\n" + "<div>Voici le détail de l'extraction T24 de " + LocalDate.now()
//					+ ":</div>" +"<div></div>\n"
//					+ "<table><tr><th>Nom de la table</th><th>Status</th><th>Durée</th><th>Read</th><th>Write</th></tr>";
//
//			for (Job job : parameterList) {
//				html = html + "<tr><td>" + job.getName() + "</td><td>" + job.getStatus() + "</td><td>"
//						+ (job.getDuration() == null ? "00:00:00" : StringUtils.substring(job.getDuration(), 0,8))
//
//						+ "</td><td>" + job.getRead() + "</td><td>" + job.getWrite() + "</td></tr>";
//			}
//
//			html = html + "</table><div></div>\n" + "</br>\n" + "<div>Cordialement,</div>\n"
//					+ "<div>Equipe Extracteur T24,</div>\n" + "</body>\n" + "</html>\n";
//			helper.setText(html, true);
//			String[] recipientList = users.split(";");
//			InternetAddress[] recipientAddress = new InternetAddress[recipientList.length];
//			int counter = 0;
//			for (String recipient : recipientList) {
//				recipientAddress[counter] = new InternetAddress(recipient.trim());
//				counter++;
//			}
//			helper.setFrom("T24.Extractor@banquezitouna.com");
//			helper.setTo(recipientAddress);
//			javaMailSender.send(mimeMessage);
//		} catch (Exception e) {
//			log.error("An error occured when try to send Email: " + e.getMessage());
//		}
//	}
//}
