package hospital.generic;

import hospital.contract.Prioritizable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/** Generic priority queue ordering Prioritizable items by urgency and arrival sequence. */
public class PriorityQueueManager<T extends Prioritizable> implements Iterable<T> {

    private final PriorityQueue<T> queue = new PriorityQueue<>(Prioritizable.<T>byUrgency());

    public void enqueue(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot queue null");
        }
        queue.add(item);
    }

    public void enqueueAll(Collection<? extends T> items) {
        for (T item : items) {
            enqueue(item);
        }
    }

    public T pollNext() {
        return queue.poll();
    }

    public T peekNext() {
        return queue.peek();
    }

    public boolean remove(T item) {
        return queue.remove(item);
    }

    public boolean contains(T item) {
        return queue.contains(item);
    }

    // PriorityQueue doesn't re-sort on mutation, so remove and re-add after re-triage.
    public void reprioritize(T item) {
        if (queue.remove(item)) {
            queue.add(item);
        }
    }

    public int drainTo(Collection<? super T> target) {
        int moved = 0;
        T next;
        while ((next = queue.poll()) != null) {
            target.add(next);
            moved++;
        }
        return moved;
    }

    // PriorityQueue's iterator/toArray isn't ordered, so copy and sort explicitly.
    public List<T> snapshot() {
        List<T> list = new ArrayList<>(queue);
        list.sort(Prioritizable.byUrgency());
        return list;
    }

    // 1-based position, or -1 if not queued.
    public int positionOf(T item) {
        int index = snapshot().indexOf(item);
        return index < 0 ? -1 : index + 1;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // Iterates over a sorted snapshot so order matches queue priority.
    @Override
    public Iterator<T> iterator() {
        return snapshot().iterator();
    }
}
