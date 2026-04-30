//package com.t24extractor.utils;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jms.core.JmsTemplate;
//import org.springframework.jms.core.MessageCreator;
//
//public class Sender
//{
//    private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);
//
//    @Autowired
//    private JmsTemplate jmsTemplate;
//
//    public void send(String dest, String message)
//    {
//        LOGGER.info("sending message='{}'", message);
//
//        MessageCreator messageCreator = session -> session.createTextMessage(message);
//
//        jmsTemplate.send(dest, messageCreator);
//    }
//}
