package com.meridian.platform.bedrockadapter.gateway;

/** Request/reply to Bedrock. One implementation over MQ, one in-memory for laptops and tests. */
public interface BedrockGateway {

    /**
     * Sends a fixed width request and blocks for the matching reply. Throws
     * {@link BedrockUnavailableException} if the broker or Bedrock does not answer in time.
     */
    String call(String tranCode, String request);
}
