package com.krishiconnect.service;

/** Published inside the transaction when a quote is sent, accepted or turned down. */
public record QuoteEvent(Long quoteId, Type type) {

    public enum Type {
        RECEIVED,
        ACCEPTED,
        REJECTED
    }
}
