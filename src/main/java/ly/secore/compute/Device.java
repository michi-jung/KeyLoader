/*
 * ly.secore.compute.Device
 * Management of devices powered by compute secore.ly Firmware
 *
 * Copyright (c) 2025 secore.ly GmbH
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of secore.ly
 * GmbH ("Confidential Information").  You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with secore.ly GmbH or one of its
 * authorized partners.
 */

package ly.secore.compute;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.Structure;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Device implements AutoCloseable {

  @Structure.FieldOrder({
    "state",
    "timestamp",
    "tamper_status"
  })
  public static class LifecycleInfo extends Structure {
    public int state;
    public int timestamp;
    public int tamper_status;

    public static final int LIFECYCLE_STATE_UNKNOWN            = -2;
    public static final int LIFECYCLE_STATE_MANUFACTURED       = -1;
    public static final int LIFECYCLE_STATE_MANUFACTURING_TEST = 0;
    public static final int LIFECYCLE_STATE_PERSONALIZATION    = 1;
    public static final int LIFECYCLE_STATE_OPERATION          = 2;
    public static final int LIFECYCLE_STATE_FORENSIC_ANALYSIS  = 3;
    public static final int LIFECYCLE_STATE_DECOMMISSIONED     = 4;

    public LifecycleInfo() { super(); }

    public LifecycleInfo(int state) {
      super();
      this.state = state;
    }

    public LifecycleInfo(Pointer p) {
      super(p);
      read();
    }

    public static String getStateName(int state) {
      switch (state) {
        case LIFECYCLE_STATE_MANUFACTURED:
          return new String("Manufactured");
        case LIFECYCLE_STATE_MANUFACTURING_TEST:
          return new String("Manufacturing Test");
        case LIFECYCLE_STATE_PERSONALIZATION:
          return new String("Personalization");
        case LIFECYCLE_STATE_OPERATION:
          return new String("Operation");
        case LIFECYCLE_STATE_FORENSIC_ANALYSIS:
          return new String("Forensic Analysis");
        case LIFECYCLE_STATE_DECOMMISSIONED:
          return new String("Decommissioned");
        default:
          return new String("Unknown State");
      }
    }
  }

  @Structure.FieldOrder({
    "mfg_reset_secret"
  })
  public static class ManufacturingResetSecret extends Structure {
    public int[] mfg_reset_secret = new int[2];

    public ManufacturingResetSecret() { super(); }

    public ManufacturingResetSecret(Pointer p) {
      super(p);
      read();
    }

    public static class ByReference extends ManufacturingResetSecret implements Structure.ByReference {}
    public static class ByValue extends ManufacturingResetSecret implements Structure.ByValue {}
  }

  @Structure.FieldOrder({
    "device_class_uuid",
    "time_of_production",
    "serial_number",
    "device_type_id",
    "ecl",
    "mac_address"
  })
  public static class ManufacturingInfo extends Structure {
    public byte[] device_class_uuid = new byte[16];
    public int time_of_production;
    public int serial_number;
    public byte device_type_id;
    public byte ecl;
    public byte[] mac_address = new byte[6];

    public static final UUID DDM_885_DEVICE_CLASS = UUID.fromString("7b345c53-df4c-5655-b7d3-c78b7bca457f");

    public static final byte DEVICE_TYPE_DDM_885_2_020272_06A = 0x00;
    public static final byte DEVICE_TYPE_DDM_885_2_020296_06A = 0x01;
    public static final byte DEVICE_TYPE_DDM_885_2_020296_06B = 0x02;
    public static final byte DEVICE_TYPE_DDM_885_2_020326_06A = 0x03;
    public static final byte DEVICE_TYPE_DDM_885_2_020326_06B = 0x04;
    public static final byte DEVICE_TYPE_DDM_885_2_020326_06C = 0x05;
    public static final byte DEVICE_TYPE_DDM_885_2_020326_06D = 0x06;
    public static final byte DEVICE_TYPE_DDM_885_2_020363_06B = 0x10;

    public ManufacturingInfo() {}

    public ManufacturingInfo(Pointer p) {
      super(p);
      read();
    }

    public UUID getDeviceClassUUID() {
      ByteBuffer bb = ByteBuffer.wrap(device_class_uuid);
      long high = bb.getLong();
      long low = bb.getLong();
      return new UUID(high, low);
    }

    public String getDeviceClassName() {
      UUID deviceClass = getDeviceClassUUID();

      if (deviceClass.equals(DDM_885_DEVICE_CLASS))
      {
        return new String("DDM 885");
      }

      return new String("Unknown Device Class");
    }

    public void setDeviceClass(UUID deviceClass) {
      ByteBuffer bb = ByteBuffer.allocate(16);
      bb.putLong(deviceClass.getMostSignificantBits());
      bb.putLong(deviceClass.getLeastSignificantBits());
      device_class_uuid = bb.array();
    }

    public byte getDeviceType() {
      return device_type_id;
    }

    public void setDeviceType(byte deviceType) {
      device_type_id = deviceType;
    }

    public String getDeviceTypeName() {
      UUID deviceClass = getDeviceClassUUID();

      if (deviceClass.equals(DDM_885_DEVICE_CLASS)) {
        switch (device_type_id) {
          case DEVICE_TYPE_DDM_885_2_020272_06A:
            return new String("2-020272-06a");
          case DEVICE_TYPE_DDM_885_2_020296_06A:
            return new String("2-020296-06a");
          case DEVICE_TYPE_DDM_885_2_020296_06B:
            return new String("2-020296-06b");
          case DEVICE_TYPE_DDM_885_2_020326_06A:
            return new String("DDM 885-R Rev. A");
          case DEVICE_TYPE_DDM_885_2_020326_06B:
            return new String("DDM 885-R Rev. B");
          case DEVICE_TYPE_DDM_885_2_020326_06C:
            return new String("DDM 885-R Rev. C");
          case DEVICE_TYPE_DDM_885_2_020326_06D:
            return new String("DDM 885-R Rev. D");
          case DEVICE_TYPE_DDM_885_2_020363_06B:
            return new String("DDM 885-H Rev. B");
          default:
            return new String("Unknown Device Type");
        }
      }

      return new String("Unknown Device Type");
    }

    public byte getEngineeringChangeLevel() {
      return ecl;
    }

    public void setEngineeringChangeLevel(byte engineeringChangeLevel) {
      ecl = engineeringChangeLevel;
    }

    public int getSerialNumber() {
      return serial_number;
    }

    public void setSerialNumber(int serialNumber) {
      serial_number = serialNumber;
    }

    public byte[] getMACAddress() {
      return mac_address;
    }

    public void setMACAddress(byte[] macAddress) {
      mac_address = macAddress;
    }

    public ZonedDateTime getTimeOfProduction() {
      return Instant.ofEpochSecond(time_of_production).atZone(ZoneOffset.UTC);
    }

    public void setTimeOfProduction(ZonedDateTime timeOfProduction) {
      time_of_production = (int)timeOfProduction.toEpochSecond();
    }

    public static class ByReference extends ManufacturingInfo implements Structure.ByReference {}
    public static class ByValue extends ManufacturingInfo implements Structure.ByValue {}
  }

  public static final int DEVICE_PERSONALITY_PCI_POI = 0;
  public static final int DEVICE_PERSONALITY_PCI_HSM = 1;

  public static final int OPERATING_MODE_DEVELOPMENT = 0;
  public static final int OPERATING_MODE_PRODUCTION  = 1;

  @Structure.FieldOrder({
    "time_of_reincarnation",
    "device_personality",
    "operating_mode",
    "master_key_id"
  })
  public static class ReincarnationInfo extends Structure {
    public int time_of_reincarnation;
    public int device_personality;
    public int operating_mode;
    public int master_key_id;

    public ReincarnationInfo() {}

    public ReincarnationInfo(Pointer p) {
      super(p);
      read();
    }

    public ZonedDateTime getTimeOfReincarnation() {
      return Instant.ofEpochSecond(time_of_reincarnation).atZone(ZoneOffset.UTC);
    }

    public void setTimeOfReincarnation(ZonedDateTime timeOfReincarnation) {
      time_of_reincarnation = (int)timeOfReincarnation.toEpochSecond();
    }

    public int getDevicePersonality() {
      return device_personality;
    }

    public void setDevicePersonality(int devicePersonality) {
      device_personality = devicePersonality;
    }

    public static String getDevicePersonalityName(int devicePersonality) {
      switch (devicePersonality) {
        case DEVICE_PERSONALITY_PCI_POI:
          return new String("PCI POI");
        case DEVICE_PERSONALITY_PCI_HSM:
          return new String("PCI HSM");
        default:
          return new String("Unknown Device Personality");
      }
    }

    public int getOperatingMode() {
      return operating_mode;
    }

    public String getOperatingModeName() {
      switch (operating_mode) {
        case OPERATING_MODE_DEVELOPMENT:
          return new String("Development");
        case OPERATING_MODE_PRODUCTION:
          return new String("Production");
        default:
          return new String("Unknown Operating Mode");
      }
    }

    public void setOperatingMode(int operatingMode) {
      operating_mode = operatingMode;
    }

    public int getMasterKeyId() {
      if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
        return master_key_id;
      } else {
        return Integer.reverseBytes(master_key_id);
      }
    }

    public String getMasterKeyIdName() {
      return String.format("%08X", getMasterKeyId());
    }

    public void setMasterKeyId(int masterKeyId) {
      if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
        master_key_id = masterKeyId;
      } else {
        master_key_id = Integer.reverseBytes(masterKeyId);
      }
    }

    public static class ByReference extends ReincarnationInfo implements Structure.ByReference {}
    public static class ByValue extends ReincarnationInfo implements Structure.ByValue {}
  }

  public static class DDM885Info {
    public String productKey;
    public int orderId;
  }

  public Device(String host, String service) throws IOException {
    compute_device =
        ComputeDeviceProxyLibrary.INSTANCE.compute_device_proxy_tcp_new(
            host,
            service,
            vlog_cb,
            null);

    if (compute_device == null) {
        throw new IOException("compute_device_proxy_tcp_new() failed.");
    }
  }

  public Device(String dev_tty_fn) throws IOException {

    ComputeDeviceProxyLibrary.compute_device_reset_cb_t reset_cb =
      new ComputeDeviceProxyLibrary.compute_device_reset_cb_t() {
        public int invoke(Pointer app_data) {
          return ComputeDeviceProxyLibrary.INSTANCE
                    .compute_device_proxy_tty_reset_cb(compute_device);
        }
      };

    compute_device =
        ComputeDeviceProxyLibrary.INSTANCE.compute_device_proxy_tty_new(
            dev_tty_fn,
            vlog_cb,
            reset_cb,
            null);

    if (compute_device == null) {
      throw new IOException("compute_device_proxy_tty_new() failed.");
    }
  }

  public void openServiceSession(int timeout) throws IOException {
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
        .compute_device_open_service_session(compute_device, timeout);

    if (ret < 0) {
      throw new IOException("compute_device_open_service_session() failed.");
    }
  }

  public void closeServiceSession() throws IOException {
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE.compute_device_close_service_session(compute_device);

    if (ret < 0) {
      throw new IOException("compute_device_close_service_session() failed.");
    }
  }

  public void requestDeferredReboot() throws IOException {
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE.compute_device_request_deferred_reboot(
              compute_device);

    if (ret < 0) {
      throw new IOException("compute_device_request_deferred_reboot() failed.");
    }
  }

  public void factoryFlash(InputStream initialFirmwareImage) throws IOException {
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE.compute_device_factory_flash(
              compute_device,
              createGetImageChunkCallback(initialFirmwareImage));

    if (ret < 0) {
      throw new IOException("compute_device_factory_flash() failed.");
    }
  }

  public void applicationUpdate(InputStream applicationImage) throws IOException {
    upload(applicationImage, IMAGE_TYPE_APP0_UPDATE, null);
    requestDeferredReboot();
  }

  public int getMfgResetSecretDerivationInput() throws IOException {
    IntByReference derivationInput = new IntByReference();
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE.
              compute_device_get_mfg_reset_secret_derivation_input(compute_device, derivationInput);

    if (ret < 0)
      {
        throw new IOException("compute_device_get_mfg_reset_secret_derivation_input() failed.");
      }

    return derivationInput.getValue();
  }

  public void lock(ManufacturingResetSecret mfgReset) throws IOException {
    int ret = 0;

    mfgReset.write();
    ret = ComputeDeviceProxyLibrary.INSTANCE.compute_device_lock(compute_device, mfgReset);

    if (ret < 0) {
      throw new IOException("compute_device_lock() failed.");
    }
  }

  public void setAppKey(byte[] keyblock)
    throws IOException
  {
    int ret;

    upload(new ByteArrayInputStream(keyblock), IMAGE_TYPE_KEY_BLOCK, null);

    ret = ComputeDeviceProxyLibrary.INSTANCE.compute_device_set_app_key(compute_device);

    if (ret < 0) {
      throw new IOException("compute_device_set_app_key() failed.");
    }
  }

  public void setIncKeyStep1(KeyLoader.SetIncKeyContext ctx)
    throws IOException
  {
    Memory responderRandom = new Memory(KeyLoader.KEY_AGREEMENT_RANDOM_LEN);
    Memory responderEphPubKey = new Memory(KeyLoader.SECP256R1_PUBLIC_KEY_LEN);
    int ret = 0;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_set_inc_key_step_1(compute_device,
                                                 ctx.initiatorRandom,
                                                 ctx.initiatorAuthPubKey,
                                                 responderRandom,
                                                 responderEphPubKey);
    if (ret < 0) {
      throw new IOException("compute_device_set_inc_key_step_1() failed.");
    }

    ctx.responderRandom = responderRandom.getByteArray(0, (int)responderRandom.size());
    ctx.responderEphPubKey = responderEphPubKey.getByteArray(0, (int)responderEphPubKey.size());
  }

  public void setIncKeyStep2(KeyLoader.SetIncKeyContext ctx)
    throws IOException
  {
    Memory responderCMAC = new Memory(KeyLoader.AES_CMAC_LEN);
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_set_inc_key_step_2(compute_device,
                                                 ctx.initiatorEphPubKey,
                                                 ctx.initiatorSignature,
                                                 responderCMAC);
    if (ret < 0) {
      throw new IOException("compute_device_set_inc_key_step_2() failed.");
    }

    ctx.responderCMAC = responderCMAC.getByteArray(0, (int)responderCMAC.size());
  }

  public void setIncKeyStep3(KeyLoader.SetIncKeyContext ctx)
    throws IOException
  {
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_set_inc_key_step_3(compute_device,
                                                 ctx.initiatorCMAC,
                                                 ctx.initiatorKeyblock);
    if (ret < 0) {
      throw new IOException("compute_device_set_inc_key_step_3() failed.");
    }
  }

  public ManufacturingInfo getManufacturingInfo()
    throws IOException
  {
    ManufacturingInfo mfgInfo = new ManufacturingInfo();
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_get_manufacturing_info(compute_device, mfgInfo);

    if (ret < 0) {
      throw new IOException("compute_device_get_manufacturing_info() failed.");
    }

    mfgInfo.read();

    return mfgInfo;
  }

  public ReincarnationInfo getReincarnationInfo()
    throws IOException
  {
    ReincarnationInfo incInfo = new ReincarnationInfo();
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_get_reincarnation_info(compute_device, incInfo);

    if (ret < 0) {
      throw new IOException("compute_device_get_reincarnation_info() failed.");
    }

    incInfo.read();

    return incInfo;
  }

  public DDM885Info getDDM885Info()
    throws IOException
  {
    DDM885Info ddm885Info = new DDM885Info();
    Memory productKey = new Memory(ComputeDeviceProxyLibrary.DDM_885_PRODUCT_KEY_LEN);
    IntByReference orderId = new IntByReference();
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_get_885_info(compute_device, productKey, orderId);

    if (ret < 0) {
      throw new IOException("compute_device_get_885_info() failed.");
    }

    ddm885Info.productKey = productKey.getString(0);
    ddm885Info.orderId = orderId.getValue();

    return ddm885Info;
  }

  public LifecycleInfo getLifecycleInfo()
    throws IOException
  {
    LifecycleInfo lifecycleInfo = new LifecycleInfo();
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_get_lifecycle_info(compute_device, lifecycleInfo);

    if (ret < 0) {
      throw new IOException("compute_device_get_lifecycle_info() failed.");
    }

    lifecycleInfo.read();

    return lifecycleInfo;
  }

  public byte[] getReincarnationKeyDerivationInfo()
    throws IOException
  {
    ManufacturingInfo mfgInfo = getManufacturingInfo();
    ReincarnationInfo incInfo = getReincarnationInfo();
    byte[] derivationInfo = new byte[mfgInfo.size() + incInfo.size()];

    System.arraycopy(mfgInfo.getPointer().getByteArray(0, mfgInfo.size()),
                     0,
                     derivationInfo,
                     0,
                     mfgInfo.size());
    System.arraycopy(incInfo.getPointer().getByteArray(0, incInfo.size()),
                     0,
                     derivationInfo,
                     mfgInfo.size(),
                     incInfo.size());

    return derivationInfo;
  }

  public void close()
  {
    if (compute_device != null) {
      ComputeDeviceProxyLibrary.INSTANCE.compute_device_delete(compute_device);
      compute_device = null;
    }
  }

  private static final Logger LOGGER = LogManager.getLogger();

  private interface CLibrary extends Library {
    CLibrary INSTANCE = (CLibrary)Native.load("c", CLibrary.class);

    int vsnprintf(byte[] buffer, int size, String format, Pointer va_list);
  }

  private interface ComputeDeviceProxyLibrary extends Library {
    ComputeDeviceProxyLibrary INSTANCE =
        (ComputeDeviceProxyLibrary)Native.load("compute-device-proxy",
                                               ComputeDeviceProxyLibrary.class);

    static final int DDM_885_PRODUCT_KEY_LEN = 18;

    interface compute_device_reset_cb_t extends Callback {
      int invoke(Pointer app_data);
    }

    interface compute_device_vlog_cb_t extends Callback {
      void invoke(Pointer app_data, int priority, String fmt, Pointer va_args);
    }

    interface compute_device_get_image_chunk_cb_t extends Callback {
      int invoke(Pointer app_data, Pointer buffer, int buffer_size);
    }

    Pointer compute_device_proxy_tcp_new(String  host,
                                         String  service,
                                         compute_device_vlog_cb_t vlog,
                                         Pointer app_data);

    Pointer compute_device_proxy_tty_new(String                    dev_tty_fn,
                                         compute_device_vlog_cb_t  vlog,
                                         compute_device_reset_cb_t reset,
                                         Pointer                   app_data);

    void compute_device_delete(Pointer compute_device);

    int compute_device_proxy_tty_reset_cb(Pointer compute_device);

    int compute_device_open_service_session(Pointer compute_device, int timeout);

    int compute_device_close_service_session(Pointer compute_device);

    int compute_device_request_deferred_reboot(Pointer compute_device);

    int compute_device_factory_flash(Pointer                             compute_device,
                                     compute_device_get_image_chunk_cb_t get_image_chunk);

    int compute_device_get_mfg_reset_secret_derivation_input(
            Pointer         compute_device,
            IntByReference  mfg_reset_secret_derivation_input);

    int compute_device_lock(Pointer                  compute_device,
                            ManufacturingResetSecret mfg_reset);

    int compute_device_set_inc_key_step_1(
            Pointer compute_device,
            byte[] initiator_random,
            byte[] initiator_auth_pub_key,
            Memory responder_random,
            Memory responder_eph_pub_key);

    int compute_device_set_inc_key_step_2(
            Pointer compute_device,
            byte[] initiator_eph_pub_key,
            byte[] initiator_signature,
            Memory responder_cmac);

    int compute_device_set_inc_key_step_3(
            Pointer compute_device,
            byte[] initiator_cmac,
            byte[] initiator_keyblock);

    int compute_device_get_manufacturing_info(
            Pointer           compute_device,
            ManufacturingInfo mfg_info);

    int compute_device_get_reincarnation_info(
            Pointer           compute_device,
            ReincarnationInfo inc_info);

    int compute_device_start_upload(
            Pointer compute_device,
            int     image_type,
            String  name);

    int compute_device_upload_chunk(
            Pointer compute_device,
            byte[]  chunk,
            int     len);

    int compute_device_finalize_upload(
            Pointer compute_device);

    int compute_device_set_app_key(
            Pointer compute_device);

    int compute_device_get_885_info(
            Pointer        compute_device,
            Memory         product_key,
            IntByReference order_id);

    int compute_device_get_lifecycle_info(
            Pointer       compute_device,
            LifecycleInfo lifecycle_info);
  }

  private Pointer compute_device;

  private ComputeDeviceProxyLibrary.compute_device_get_image_chunk_cb_t
              createGetImageChunkCallback(InputStream image)
  {
    return new ComputeDeviceProxyLibrary.compute_device_get_image_chunk_cb_t()
    {
      public int invoke(Pointer app_data, Pointer buffer, int buffer_size) {
        int bytesRead = -1;
        try {
          byte[] buf = new byte[buffer_size];

          bytesRead = image.read(buf);

          System.out.printf("Read %d bytes from image stream\n", bytesRead);

          if (bytesRead > 0)
          {
            buffer.getByteBuffer(0, bytesRead).put(buf, 0, bytesRead);
          }

          if (bytesRead == -1)
          {
            bytesRead = 0;
          }
        }
        catch (IOException e) {
          bytesRead = -1;
        }

        return bytesRead;
      }
    };
  }

  private static final int MAX_IMAGE_CHUNK_SIZE   = 192;
  /* private static final int IMAGE_TYPE_FW_UPDATE   = 1;*/
  private static final int IMAGE_TYPE_APP0_UPDATE = 2;
  /* private static final int IMAGE_TYPE_DEVCFG      = 3;
   * private static final int IMAGE_TYPE_LOGS        = 4;
   * private static final int IMAGE_TYPE_FFDC        = 5; */
  private static final int IMAGE_TYPE_KEY_BLOCK   = 6;
  /* private static final int IMAGE_TYPE_SHARED_FILE = 7;
   * private static final int IMAGE_TYPE_FILE_LIST   = 8; */

  private void upload(InputStream data, int imageType, String name)
    throws IOException
  {
    int ret;

    ret = ComputeDeviceProxyLibrary.INSTANCE
              .compute_device_start_upload(compute_device, imageType, name);

    if (ret != 0) {
      throw new IOException("compute_device_start_upload() failed.");
    }

    try {
      byte[] chunk = new byte[MAX_IMAGE_CHUNK_SIZE];
      int size;

      while (true)
        {
          size = data.read(chunk);

          if (size <= 0)
          {
            break;
          }

          ret = ComputeDeviceProxyLibrary.INSTANCE
                    .compute_device_upload_chunk(compute_device, chunk, size);

          if (ret != 0) {
            throw new IOException("compute_device_upload_chunk() failed.");
          }
        }
    }
    finally {
      ret = ComputeDeviceProxyLibrary.INSTANCE
                .compute_device_finalize_upload(compute_device);

      if (ret != 0) {
        throw new IOException("compute_device_finalize_upload() failed.");
      }
    }
  }

  private final ComputeDeviceProxyLibrary.compute_device_vlog_cb_t vlog_cb =
      new ComputeDeviceProxyLibrary.compute_device_vlog_cb_t() {
        public void invoke(Pointer app_data, int priority, String fmt, Pointer va_args) {
          byte[] buffer = new byte[2048];
          Level level;

          switch (priority) {
            case 0:
            case 1:
            case 2:
              level = Level.FATAL;
              break;
            case 3:
              level = Level.ERROR;
              break;
            case 4:
              level = Level.WARN;
              break;
            case 5:
            case 6:
              level = Level.INFO;
              break;
            case 7:
            default:
              level = Level.DEBUG;
              break;
          }

          CLibrary.INSTANCE.vsnprintf(buffer, buffer.length, fmt, va_args);

          LOGGER.atLevel(level).log(new String(buffer));
        }
      };
}
