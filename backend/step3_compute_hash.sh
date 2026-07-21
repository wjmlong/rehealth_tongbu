#!/bin/bash
set -e

echo "=== Method 1: Try running PasswordUtil via java -cp ==="
docker exec rehealth-staging-backend-1 sh -c 'java -cp /opt/rehealth/app.jar -Dloader.main=org.springframework.boot.loader.PropertiesLauncher org.springframework.boot.loader.launch.PropertiesLauncher --help 2>&1 | head -3' 2>&1

echo ""
echo "=== Method 2: Try using spring-boot-loader to run a command ==="
# Write a tiny Java file, compile it with the fat jar on classpath
cat > /tmp/ComputeHash.java << 'JAVA_EOF'
import org.jeecg.common.util.PasswordUtil;
public class ComputeHash {
    public static void main(String[] args) throws Exception {
        String hash = PasswordUtil.encrypt("123456", "13507007984", "APDzGLuO");
        System.out.println("HASH=" + hash);
    }
}
JAVA_EOF

# Copy into container, compile, and run
docker cp /tmp/ComputeHash.java rehealth-staging-backend-1:/tmp/ComputeHash.java 2>&1
docker exec rehealth-staging-backend-1 sh -c 'javac -cp /opt/rehealth/app.jar /tmp/ComputeHash.java 2>&1' 2>&1
echo "=== Compile done, running ==="
docker exec rehealth-staging-backend-1 sh -c 'java -cp /opt/rehealth/app.jar:/tmp ComputeHash 2>&1' 2>&1
