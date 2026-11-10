package hospital.device;

import hospital.exception.InvalidVitalSignsException;
import hospital.model.VitalSigns;

/**
 * Common interface for reading vital signs from bedside monitors.
 */
public interface VitalSignsSource {

    VitalSigns readVitals() throws InvalidVitalSignsException;

    String getSourceName();
}
