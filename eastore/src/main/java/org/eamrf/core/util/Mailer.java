/**
 * 
 */
package org.eamrf.core.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @author sal
 *
 * Utility class for sending email.
 */
public class Mailer {
	
	public Mailer() {}

	/**
	 * Send email.
	 * 
	 * @param smtpServer - SMTP server.
	 * @param to - The To: email address. Comma delimit for more than one address.
	 * @param from - The From: email address.
	 * @param subject - Email subject line.
	 * @param body - Body of the email.
	 * @throws Exception
	 */
	public void send(String smtpServer, String to, String from, String subject, String body) throws Exception {

		try{

			checkRequiredFields(smtpServer,to,from);

			String sub = StringUtil.changeNull(subject);
			String bod = StringUtil.changeNull(body);

			Properties props = System.getProperties();
			props.put("mail.smtp.host", smtpServer);

			// get default session
			Session session = Session.getDefaultInstance(props, null);

			// create Message object
			Message msg = new MimeMessage(session);

			// set from and to fields
			msg.setFrom(new InternetAddress(from));
			msg.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to, false));

			// set subject and body fields
			msg.setSubject(sub);
			msg.setText(bod);

			// set header and date
			msg.setHeader("X-Mailer", "Frontier Science and Technology");
			msg.setSentDate(new Date());

			// send the message
			Transport.send(msg);

		}catch(MessagingException me){
			throw new Exception(Mailer.class.getName() + ".send(): " + me.getMessage());
		}

	}
	
	/**
	 * Checks to make sure a SMTP server, From address, and To address were provided.
	 *
	 * @return true if everything is ok, false otherwise.
	 */
	private void checkRequiredFields(String smtpServer, String to, String from) throws Exception {

		if(StringUtil.isNullEmpty(smtpServer)){
			throw new Exception(Mailer.class.getName() + ".checkRequiredFields(): You must specify a SMTP server.");
		}else if(StringUtil.isNullEmpty(to)){
			throw new Exception(Mailer.class.getName() + ".checkRequiredFields(): You must specify the To field.");
		}else if(StringUtil.isNullEmpty(from)){
			throw new Exception(Mailer.class.getName() + ".checkRequiredFields(): You must specify the From field.");
		}
	}		
}
