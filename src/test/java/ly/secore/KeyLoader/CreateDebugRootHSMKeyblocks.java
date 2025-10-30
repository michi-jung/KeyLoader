package ly.secore.KeyLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.HexFormat;
import ly.secore.compute.HardwareSecurityModule;

public class CreateDebugRootHSMKeyblocks {
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

        HardwareSecurityModule.RootHSMKeys rootHSMKeys = new HardwareSecurityModule.RootHSMKeys();

        try (HardwareSecurityModule keyLoader = new HardwareSecurityModule(
            "/usr/local/lib/libcryptok.so", 0))
        {
            // Generate and import the root HSM keyblocks into the HSM
            rootHSMKeys = keyLoader.createDebugRootHSMKeyblocks(rootHsmIncarnationKeyValue);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);;

            System.out.println(mapper.writerWithDefaultPrettyPrinter()
                                   .writeValueAsString(rootHSMKeys));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        System.exit(0);
    }
}
