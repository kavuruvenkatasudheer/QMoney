
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.springframework.http.ResponseEntity;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  static RestTemplate restTemplate = new RestTemplate();

  //-------------------------------------------------------------------
  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    List<String> symbols = new ArrayList<>();
    if (args.length == 0) {
      return symbols;
    }

    String filename = args[0];
    ObjectMapper objectMapper = new ObjectMapper();
    File file = resolveFileFromResources(filename);

    // Read JSON data from file and extract symbols
    JsonNode rootNode = objectMapper.readTree(file);

    for (JsonNode node : rootNode) {
      String symbol = node.get("symbol").asText();
      symbols.add(symbol);
    }

    return symbols;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }
//--------------------------------------------------------------
  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI())
        .toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
  //-----------------------------------------------------

  public static List<String> debugOutputs() {
    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "trades.json";
    String toStringOfObjectMapper = "ObjectMapper";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "";
    return Arrays.asList(
        new String[] {valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
            functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace});
  }

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.
  // and deserialize the results in List<Candle>
  //private static final String API_TOKEN = "60b551d23b82e0bc314a2286cc0f87f87bccad7a";
  private static final String API_TOKEN ="fa8e70e333f5976efcc6705003b43d81e8df581e";
 // private static final String API_TOKEN="048ec9530b4dc76782ac3cac092bb9f153c73c4a";
 //private static final String API_TOKEN= "e838fbe509441e3f8696593616a718b26b2b943e";

 //---------------------------------------------------------------------
  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    String filename = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);

    List<PortfolioTrade> trades = readTradesFromJson(filename);

    for (PortfolioTrade trade : trades) {
      if (endDate.isBefore(trade.getPurchaseDate())) {
        throw new RuntimeException("End date is before the purchase date of a trade.");
      }
    }
    // Map to store closing prices mapped to stock symbols
    Map<String, Double> closingPricesMap = new HashMap<>();

    RestTemplate restTemplate = new RestTemplate();

    for (PortfolioTrade trade : trades) {
      String apiUrl = prepareUrl(trade, endDate, API_TOKEN);
      ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);
      String responseBody = responseEntity.getBody();

      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode root = objectMapper.readTree(responseBody);

      // Assuming that the API returns an array of JSON objects containing "close" field
      for (JsonNode node : root) {
        double closingPrice = node.get("close").asDouble();
        closingPricesMap.put(trade.getSymbol(), closingPrice);
      }
    }
    List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(closingPricesMap.entrySet());
    sortedEntries.sort(Map.Entry.comparingByValue());

    // Extract sorted stock symbols
    List<String> sortedStockList = new ArrayList<>();
    for (Map.Entry<String, Double> entry : sortedEntries) {
      sortedStockList.add(entry.getKey());
    }

    return sortedStockList;
  }
