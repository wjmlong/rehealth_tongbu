import org.jeecg.common.util.PasswordUtil;

public class ComputeHash {
    public static void main(String[] args) throws Exception {
        String hash = PasswordUtil.encrypt("123456", "13507007984", "APDzGLuO");
        System.out.println("HASH=" + hash);
    }
}
