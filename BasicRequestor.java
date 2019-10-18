package com.solace.connect;


import java.util.UUID;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.stereotype.Service;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import com.solacesystems.jms.SupportedProperty;

@Service
public class BasicRequestor {

    public String process(String payload) throws Exception {

       
        SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory();
        connectionFactory.setHost("tcp://10.140.102.113:55555");
        connectionFactory.setVPN("ST_O2A_VPN");
        connectionFactory.setUsername("tibcoesb");
        connectionFactory.setPassword("tibcoesb");

        Connection connection = connectionFactory.createConnection();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

       
        Queue queue = session.createQueue("test1");
        MessageProducer requestProducer = session.createProducer(queue);

        Queue replyToQueue = session.createQueue("test");

        MessageConsumer replyConsumer = session.createConsumer(replyToQueue);

        // Start receiving replies
        connection.start();

        // Create a request.
        TextMessage request = session.createTextMessage(payload);
        // The application must put the destination of the reply in the replyTo field of the request
        request.setJMSReplyTo(replyToQueue);
        // The application must put a correlation ID in the request
        String correlationId = UUID.randomUUID().toString();
        request.setJMSCorrelationID(correlationId);


        // Send the request
        requestProducer.send(queue, request, DeliveryMode.NON_PERSISTENT,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE);

        System.out.println("Sent successfully. Waiting for reply..."+request.getJMSCorrelationID());

        // the main thread blocks at the next statement until a message received or the timeout occurs
        Message reply = replyConsumer.receive(10000);
        if (reply == null) {
            throw new Exception("Failed to receive a reply in " + 10000 + " msecs");
        }
        System.out.printf("Message Content:%n%s%n", SolJmsUtility.dumpMessage(reply));

        // Process the reply
        if (reply.getJMSCorrelationID() == null) {
            throw new Exception(
                    "Received a reply message with no correlationID. This field is needed for a direct request.");
        }

        // Apache Qpid JMS prefixes correlation ID with string "ID:" so remove such prefix for interoperability
        if (!reply.getJMSCorrelationID().replaceAll("ID:", "").equals(correlationId)) {
            throw new Exception("Received invalid correlationID in reply message.");
        }

        if (reply instanceof TextMessage) {
            System.out.printf("TextMessage response received: '%s'%n", ((TextMessage) reply).getText());
            if (!reply.getBooleanProperty(SupportedProperty.SOLACE_JMS_PROP_IS_REPLY_MESSAGE)) {
                System.out.println("Warning: Received a reply message without the isReplyMsg flag set.");
            }
        } else {
            System.out.println("Message response received.");
        }

        

        connection.stop();
        replyConsumer.close();
        requestProducer.close();
        session.close();
        connection.close();
        
        return ((TextMessage) reply).getText();
    }

  }

