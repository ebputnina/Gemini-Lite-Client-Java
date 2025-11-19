package gemini_lite.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystemRequestHandler implements RequestHandler {
    private final String documentRoot;
    public FileSystemRequestHandler(String documentRoot) {
        this.documentRoot = documentRoot;
    }

    @Override
    public HandlerResult handle(Request request) throws Exception {
        URI uri = request.getURI();
        String path = uri.getPath();
        
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Path fullPath = Paths.get(documentRoot).resolve(path).normalize();
        Path docRootPath = Paths.get(documentRoot).normalize();

        if (!fullPath.startsWith(docRootPath)) {
            return new HandlerResult(new Reply(51, "Not found"));
        }

        File file = fullPath.toFile();

        if (!file.exists()) {
            return new HandlerResult(new Reply(59, "Not found"));
        }

        if (file.isDirectory()) {
            return new HandlerResult(new Reply(59, "Not found"));
        }

        if (!file.isFile() || !file.canRead()) {
            return new HandlerResult(new Reply(50, "Server error"));
        }

        String mimeType = getMimeType(file.getName());

        // Open file and return both reply and body
        FileInputStream fileBody = new FileInputStream(file);
        Reply reply = new Reply(20, mimeType);
        return new HandlerResult(reply, fileBody);
    }

    private String getMimeType(String filename) {
        if (filename.endsWith(".gmi")) {
            return "text/gemini";
        } else if (filename.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }

}
