package hospital.generic;

import hospital.equipment.EquipmentType;
import hospital.equipment.MedicalEquipment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic equipment inventory supporting predicate queries and filtered iteration.
 */
public class Inventory<T extends MedicalEquipment> implements Iterable<T> {

    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add null equipment");
        }
        items.add(item);
    }

    public void addAll(Collection<? extends T> newItems) {
        for (T item : newItems) {
            add(item);
        }
    }

    public boolean remove(T item) {
        return items.remove(item);
    }

    public int size() {
        return items.size();
    }

    public List<T> getAll() {
        return Collections.unmodifiableList(items);
    }

    public List<T> findAll(Predicate<? super T> condition) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (condition.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public Optional<T> findFirst(Predicate<? super T> condition) {
        for (T item : items) {
            if (condition.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public Optional<T> findById(String id) {
        return findFirst(e -> e.getId().equalsIgnoreCase(id));
    }

    public int count(Predicate<? super T> condition) {
        return findAll(condition).size();
    }

    public <R> List<R> map(Function<? super T, ? extends R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : items) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    @Override
    public Iterator<T> iterator() {
        return new InventoryIterator();
    }

    // Returns an iterator restricted to the specified equipment type
    public Iterator<T> iteratorOf(EquipmentType type) {
        class FilteredIterator implements Iterator<T> {
            private final Iterator<T> all = items.iterator();
            private T nextMatch = advance();

            private T advance() {
                while (all.hasNext()) {
                    T candidate = all.next();
                    if (candidate.getType() == type) {
                        return candidate;
                    }
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return nextMatch != null;
            }

            @Override
            public T next() {
                if (nextMatch == null) {
                    throw new NoSuchElementException();
                }
                T current = nextMatch;
                nextMatch = advance();
                return current;
            }
        }
        return new FilteredIterator();
    }

    private class InventoryIterator implements Iterator<T> {
        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor < Inventory.this.items.size();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return Inventory.this.items.get(cursor++);
        }
    }
}
