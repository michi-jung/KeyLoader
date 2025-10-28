package ly.secore.KeyLoader;

import java.util.HexFormat;
import ly.secore.compute.HardwareSecurityModule;

public class KBPKImport {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java KBPKImport <kpbk-label> <kbpk-value>");
            System.exit(1);
        }

        byte[] kbpkValue = HexFormat.of().withDelimiter(":").parseHex(args[1]);

        if (kbpkValue.length != 32) {
            System.err.println("KBPK value must be 32 bytes long");
            System.exit(1);
        }

        try (HardwareSecurityModule keyLoader = new HardwareSecurityModule(
            "/usr/local/lib/libcryptok.so", 0))
        {
            // Import the KBPK value into the HSM
            keyLoader.createKeyBlockProtectionKey(args[0], kbpkValue);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
