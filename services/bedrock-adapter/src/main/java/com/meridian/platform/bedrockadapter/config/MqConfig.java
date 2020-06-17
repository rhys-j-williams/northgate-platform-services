package com.meridian.platform.bedrockadapter.config;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.SimpleMessageConverter;

/**
 * Two connection factories, one JMS abstraction. Everything downstream (JmsTemplate, listener
 * containers, queue names) is identical; only the bean chosen by profile differs. The Artemis
 * factory exists because the IBM MQ developer image cannot be pulled on every build agent
 * (PLAT-2966), not because anyone plans to run Artemis in the bank.
 */
@Configuration
@EnableJms
public class MqConfig {

    @Bean
    @Profile("!local-artemis")
    public ConnectionFactory ibmMqConnectionFactory(
        @Value("${meridian.mq.host}") String host,
        @Value("${meridian.mq.port}") int port,
        @Value("${meridian.mq.queue-manager:QM1}") String queueManager,
        @Value("${meridian.mq.channel:DEV.APP.SVRCONN}") String channel,
        @Value("${meridian.mq.user:app}") String user,
        @Value("${meridian.mq.password:}") String password) throws JMSException {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(host);
        factory.setPort(port);
        factory.setQueueManager(queueManager);
        factory.setChannel(channel);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setStringProperty(WMQConstants.USERID, user);
        factory.setStringProperty(WMQConstants.PASSWORD, password);
        // Reconnect is handled by the listener container backoff, not the client. PLAT-3105.
        factory.setClientReconnectOptions(WMQConstants.WMQ_CLIENT_RECONNECT_DISABLED);
        return factory;
    }

    @Bean
    @Profile("local-artemis")
    public ConnectionFactory artemisConnectionFactory(
        @Value("${meridian.mq.artemis-url:tcp://localhost:61616}") String url,
        @Value("${meridian.mq.user:artemis}") String user,
        @Value("${meridian.mq.password:}") String password) {
        return new ActiveMQConnectionFactory(url, user, password);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory,
                                   @Value("${meridian.mq.receive-timeout-ms:5000}") long receiveTimeout) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setReceiveTimeout(receiveTimeout);
        template.setMessageConverter(new SimpleMessageConverter());
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("1-1");
        factory.setSessionTransacted(true);
        // Broker down at start up is a warning, not a failure. The container keeps retrying.
        factory.setRecoveryInterval(5000L);
        return factory;
    }
}
