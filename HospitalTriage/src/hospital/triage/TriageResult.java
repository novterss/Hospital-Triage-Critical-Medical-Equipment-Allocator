package hospital.triage;

import hospital.equipment.EquipmentType;
import hospital.model.TriageLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable snapshot of a patient's triage assessment.
 */
public final class TriageResult {

    private final TriageLevel level;
    private final List<String> reasons;
    private final Set<EquipmentType> recommendedEquipment;
    private final String protocolName;

    public TriageResult(TriageLevel level, Collection<? extends String> reasons,
                        Collection<? extends EquipmentType> recommendedEquipment, String protocolName) {
        this.level = level;
        this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
        Set<EquipmentType> copy = EnumSet.noneOf(EquipmentType.class);
        copy.addAll(recommendedEquipment);
        this.recommendedEquipment = Collections.unmodifiableSet(copy);
        this.protocolName = protocolName;
    }

    public TriageLevel getLevel() {
        return level;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public Set<EquipmentType> getRecommendedEquipment() {
        return recommendedEquipment;
    }

    public String getProtocolName() {
        return protocolName;
    }

    @Override
    public String toString() {
        return level + " by " + protocolName + " " + reasons;
    }
}
