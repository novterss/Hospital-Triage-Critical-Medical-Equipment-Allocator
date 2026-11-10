package hospital.contract;

import java.util.Comparator;

/**
 * Contract for items ordered in a priority queue (higher score = more urgent).
 */
public interface Prioritizable {

    int getPriorityScore();

    // Tie-breaker sequence when priority scores are equal (earlier arrival first)
    long getArrivalOrder();

    default boolean isMoreUrgentThan(Prioritizable other) {
        return getPriorityScore() > other.getPriorityScore();
    }

    // Highest score first, then earliest arrival
    static <T extends Prioritizable> Comparator<T> byUrgency() {
        return (a, b) -> {
            int byScore = Integer.compare(b.getPriorityScore(), a.getPriorityScore());
            return byScore != 0 ? byScore : Long.compare(a.getArrivalOrder(), b.getArrivalOrder());
        };
    }
}
