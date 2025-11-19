package gemini_lite.protocol;

public interface RequestHandler {
    HandlerResult handle(Request request) throws Exception;
}
