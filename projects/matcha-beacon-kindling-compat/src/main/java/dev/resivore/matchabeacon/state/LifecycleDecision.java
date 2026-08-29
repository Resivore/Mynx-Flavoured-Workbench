package dev.resivore.matchabeacon.state;

/** Action requested from the runtime coordinator after one pure lifecycle step. */
public enum LifecycleDecision {
    /** Player or beacon location cannot currently be processed; consume no active time. */
    PAUSE,
    /** One valid active tick was consumed without crossing a lifecycle boundary. */
    ADVANCE,
    /** The approach already emitted ARRIVE and is waiting for explicit beginVisit. */
    WAITING_FOR_VISIT,
    /** Spawn exactly one trader, then call beginVisit with that trader's UUID. */
    ARRIVE,
    /** Invalid beacon/marker/trader state requires exact-owner cleanup. */
    CANCEL,
    /** The exact five-minute active visit completed and requires departure cleanup. */
    DEPART
}
