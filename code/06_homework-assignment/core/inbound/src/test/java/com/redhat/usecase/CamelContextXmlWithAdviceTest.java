package com.redhat.usecase;

import java.io.FileInputStream;

import org.apache.activemq.broker.BrokerService;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelContextXmlWithAdviceTest extends CamelSpringTestSupport {

	@Produce(uri = "direct:integrateRoute")
	protected ProducerTemplate inputEndpoint;

	@EndpointInject(uri = "mock:deim-queue")
	protected MockEndpoint resultEndpoint;

	BrokerService broker = null;

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
					context.getRouteDefinition("matchRoute").adviceWith(context, new AdviceWithRouteBuilder() {

						@Override
						public void configure() throws Exception {
							weaveById("inbound-queue").replace().to("mock:deim-queue");

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
		String expectedResult = readFile("src/test/data/Person.xml");
		String input = readFile("src/test/data/Person.xml");

		resultEndpoint.expectedBodiesReceived(expectedResult);
		resultEndpoint.expectedMessageCount(1);

		inputEndpoint.sendBodyAndHeader(input, "method", "match");

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
