package org.example;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TicketAnalyzer {
    public static void main(String[] args) throws IOException, ParseException {
        JSONArray tickets = getJsonArray(args[0]);
        Map<String, Long> minFlightTimesForCarriers = getMinFlightTimeGroupByCarrierFromTickets(tickets);

        System.out.println("Минимальное время полета по авиакомпаниям:");

        for (Map.Entry<String, Long> entry : minFlightTimesForCarriers.entrySet()) {
            System.out.println(entry.getKey() + ": " + formatDuration(entry.getValue()));
        }

        List<Long> prices = getPricesFromTickets(tickets);

        double avgPrice = prices.stream().mapToInt(Long::intValue).average().orElse(0);
        double medianPrice = getPercentileFlightTime(50, prices);
        double diff = avgPrice - medianPrice;

        System.out.println("\nРазница между средней ценой и медианой:");
        System.out.println("Средняя цена: " + avgPrice + " руб.");
        System.out.println("Медиана: " + medianPrice + " руб.");
        System.out.println("Разница: " + diff + " руб.");

    }

    public static String formatDuration(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    private static Map<String, Long> getMinFlightTimeGroupByCarrierFromTickets(JSONArray tickets) throws IOException, ParseException {
        Map<String, Long> minFlightTimes = new HashMap<>();
        for (Object ticket : tickets) {
            JSONObject ticketJsonObject = (JSONObject) ticket;
            String carrier = (String) ticketJsonObject.get("carrier");
            Duration time = getFlightTimeFromTicket(ticketJsonObject);
            if (!minFlightTimes.containsKey(carrier) || time.toMinutes() < minFlightTimes.get(carrier)) {
                minFlightTimes.put(carrier, time.toMinutes());
            }
        }
        return minFlightTimes;
    }

    private static JSONArray getJsonArray(String fileName) throws IOException, ParseException {
        List<Long> prices = new ArrayList<>();
        String jsonString = readJsonFile(fileName);
        JSONParser jsonParser = new JSONParser();
        JSONObject json = (JSONObject) jsonParser.parse(jsonString);
        return (JSONArray) json.get("tickets");
    }

    private static List<Long> getPricesFromTickets(JSONArray tickets) throws IOException, ParseException {
        List<Long> prices = new ArrayList<>();
        for (Object ticket : tickets) {
            JSONObject ticketJsonObject = (JSONObject) ticket;
            prices.add((Long) ticketJsonObject.get("price"));
        }
        return prices;
    }

    public static String readJsonFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                content.append(buffer, 0, n);
            }
        }
        return content.toString().replace("\uFEFF", "");
    }

    private static String checkTimeFormat(String time) {
        if (time.length() < 5)
            time = 0 + time;
        return time;
    }

    private static Duration getFlightTimeFromTicket(JSONObject ticket) {
        String departureTime = (String) ticket.get("departure_time");
        String departureDate = (String) ticket.get("departure_date");
        String arrivalTime = (String) ticket.get("arrival_time");
        String arrivalDate = (String) ticket.get("arrival_date");

        LocalTime dT = LocalTime.parse(checkTimeFormat(departureTime));
        LocalDate dD = parseDate(departureDate);
        LocalTime aT = LocalTime.parse(checkTimeFormat(arrivalTime));
        LocalDate aD = parseDate(arrivalDate);

        ZoneId vladivostokZone = ZoneId.of("Asia/Vladivostok");
        ZoneId telAvivZone = ZoneId.of("Asia/Jerusalem");

        ZonedDateTime departure = ZonedDateTime.of(
                dD,
                dT,
                vladivostokZone
        );

        ZonedDateTime arrival = ZonedDateTime.of(
                aD,
                aT,
                telAvivZone
        );

        return Duration.between(departure, arrival);
    }

    public static LocalDate parseDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");

        try {
            return LocalDate.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            System.err.println("Ошибка парсинга даты: " + e.getMessage());
            return null;
        }
    }

    private static long getPercentileFlightTime(int percentile, List<Long> listOfFlightTime) {
        List<Long> listOfFlightTimeSorted = listOfFlightTime.stream().sorted().toList();
        double k = listOfFlightTime.size() * percentile / 100;
        return listOfFlightTimeSorted.get((int) Math.ceil(k));
    }
}
