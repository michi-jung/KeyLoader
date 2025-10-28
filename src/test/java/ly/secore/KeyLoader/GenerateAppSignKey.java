package ly.secore.KeyLoader;

import ly.secore.compute.HardwareSecurityModule;

public class GenerateAppSignKey {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java GenerateAppSignKey <app-sign-key-label>");
            System.exit(1);
        }

        String appSignKeyLabel = args[0];

        try (HardwareSecurityModule keyLoader = new HardwareSecurityModule(
            "/usr/local/lib/libcryptok.so", 0))
        {
            // Generate and import the application signing key into the HSM
            keyLoader.createSigningKeyPair(appSignKeyLabel);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}