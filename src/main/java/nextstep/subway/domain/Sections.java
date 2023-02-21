package nextstep.subway.domain;

import nextstep.subway.domain.exception.EntityAlreadyExistsException;
import nextstep.subway.domain.exception.EntityNotFoundException;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.*;
import java.util.stream.Collectors;

@Embeddable
public class Sections {
    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public void addSection(Section newSection) {
        if (sections.isEmpty()) {
            sections.add(newSection);
            return;
        }

        validateAddSection(newSection);

        if (isFirstEndSection(newSection) || isLastEndSection(newSection)) {
            sections.add(newSection);
            return;
        }

        addSectionBetweenExistSection(newSection);
    }

    private void addSectionBetweenExistSection(Section newSection) {
        if (!addDownStation(newSection)) {
            addUpStation(newSection);
        }
    }

    private void addUpStation(Section newSection) {
        Optional<Section> optOldSection = sections.stream()
                .filter(section -> section.getDownStation().equals(newSection.getDownStation()))
                .findFirst();

        if (optOldSection.isPresent()) {
            Section oldSection = optOldSection.get();
            validateDistance(oldSection.getUpStation(), newSection);
            sections.add(oldSection.addStation(newSection.getUpStation(), oldSection.getDistance() - newSection.getDistance()));
        }
    }

    private boolean addDownStation(Section newSection) {
        Optional<Section> optOldSection = sections.stream()
                .filter(section -> section.getUpStation().equals(newSection.getUpStation()))
                .findFirst();

        if (optOldSection.isPresent()) {
            Section oldSection = optOldSection.get();
            validateDistance(oldSection.getUpStation(), newSection);
            sections.add(oldSection.addStation(newSection.getDownStation(), oldSection.getDistance() - newSection.getDistance()));
            return true;
        }
        return false;
    }

    private void validateDistance(Station oldStation, Section newSection) {
        if (checkedDistance(oldStation, newSection)) {
            throw new IllegalArgumentException("The length of the section you want to register is greater than or equal to the length of the existing section");
        }
    }

    private boolean checkedDistance(Station upStation, Section newSection) {
        Section oldSection = getSectionByUpStation(upStation);
        return newSection.getDistance() >= oldSection.getDistance();
    }

    private Section getSectionByUpStation(Station upStation) {
        return sections.stream()
                .filter(section -> section.getUpStation().equals(upStation))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(upStation.getId(), "Station Id"));
    }

    private boolean isFirstEndSection(Section newSection) {
        return getFirstSection().isUpStation(newSection.getDownStation());
    }

    private boolean isLastEndSection(Section newSection) {
        return getLastSection().isDownStation(newSection.getUpStation());
    }

    private void validateAddSection(Section newSection) {
        if (isContainsStation(newSection.getUpStation()) && isContainsStation(newSection.getDownStation())) {
            throw new EntityAlreadyExistsException("Both stations already exist on the line.");
        }

        if (!isContainsStation(newSection.getUpStation()) && !isContainsStation(newSection.getDownStation())) {
            throw new EntityNotFoundException(String.format("Either station should be included."));
        }
    }

    public List<Section> getSections() {
        return sections;
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }

    public List<Station> getStations() {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Station> stations = new ArrayList<>();
        Station nowStation = getFirstStation();
        Station lastStation = getLastStation();
        stations.add(nowStation);

        while (!Objects.equals(nowStation, lastStation)) {
            Station finalNowStation = nowStation;
            Section section = sections.stream().filter(oldSection -> oldSection.getUpStation().equals(finalNowStation)).findFirst().orElseThrow(() -> new EntityNotFoundException("Section"));
            nowStation = section.getDownStation();
            stations.add(nowStation);
        }
        return stations;
    }

    private Station getFirstStation() {
        List<Station> downStations = sections.stream()
                .map(Section::getDownStation)
                .collect(Collectors.toList());

        return sections.stream()
                .map(Section::getUpStation)
                .filter(station -> !downStations.contains(station))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("UpStation"));
    }

    private Station getLastStation() {
        List<Station> upStations = sections.stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());

        return sections.stream()
                .map(Section::getDownStation)
                .filter(station -> !upStations.contains(station))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("DownStation"));
    }

    public void removeSection(Station station) {
        if (sections.size() < 2) {
            throw new IllegalArgumentException("If there is less than one registered section, you cannot delete it.");
        }

        if (!getLastSection().isDownStation(station)) {
            throw new IllegalArgumentException();
        }

        sections.remove(getLastSection());
    }

    public Section getLastSection() {
        return sections.get(sections.size() - 1);
    }

    private Section getFirstSection() {
        return sections.get(0);
    }

    private boolean isContainsStation(Station station) {
        List<Station> stations = getStations();
        return stations.contains(station);
    }

    public int getDistance() {
        return sections.stream()
                .mapToInt(Section::getDistance).sum();
    }
}