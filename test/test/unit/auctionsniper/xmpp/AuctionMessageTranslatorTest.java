package test.unit.auctionsniper.xmpp;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.endtoend.auctionsniper.ApplicationRunner;
import auctionsniper.AuctionEventListener;
import auctionsniper.AuctionEventListener.PriceSource;
import auctionsniper.xmpp.AuctionMessageTranslator;
import auctionsniper.xmpp.XMPPFailureReporter;

@RunWith(JMock.class)
public class AuctionMessageTranslatorTest {
	public static final Chat UNUSED_CHAT = null;
	private final Mockery context = new Mockery();
	private final AuctionEventListener listener = context.mock(AuctionEventListener.class);
	private final XMPPFailureReporter failureReporter = context.mock(XMPPFailureReporter.class);
	private final AuctionMessageTranslator translator =
			new AuctionMessageTranslator(ApplicationRunner.SNIPER_ID, listener, failureReporter);

	@Test public void
	notifiesAuctionClosedWhenCloseMessageReceived() {
		context.checking(new Expectations() {{
			oneOf(listener).auctionClosed();
		}});
		Message message = new Message();
		message.setBody("SOLVersion: 1.1; Event: CLOSE;");
		translator.processMessage(UNUSED_CHAT, message);
	}

	@Test public void
	notifiesBidDtailsWhenCurrentPriceMessageReceivedFromOtherBidder() {
		context.checking(new Expectations() {{
			exactly(1).of(listener).currentPrice(192, 7, PriceSource.FromOtherBidder);
		}});

		Message message = new Message();
		message.setBody("SOLVersion: 1.1; Event: PRICE; CurrentPrice: 192; Increment: 7; Bidder: Someone else;");
		translator.processMessage(UNUSED_CHAT, message);
	}

	@Test public void
	notifiesBidDtailsWhenCurrentPriceMessageReceivedFromSniper() {
		context.checking(new Expectations() {{
			exactly(1).of(listener).currentPrice(234, 5, PriceSource.FromSniper);
		}});

		Message message = new Message();
		message.setBody("SOLVersion: 1.1; Event: PRICE; CurrentPrice: 234; Increment: 5; Bidder: "+ ApplicationRunner.SNIPER_ID +";");
		translator.processMessage(UNUSED_CHAT, message);
	}

	@Test public void
	notifiesAuctionFailedWhenBadMessageReceived() {
		String badMessage = "a bad message";
		expectFailureWIthMessage(badMessage);
		translator.processMessage(UNUSED_CHAT, message(badMessage));
	}

	@Test public void
	notifiesAuctionFailedWhenEventTypeMissing() {
		String badMessage = "SOL Version: 1.1; CurrentPrice: 234; Increment: 5; Bidder: " + ApplicationRunner.SNIPER_ID + ";";
		expectFailureWIthMessage(badMessage);
		translator.processMessage(UNUSED_CHAT, message(badMessage));
	}

	private void expectFailureWIthMessage(final String badMessage) {
		context.checking(new Expectations() {{
			exactly(1).of(listener).auctionFailed();
			oneOf(failureReporter).cannotTranslateMessage(
					with(ApplicationRunner.SNIPER_ID), with(badMessage),
					with(any(Exception.class)));
		}});
	}

	private Message message(String messageBody) {
		Message message = new Message();
		message.setBody(messageBody);
		return message;
	}

}
