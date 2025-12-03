package gemini_lite.protocol;

/**
 * Anything that wants to handle a request must implement handle and return a HandlerResult.
 */
public interface RequestHandler {
    HandlerResult handle(Request request) throws Exception;
}
// server uses this: decoupled from specific handlers; more classes could use it.. (˵•̀ᴗ-˵) ✧
