package ly.secore.KeyLoader;

import ly.secore.compute.HardwareSecurityModule;
import java.util.HexFormat;
import com.mythosil.sss4j.Sss4j;

public class MEKKeyShareGenerator {
    public static void main(String[] args) {
        try (HardwareSecurityModule keyLoader = new HardwareSecurityModule("/usr/local/lib/libcryptok.so", 0)) {
            // Generate random master encryption key shares
            var shares = keyLoader.createRandomMasterEncryptionKeyShares();

            // Print the generated shares
            for (var share : shares) {
                System.out.println("Share Value: " + HexFormat.ofDelimiter(":").withUpperCase().formatHex(share.getValue()));
            }

            shares.remove(0);

            System.out.println("Combined: " +
                               HexFormat.ofDelimiter(":").withUpperCase().formatHex(Sss4j.combine(shares)));
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
