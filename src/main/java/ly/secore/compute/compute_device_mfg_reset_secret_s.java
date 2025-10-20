package ly.secore.compute;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

@Structure.FieldOrder({ "mfg_reset_secret" })
public class compute_device_mfg_reset_secret_s extends Structure {

  /** Corresponds to uint32_t mfg_reset_secret[2]; */
  public int[] mfg_reset_secret = new int[2];

  public compute_device_mfg_reset_secret_s() { super(); }

  public compute_device_mfg_reset_secret_s(Pointer p) {
      super(p);
      read();
  }

  /** By-reference and by-value nested types for pointer passing */
  public static class ByReference extends compute_device_mfg_reset_secret_s implements Structure.ByReference {}
  public static class ByValue extends compute_device_mfg_reset_secret_s implements Structure.ByValue {}
}