//---------------------------------------------------------------
  public static List<PortfolioTrade> readTradesFromJson(String filename)
      throws IOException, URISyntaxException {
    ObjectMapper objectMapper = getObjectMapper();
    File file = resolveFileFromResources(filename);

    // Read JSON data from file
    JsonNode rootNode = objectMapper.readTree(file);

    // Extract PortfolioTrade objects from JSON
    List<PortfolioTrade> trades = new ArrayList<>();
    for (JsonNode tradeNode : rootNode) {
      PortfolioTrade trade = objectMapper.treeToValue(tradeNode, PortfolioTrade.class);
      trades.add(trade);
    }
    return trades;
  }


  // TODO:
  // Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    String stock = trade.getSymbol();
    String url = "https://api.tiingo.com/tiingo/daily/" + stock + "/prices?startDate="
        + trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;
    return url;
  }

  private static String prepareUrl(String symbol, String startDate, String endDate, String token) {
    return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + startDate
        + "&endDate=" + endDate + "&token=" + token;
  }

  // TODO:
  // Ensure all tests are passing using below command
  // ./gradlew test --tests ModuleThreeRefactorTest
  public static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    if (candles.isEmpty()) {
      return null; // or throw an exception, depending on your requirements
    }
    return candles.get(0).getOpen(); // Assuming candles are sorted by date
  }

  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    if (candles.isEmpty()) {
      return null; // or throw an exception, depending on your requirements
    }
    return candles.get(candles.size() - 1).getClose(); // Assuming candles are sorted by date
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    List<Candle> candles = new ArrayList<>();

    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = getObjectMapper();

    // Format dates
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String startDateStr = trade.getPurchaseDate().format(formatter);
    String endDateStr = endDate.format(formatter);

    // Build API URL
    String apiUrl = prepareUrl(trade.getSymbol(), startDateStr, endDateStr, token);

    // Call API
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);
    String responseBody = responseEntity.getBody();

    // Parse JSON response
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      for (JsonNode node : root) {
        Candle candle = parseCandle(node);
        candles.add(candle);
      }
    } catch (Exception e) {
      e.printStackTrace(); // Handle parsing exception
    }

    return candles;
  }

  // Method to parse JSON node into Candle object
  private static Candle parseCandle(JsonNode node) {
    // Assuming JSON node structure matches Candle interface
    Double open = node.get("open").asDouble();
    Double close = node.get("close").asDouble();
    Double high = node.get("high").asDouble();
    Double low = node.get("low").asDouble();
    String dateString = node.get("date").asText().split("T")[0];
    LocalDate date = LocalDate.parse(dateString);

    return new TiingoCandle(open, close, high, low, date);
  }

  public static Double getOpeningPriceOnStartDate(String symbol, LocalDate startDate) {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();

    // Format the start date
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String startDateStr = startDate.format(formatter);

    // Build the API URL
    String apiUrl = prepareUrl(symbol, startDateStr, startDateStr, API_TOKEN);

    // Call the Tiingo API to get the opening price on start date
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);
    String responseBody = responseEntity.getBody();

    try {
      // Parse the JSON response
      JsonNode root = objectMapper.readTree(responseBody);
      double openingPrice = root.get(0).get("open").asDouble(); // Assuming the API returns an array
      return openingPrice;
    } catch (Exception e) {
      e.printStackTrace(); // Handle parsing exception
      return null;
    }
  }

  public static Double getClosingPriceOnEndDate(String symbol, LocalDate endDate) {
    
    ObjectMapper objectMapper = new ObjectMapper();

    // Format the end date
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String endDateStr = endDate.format(formatter);

    // Build the API URL to fetch closing prices
    String apiUrl = prepareUrl(symbol, endDateStr, endDateStr, API_TOKEN);

    // Call the Tiingo API to get the closing price on the specified end date
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);
    String responseBody = responseEntity.getBody();

    try {
      // Parse the JSON response
      JsonNode root = objectMapper.readTree(responseBody);

      // Check if closing price is available for the specified end date
      if (root.size() > 0) {
        double closingPrice = root.get(0).get("close").asDouble();
        return closingPrice;
      } else {
        // If closing price is not available for specified end date, find the last available closing
        // price before the end date
        LocalDate lastTradingDate = findLastTradingDate(symbol, endDate);
        if (lastTradingDate != null) {
          String lastTradingDateStr = lastTradingDate.format(formatter);
          apiUrl = prepareUrl(symbol, lastTradingDateStr, lastTradingDateStr, API_TOKEN);

          responseEntity = restTemplate.getForEntity(apiUrl, String.class);
          responseBody = responseEntity.getBody();

          root = objectMapper.readTree(responseBody);
          if (root.size() > 0) {
            double closingPrice = root.get(0).get("close").asDouble();
            return closingPrice;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace(); // Handle parsing exception
    }

    return null;
  }

  private static LocalDate findLastTradingDate(String symbol, LocalDate endDate) {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();

    LocalDate currentDate = endDate.minusDays(1); // Start from the day before the end date

    while (currentDate.isAfter(LocalDate.of(1970, 1, 1))) { // Assuming Tiingo data is available
                                                            // since 1970
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      String currentDateStr = currentDate.format(formatter);

      // Build the API URL to fetch closing prices for the current date
      String apiUrl = prepareUrl(symbol, currentDateStr, currentDateStr, API_TOKEN);

      // Call the Tiingo API to get the closing price for the current date
      ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);
      String responseBody = responseEntity.getBody();

      try {
        // Parse the JSON response
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.size() > 0) {
          // If closing price is available for the current date, return the current date
          return currentDate;
        }
      } catch (Exception e) {
        e.printStackTrace(); // Handle parsing exception
      }

      // Move to the previous date
      currentDate = currentDate.minusDays(1);
    }

    return null; // Return null if no trading date is found before the end date
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double totalReturn = (sellPrice - buyPrice) / buyPrice;

    // Calculate time elapsed in years
    long daysBetween = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS);
    double yearsElapsed = daysBetween / 365.0; // Assuming 365 days in a year

    // Calculate annualized return
    double annualizedReturn = Math.pow(1 + totalReturn, 1 / yearsElapsed) - 1;

    // Create and return AnnualizedReturn object
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
    // return new AnnualizedReturn("", 0.0, 0.0);
  }

  private static PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    List<PortfolioTrade> portfolioTrades = readTradesFromFile(file);
    return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }

  private static List<PortfolioTrade> readTradesFromFile(String filePath) throws IOException {
    String contents = readFileAsString(filePath);
    ObjectMapper objectMapper = getObjectMapper();
    return Arrays.asList(objectMapper.readValue(contents, PortfolioTrade[].class));
  }

  private static String readFileAsString(String filePath) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filePath)));
  }


  public static String getToken() {
    return API_TOKEN;
  }


  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws RuntimeException, IOException, URISyntaxException {
    String filename = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);

    // Read portfolio trades from JSON file and sort them
    List<PortfolioTrade> trades = readTradesFromJson(filename);

    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    for (PortfolioTrade trade : trades) {
      Double buyPrice = getOpeningPriceOnStartDate(trade.getSymbol(), trade.getPurchaseDate());
      Double sellPrice = getClosingPriceOnEndDate(trade.getSymbol(), endDate);
      AnnualizedReturn annualizedReturn =
          calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
      annualizedReturns.add(annualizedReturn);
    }
    Collections.sort(annualizedReturns,
        (a1, a2) -> Double.compare(a2.getAnnualizedReturn(), a1.getAnnualizedReturn()));
    return annualizedReturns;
  }
  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    // Step 1: Resolve File Path
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);

    // Step 2: Read portfolio trades from JSON file
    List<PortfolioTrade> portfolioTrades = readTradesFromJson(file);

    // Step 3: Create a RestTemplate instance
    RestTemplate restTemplate = new RestTemplate();

    // Step 4: Create a PortfolioManager instance using PortfolioManagerFactory
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);

    // Step 5: Calculate annualized returns using PortfolioManager
    List<AnnualizedReturn> annualizedReturns =
        portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);

    // Step 6: Print the result
    printJsonObject(annualizedReturns);

  }


}

