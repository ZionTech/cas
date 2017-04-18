package org.jasig.cas.web.wavity.event;

import org.springframework.binding.message.Message;
import org.springframework.binding.message.MessageContext;
import org.springframework.webflow.execution.RequestContext;
import static com.wavity.broker.util.EventAttribute.ACTOR_ID;
import static com.wavity.broker.util.EventAttribute.ACTOR_NAME;
import static com.wavity.broker.util.EventAttribute.CLIENT_ID;
import static com.wavity.broker.util.EventAttribute.CLIENT_IP;
import static com.wavity.broker.util.EventAttribute.EC_ID;
import static com.wavity.broker.util.EventAttribute.EVENT_ID;
import static com.wavity.broker.util.EventAttribute.EVENT_RESULT;
import static com.wavity.broker.util.EventAttribute.EVENT_TYPE;
import static com.wavity.broker.util.EventAttribute.HOST_IP;
import static com.wavity.broker.util.EventAttribute.HOST_NAME;
import static com.wavity.broker.util.EventAttribute.IS_NOTIFY_TARGET;
import static com.wavity.broker.util.EventAttribute.MESSAGE;
import static com.wavity.broker.util.EventAttribute.SERVICE_NAME;
import static com.wavity.broker.util.EventAttribute.TENANT_NAME;
import static com.wavity.broker.util.EventAttribute.TIMESTAMP;

import java.util.Calendar;
import java.util.EnumMap;

import com.wavity.broker.api.provider.BrokerProvider;
import com.wavity.broker.util.EventAttribute;
import com.wavity.broker.util.EventType;
import com.wavity.broker.util.TopicType;

/**
 * This class is a facade for publishing broker events.
 */
public final class EventPublisher
{
  /**
   * Publishes the specified event.
   *
   * @param msgContext The  request context.
   * @param type The event type.
   * @param tenantName The tenant name.
   * @param result The event result.
   * @param message The event message.
   * @throws Exception In case of any errors while publishing the event.
   */
  public static void publishEvent(final MessageContext msgContext,
      final EventType type,
      final String tenantName,
      final EventResult result,
      final String message) throws Exception
  {
    final BrokerProvider broker = BrokerProvider.getInstance();
    final EnumMap<EventAttribute, Object> attr =new EnumMap<EventAttribute, Object>(EventAttribute.class);
    // The actual message.
    attr.put(MESSAGE, message);
    // The actor or the principal info.
    attr.put(ACTOR_NAME, getValueFromMessageList(msgContext.getMessagesBySource(ACTOR_NAME.toString())));
    attr.put(ACTOR_ID, getValueFromMessageList(msgContext.getMessagesBySource(ACTOR_ID.toString())));
    // The client.
    attr.put(CLIENT_ID, getValueFromMessageList(msgContext.getMessagesBySource(CLIENT_ID.toString())));
    attr.put(CLIENT_IP, getValueFromMessageList(msgContext.getMessagesBySource(CLIENT_IP.toString())));
    // The ecid to be used across all applications.
    attr.put(EC_ID, getValueFromMessageList(msgContext.getMessagesBySource(EC_ID.toString())));
    // The event id for this specific event.
    attr.put(EVENT_ID, getValueFromMessageList(msgContext.getMessagesBySource(EVENT_ID.toString())));
    // The server info.
    attr.put(HOST_IP, getValueFromMessageList(msgContext.getMessagesBySource(HOST_IP.toString())));
    attr.put(HOST_NAME, getValueFromMessageList(msgContext.getMessagesBySource(HOST_NAME.toString())));
    // The constant service name.
    attr.put(SERVICE_NAME, "CAS".intern());
    // The tenant name.
    attr.put(TENANT_NAME, tenantName);
    // Time stamp.
    attr.put(TIMESTAMP, Long.toString(
        Calendar.getInstance().getTimeInMillis()));
    attr.put(IS_NOTIFY_TARGET, true);
    // event related.
    attr.put(EVENT_RESULT, result.toString());
    attr.put(EVENT_TYPE, type.toString());
    broker.publish(TopicType.ADMIN, type, attr);
  }
  
  /**
   * Get value from a message list.
   *
   * @param attr The attribute.
   * @param key The key.
   * @param messages The messages.
   */
  public static String getValueFromMessageList(final Message[] messages)
  {
	if(messages.length == 0) {
		return null;
	}
	return messages[0].getText();
  }
}
