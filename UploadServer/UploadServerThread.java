import java.net.*;
import java.io.*;

public class UploadServerThread extends Thread {
   private Socket socket = null;

   public UploadServerThread(Socket socket) {
      super("DirServerThread");
      this.socket = socket;
   }

   public void run() {
      System.out.println("Thread running");
      try {
         // Use BufferedInputStream to read the request
         BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
         in.mark(0);
         BufferedReader reader = new BufferedReader(new InputStreamReader(in));

         // Read the first line of the request
         String requestLine = reader.readLine();
         if (requestLine != null) {
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String uri = requestParts[1];

            in.reset();

            HttpServletRequest req = new HttpServletRequest(in);
            OutputStream baos = new ByteArrayOutputStream();
            HttpServletResponse res = new HttpServletResponse(baos);

           //Using Reflection
           //Class<?> servletClass;
           //servletClass = Class.forName("UploadServlet");
           //HttpServlet httpServlet = (HttpServlet) servletClass.getDeclaredConstructor().newInstance();
           
           // Using conventional new method
            HttpServlet httpServlet = new UploadServlet();

            // // Check the HTTP Request Type
            if ("GET".equalsIgnoreCase(method) && "/".equals(uri)) {
               System.out.println("Calling UploadServlet's doGet");
               httpServlet.doGet(req, res);
            } else if ("POST".equalsIgnoreCase(method)) {
               System.out.println("Calling UploadServlet's doPost");
               httpServlet.doPost(req, res);
            } else {
               // Handle unsupported methods
               res.getOutputStream().write("HTTP/1.1 405 Method Not Allowed\r\n\r\n".getBytes());
            }


            // Send response back to client
            OutputStream out = socket.getOutputStream();
            out.write(((ByteArrayOutputStream) baos).toByteArray());
            out.flush();
         }

         socket.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
