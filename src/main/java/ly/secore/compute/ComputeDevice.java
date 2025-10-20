package ly.secore.compute;

import java.io.InputStream;
import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

public class ComputeDevice implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger();

  protected interface CLibrary extends Library {
    CLibrary INSTANCE = (CLibrary)Native.load("c", CLibrary.class);

    int vsnprintf(byte[] buffer, int size, String format, Pointer va_list);
  }

  protected interface ComputeDeviceProxyLibrary extends Library {
    ComputeDeviceProxyLibrary INSTANCE =
        (ComputeDeviceProxyLibrary)Native.load("compute-device-proxy",
                                               ComputeDeviceProxyLibrary.class);

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

    int compute_device_open_service_session(Pointer compute_device);

    int compute_device_close_service_session(Pointer compute_device);

    int compute_device_request_deferred_reboot(Pointer compute_device);

    int compute_device_factory_flash(Pointer compute_device,
                                     compute_device_get_image_chunk_cb_t get_image_chunk);
  }

  protected Pointer compute_device;

  protected final ComputeDeviceProxyLibrary.compute_device_vlog_cb_t vlog_cb =
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

  public ComputeDevice(String host, String service) throws IOException {
    compute_device =
        ComputeDeviceProxyLibrary.INSTANCE.compute_device_proxy_tcp_new(
            host,
            service,
            vlog_cb,
            null);

    if (compute_device == null)
      {
        throw new IOException("compute_device_proxy_tcp_new() failed.");
      }
  }

  public ComputeDevice(String dev_tty_fn) throws IOException {
     ComputeDevice owner = this;

     ComputeDeviceProxyLibrary.compute_device_reset_cb_t reset_cb =
        new ComputeDeviceProxyLibrary.compute_device_reset_cb_t() {
          protected ComputeDevice self = owner;

          public int invoke(Pointer app_data) {
            return ComputeDeviceProxyLibrary.INSTANCE.compute_device_proxy_tty_reset_cb(
                       self.compute_device);
          }
        };

    compute_device =
        ComputeDeviceProxyLibrary.INSTANCE.compute_device_proxy_tty_new(
            dev_tty_fn,
            vlog_cb,
            reset_cb,
            null);

    if (compute_device == null)
      {
        throw new IOException("compute_device_proxy_tty_new() failed.");
      }
  }

  public int openServiceSession() {
    return ComputeDeviceProxyLibrary.INSTANCE.compute_device_open_service_session(compute_device);
  }

  public int closeServiceSession() {
    return ComputeDeviceProxyLibrary.INSTANCE.compute_device_close_service_session(compute_device);
  }

  public int requestDeferredReboot() {
    return ComputeDeviceProxyLibrary.INSTANCE.compute_device_request_deferred_reboot(
               compute_device);
  }

  public int factoryFlash(InputStream initialFirmwareImage) {
    ComputeDeviceProxyLibrary.compute_device_get_image_chunk_cb_t get_chunk_cb =
        new ComputeDeviceProxyLibrary.compute_device_get_image_chunk_cb_t() {
          protected InputStream image = initialFirmwareImage;

          public int invoke(Pointer app_data, Pointer buffer, int buffer_size) {
            int bytesRead = -1;

            try {
              byte[] buf = new byte[buffer_size];

              bytesRead = image.read(buf);

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
    
    return ComputeDeviceProxyLibrary.INSTANCE.compute_device_factory_flash(compute_device,
                                                                           get_chunk_cb);
  }

  public void close()
  {
    ComputeDeviceProxyLibrary.INSTANCE.compute_device_delete(compute_device);
  }
}
