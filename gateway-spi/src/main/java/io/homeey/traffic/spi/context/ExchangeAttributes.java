package io.homeey.traffic.spi.context;

public final class ExchangeAttributes {

    public static final String REQUEST_PATH = "request.path";
    public static final String REQUEST_METHOD = "request.method";
    public static final String REQUEST_HEADERS = "request.headers";
    public static final String REQUEST_BODY = "request.body";

    public static final String RESPONSE_STATUS = "response.status";
    public static final String RESPONSE_HEADERS = "response.headers";
    public static final String RESPONSE_BODY = "response.body";

    private ExchangeAttributes() {
    }
}
