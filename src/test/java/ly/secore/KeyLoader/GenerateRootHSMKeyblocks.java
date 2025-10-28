package ly.secore.KeyLoader;

import java.util.HexFormat;
import java.util.List;

import ly.secore.compute.HardwareSecurityModule;

public class GenerateRootHSMKeyblocks {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java GenerateRootHSMKeyblocks <root-hsm-incarnation-key-value>");
            System.exit(1);
        }

        byte[] rootHsmIncarnationKeyValue = HexFormat.of().withDelimiter(":").parseHex(args[0]);

        if (rootHsmIncarnationKeyValue.length != 32) {
            System.err.println("Root HSM Incarnation key value must be 32 bytes long");
            System.exit(1);
        }

        List<String> rootHsmKeyblocks = new java.util.ArrayList<>();

        try (HardwareSecurityModule keyLoader = new HardwareSecurityModule(
            "/usr/local/lib/libcryptok.so", 0))
        {
            // Generate and import the root HSM keyblocks into the HSM
            rootHsmKeyblocks = keyLoader.generateRootHSMKeyblocks(rootHsmIncarnationKeyValue);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        for (String keyblock : rootHsmKeyblocks) {
            System.out.println(keyblock);
        }
        System.exit(0);
    }
}
