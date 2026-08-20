import com.squareup.moshi.Moshi;
import com.squareup.moshi.JsonAdapter;

public class TestMoshi {
    public static class TestBook {
        public String id;
        public String title;
    }
    public static void main(String[] args) throws Exception {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<TestBook> adapter = moshi.adapter(TestBook.class);
        TestBook b = adapter.fromJson("{\"id\": 123, \"title\": \"Hello\"}");
        System.out.println("Success: " + b.id + " " + b.title);
    }
}
