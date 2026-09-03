import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GenBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("123456");
        System.out.println("Hash: " + hash);
        System.out.println("Matches 123456: " + encoder.matches("123456", hash));
    }
}
