package hospital.contract;

/** Contract for equipment requiring periodic maintenance based on usage hours. */
public interface Maintainable {

    double DEFAULT_SERVICE_INTERVAL_HOURS = 100.0;

    double getUsageHours();

    boolean isUnderMaintenance();

    void startMaintenance();

    void completeMaintenance();

    // Subtypes may override for shorter or longer service intervals
    default double getServiceIntervalHours() {
        return DEFAULT_SERVICE_INTERVAL_HOURS;
    }

    default boolean needsService() {
        return getUsageHours() >= getServiceIntervalHours();
    }

    default String statusLabel() {
        if (isUnderMaintenance()) {
            return "Under maintenance";
        }
        return needsService() ? "Service due" : "OK";
    }
}
