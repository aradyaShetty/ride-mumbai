package com.ridemumbai.util;

import com.ridemumbai.model.Route;
import com.ridemumbai.model.Station;
import com.ridemumbai.repository.RouteRepository;
import com.ridemumbai.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component // Marks this as a Spring bean to be managed
@RequiredArgsConstructor // Injects dependencies via constructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking if mock data needs to be initialized...");

        // Only add data if the stations table is empty to avoid duplicates on restarts
        if (stationRepository.count() == 0) {
            log.info("No existing station data found. Initializing mock stations and routes...");

            // --- Create Mock Stations ---
            Station andheri = Station.builder().name("Andheri").build();
            Station weh = Station.builder().name("WEH").build(); // Western Express Highway
            Station chakala = Station.builder().name("Chakala").build();
            Station airport = Station.builder().name("Airport Road").build();
            Station marol = Station.builder().name("Marol Naka").build();
            Station saki = Station.builder().name("Saki Naka").build();
            Station asalpha = Station.builder().name("Asalpha").build();
            Station jagruti = Station.builder().name("Jagruti Nagar").build();
            Station ghatkopar = Station.builder().name("Ghatkopar").build();

            // Save stations to the database
            List<Station> stations = List.of(andheri, weh, chakala, airport, marol, saki, asalpha, jagruti, ghatkopar);
            stationRepository.saveAll(stations);
            log.info("Saved {} mock stations.", stations.size());

            // --- Create Mock Routes (Direct connections for Line 1) ---
            // Simulating connections in both directions with estimated distances
            Route r1 = Route.builder().startStationName("Andheri").endStationName("WEH").distance(1.5).build();
            Route r2 = Route.builder().startStationName("WEH").endStationName("Andheri").distance(1.5).build();
            Route r3 = Route.builder().startStationName("WEH").endStationName("Chakala").distance(1.0).build();
            Route r4 = Route.builder().startStationName("Chakala").endStationName("WEH").distance(1.0).build();
            Route r5 = Route.builder().startStationName("Chakala").endStationName("Airport Road").distance(0.8).build();
            Route r6 = Route.builder().startStationName("Airport Road").endStationName("Chakala").distance(0.8).build();
            Route r7 = Route.builder().startStationName("Airport Road").endStationName("Marol Naka").distance(0.7).build();
            Route r8 = Route.builder().startStationName("Marol Naka").endStationName("Airport Road").distance(0.7).build();
            Route r9 = Route.builder().startStationName("Marol Naka").endStationName("Saki Naka").distance(1.1).build();
            Route r10 = Route.builder().startStationName("Saki Naka").endStationName("Marol Naka").distance(1.1).build();
            Route r11 = Route.builder().startStationName("Saki Naka").endStationName("Asalpha").distance(1.2).build();
            Route r12 = Route.builder().startStationName("Asalpha").endStationName("Saki Naka").distance(1.2).build();
            Route r13 = Route.builder().startStationName("Asalpha").endStationName("Jagruti Nagar").distance(1.0).build();
            Route r14 = Route.builder().startStationName("Jagruti Nagar").endStationName("Asalpha").distance(1.0).build();
            Route r15 = Route.builder().startStationName("Jagruti Nagar").endStationName("Ghatkopar").distance(1.3).build();
            Route r16 = Route.builder().startStationName("Ghatkopar").endStationName("Jagruti Nagar").distance(1.3).build();

            // Save routes to the database
            List<Route> routes = List.of(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, r16);
            routeRepository.saveAll(routes);
            log.info("Saved {} mock routes.", routes.size());

        } else {
            log.info("Station data already exists. Skipping mock data initialization.");
        }
    }
}