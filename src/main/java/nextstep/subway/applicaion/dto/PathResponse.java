package nextstep.subway.applicaion.dto;

import nextstep.subway.domain.Station;

import java.util.ArrayList;
import java.util.List;

public class PathResponse {
    private final List<Station> stations = new ArrayList<>();
    private final int distance;

    private PathResponse(List<Station> stations, int distance) {
        this.stations.addAll(stations);
        this.distance = distance;
    }

    public static PathResponse of(List<Station> stations, double pathWeight) {
        return new PathResponse(stations, Double.valueOf(pathWeight).intValue());
    }

    public List<Station> getStations() {
        return stations;
    }

    public int getDistance() {
        return distance;
    }
}