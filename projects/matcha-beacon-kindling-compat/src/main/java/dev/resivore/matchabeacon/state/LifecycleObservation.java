package dev.resivore.matchabeacon.state;

/**
 * One server-tick observation supplied by the runtime coordinator.
 *
 * @param playerOnline true only for the exact summoning player
 * @param locationProcessable true only when the exact dimension/chunk can be meaningfully checked
 * @param campfireValid true only when the exact beacon block is still the required lit campfire
 * @param exactMarkerPresent true only when the record's marker UUID is present at the recorded beacon
 * @param exactTraderPresent true only when the record's trader UUID is present during a visit
 */
public record LifecycleObservation(
        boolean playerOnline,
        boolean locationProcessable,
        boolean campfireValid,
        boolean exactMarkerPresent,
        boolean exactTraderPresent
) {
    public static LifecycleObservation activeApproach() {
        return new LifecycleObservation(true, true, true, true, false);
    }

    public static LifecycleObservation activeVisit() {
        return new LifecycleObservation(true, true, true, true, true);
    }
}
