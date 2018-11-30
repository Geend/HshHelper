package extension;

public class StringHelper {
    public static boolean empty( final String s ) {
        // Null-safe, short-circuit evaluation.
        return s == null || s.trim().isEmpty();
    }
}
