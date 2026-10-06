public class Activity {
   public static void main(String[] args) {
      new Activity().onCreate();
   }
   public void onCreate() {
      UploadClient client = new UploadClient();
      String response = client.uploadFile();
      System.out.println(response);
   }
}