package com.redhat.customer.translate;

import java.io.FileInputStream;
import java.net.URI;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelContextXmlWithAdviceTest extends CamelSpringTestSupport {

	@Produce(uri = "active-mq:q.empi.deim.in")
	protected ProducerTemplate inputEndpoint;

	@EndpointInject(uri = "mock:deim-queue")
	protected MockEndpoint resultEndpoint;

	BrokerService broker = null;

	@Before
	public void initialize() throws Exception {
		broker = new BrokerService();
		TransportConnector connector = new TransportConnector();
		connector.setUri(new URI("tcp://localhost:61617"));
		broker.addConnector(connector);
		broker.start();
		broker.waitUntilStarted();
		KeyValueHolder serviceHolder = new KeyValueHolder(new ActiveMQComponent(), null);
	}

	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext(new String[] { "bundleTestContext.xml", "camelTestContext.xml" });
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				try {
					context.getRouteDefinition("translate").adviceWith(context, new AdviceWithRouteBuilder() {

						@Override
						public void configure() throws Exception {
							weaveById("outbound-queue").replace().to("mock:deim-queue");

						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};
	}

	@Test
	public void testCamelRoute() throws Exception {
		String expectedResult = readFile("src/test/data/NextGateRequest.xml");
		String input = readFile("src/test/data/Person.xml");

		resultEndpoint.expectedBodiesReceived(expectedResult);
		resultEndpoint.expectedMessageCount(1);

		inputEndpoint.sendBody(input);

		resultEndpoint.assertIsSatisfied();
	}

	private String readFile(String filePath) throws Exception {
		String content;
		FileInputStream fis = new FileInputStream(filePath);
		try {
			content = createCamelContext().getTypeConverter().convertTo(String.class, fis);
		} finally {
			fis.close();
		}
		return content;
	}

}
