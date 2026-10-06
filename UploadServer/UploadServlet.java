import java.io.*;
import java.util.Arrays;

public class UploadServlet extends HttpServlet {
   protected void doGet(HttpServletRequest request, HttpServletResponse response) {
      try {
         System.out.println("in doGet of original UploadServlet");
         OutputStream out = response.getOutputStream();

         // Serve the HTML file
         File htmlFile = new File("Form.html");
         FileInputStream fileInputStream = new FileInputStream(htmlFile);
         String content = "HTTP/1.1 200 OK\r\n" +
               "Content-Type: text/html\r\n" +
               "Content-Length: " + htmlFile.length() + "\r\n" +
               "\r\n";
         out.write(content.getBytes());
         byte[] buffer = new byte[4096];
         int bytesRead;
         while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
         }
         fileInputStream.close();
         out.close();

      } catch (Exception ex) {
         System.err.println(ex);
      }
   }

   protected void doPost(HttpServletRequest request, HttpServletResponse response) {
      try {

         InputStream in = request.getInputStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();

         String line;
         int contentLength = 0;
         String boundary = "";
         String caption = "";
         String date = "";
         String filename = "";

         // Read HTTP Request headers
         while ((line = readRequestLine(in)) != null && line.length() > 0) {

            String[] lineParts = line.split(" ");
            String header = lineParts[0];

            if (header.equals("Content-Type:")) {
               if (lineParts.length > 1 && lineParts[1].equals("multipart/form-data;")) {
                  String boundaryLine = lineParts[2];
                  boundary = boundaryLine.substring("boundary=".length());
               }
            }

            if (header.equals("Content-Length:")) {
               if (lineParts.length > 1) {
                  contentLength = Integer.parseInt(lineParts[1]);
               } else {
                  System.err.println("Content length missing from HTTP request");
               }
            }
         }

         // Read content before file
         boolean inFileContent = false;
         while (!inFileContent && (line = readRequestLine(in)) != null) {
            contentLength -= (line.length() + 2);

            String[] lineParts = line.split(" ");
            String header = lineParts[0];

            if (header.equals("Content-Disposition:")) {
               if (line.contains("filename=\"")) {
                  // File part
                  int start = line.indexOf("filename=\"") + "filename=\"".length();
                  filename = line.substring(start, line.lastIndexOf('"'));
                  line = readRequestLine(in);               // Content-Type line
                  contentLength -= (line.length() + 2);
                  line = readRequestLine(in);               // blank line
                  contentLength -= (line.length() + 2);
                  inFileContent = true;
               } else if (line.contains("name=\"")) {
                  // Plain text field
                  String field = line.substring(line.indexOf("name=\"") + 6, line.lastIndexOf('"'));
                  line = readRequestLine(in);               // blank line
                  contentLength -= (line.length() + 2);
                  line = readRequestLine(in);               // value
                  contentLength -= (line.length() + 2);

                  if (field.equals("caption")) {
                     caption = line;
                  } else if (field.equals("date")) {
                     date = line;
                  }
               }
            }
         }

         System.out.println("CAPTION: " + caption);
         System.out.println("DATE: " + date);
         System.out.println("FILENAME: " + filename);

         // Read file contents
         String finalBoundary = "--" + boundary + "--";
         int endLength = finalBoundary.length() + 4; // two lines of CRLF
         byte[] content = new byte[1];
         while (contentLength > endLength && in.read(content, 0, 1) != -1) {
            contentLength--;
            int clen = content.length;
            baos.write(content, 0, clen);
         }

         // Write file bytes to file on server
         System.out.println("writing to file...");
         new File("images").mkdirs();
         OutputStream outputStream = new FileOutputStream(
               new File("images/" + caption + "_" + date + "_" + filename));
         baos.writeTo(outputStream);
         outputStream.close();

         OutputStream out = response.getOutputStream();
         // Send HTTP response headers
         out.write("HTTP/1.1 200 OK\r\n".getBytes());
         out.write("Content-Type: text/html\r\n".getBytes());
         out.write("Connection: close\r\n".getBytes());
         int responseLength = 0;
         String topPart = "<!DOCTYPE html><html><body><ul>";
         String bottomPart = "</ul></body></html>";
         responseLength = topPart.length() + bottomPart.length();

         String middlePart = "";
         File dir = new File(".\\images");
         String[] chld = dir.list();
         Arrays.sort(chld);
         for (int i = 0; i < chld.length; i++) {
            String fileName = "<li>" + chld[i] + "</li>";
            middlePart += fileName;
         }
         responseLength += middlePart.length();
         out.write(("Content-Length: " + responseLength + "\r\n").getBytes());
         out.write("\r\n".getBytes());
         out.flush();

         out.write(topPart.getBytes());
         out.write(middlePart.getBytes());
         out.write(bottomPart.getBytes());
         out.flush();
         out.close();

      } catch (Exception ex) {
         System.err.println(ex);
      }
   }

   private String readRequestLine(InputStream is) {
      String s = "";
      boolean hitReturn = false;
      int c;

      try {
         while ((c = is.read()) != -1) {
            if (c == '\r') {
               hitReturn = true;
            } else if (hitReturn && c == '\n') {
               return s;
            } else {
               s += (char) c;
            }
         }
      } catch (Exception e) {
         return null;
      }
      return s.length() > 0 ? s : null;
   }
}