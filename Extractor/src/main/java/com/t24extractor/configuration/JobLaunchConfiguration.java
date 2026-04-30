//package com.t24extractor.configuration;
//
//import com.t24extractor.model.TableMessage;
//import jakarta.jms.ConnectionFactory;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.batch.integration.launch.JobLaunchingGateway;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.PropertySource;
//import org.springframework.integration.channel.DirectChannel;
//import org.springframework.integration.config.EnableIntegration;
//import org.springframework.integration.dsl.IntegrationFlow;
//import org.springframework.integration.handler.LoggingHandler;
//import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
//import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
//import org.springframework.jms.listener.DefaultMessageListenerContainer;
//import org.springframework.jms.support.converter.MarshallingMessageConverter;
//import org.springframework.jms.support.converter.MessageType;
//import org.springframework.messaging.MessageChannel;
//import org.springframework.oxm.jaxb.Jaxb2Marshaller;
//
//@Configuration
//@EnableIntegration
//@PropertySource("classpath:application.properties")
//public class JobLaunchConfiguration
//{
//    /*
//    * This the logic of launching a job using jms event driven endpoint
//    */
//
//    @Value("${queue.destination}")
//    private String queue;
//
//    @Qualifier("jmsConnectionFactory")
//    @Autowired
//    private ConnectionFactory connectionFactory;
//
//
//    @Autowired
//    private JobLaunchingGateway jobLaunchingGateway;
//
//    @Bean
//    public MessageChannel inputChannel()
//    {
//        return new DirectChannel();
//    }
//
//    @Bean
//    public MessageChannel outputChannel()
//    {
//        return new DirectChannel();
//    }
//
//
//    @Bean
//    public DefaultMessageListenerContainer messageListenerContainer()
//    {
//    	DefaultMessageListenerContainer simpleMessageListenerContainer = new DefaultMessageListenerContainer();
//
//        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
//        simpleMessageListenerContainer.setDestinationName(queue);
//        simpleMessageListenerContainer.getMessageListener();
//        simpleMessageListenerContainer.setAutoStartup(true);
//        simpleMessageListenerContainer.setSessionTransacted(false);
//        simpleMessageListenerContainer.setExposeListenerSession(false);
//
//        return simpleMessageListenerContainer;
//    }
//
//    @Bean
//    public JmsMessageDrivenEndpoint jmsMessageDrivenEndpoint()
//    {
//        ChannelPublishingJmsMessageListener channelPublishingJmsMessageListener =
//        		new ChannelPublishingJmsMessageListener();
//
//        channelPublishingJmsMessageListener.setRequestChannel(inputChannel());
//        channelPublishingJmsMessageListener.setMessageConverter(jmsMarshallingMessageConverter());
//
//        return new JmsMessageDrivenEndpoint(messageListenerContainer(), channelPublishingJmsMessageListener);
//    }
//
//    @Bean
//    public MarshallingMessageConverter jmsMarshallingMessageConverter()
//    {
//        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
//
//        marshaller.setClassesToBeBound(TableMessage.class);
//        marshaller.setCheckForXmlRootElement(true);
//        marshaller.createUnmarshaller();
//
//        MarshallingMessageConverter converter =
//                new MarshallingMessageConverter();
//
//        converter.setMarshaller(marshaller);
//        converter.setUnmarshaller(marshaller);
//        converter.setTargetType(MessageType.TEXT);
//
//        return converter;
//    }
//
//    @Bean
//    public IntegrationFlow jobFlow()
//    {
//        return IntegrationFlow.from("outputChannel")
//                .handle(jobLaunchingGateway)
//                .handle(logger())
//                .get();
//    }
//
//    @Bean
//    JobLaunchingGateway jobLaunchingGateway(JobLauncher jobLauncher)
//    {
//        return new JobLaunchingGateway(jobLauncher);
//    }
//
//    @Bean
//    LoggingHandler logger()
//    {
//        return new LoggingHandler("INFO");
//    }
//}
