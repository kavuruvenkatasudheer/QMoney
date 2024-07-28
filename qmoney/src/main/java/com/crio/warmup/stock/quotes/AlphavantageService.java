
package com.crio.warmup.stock.quotes;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;


public class AlphavantageService implements StockQuotesService {


  
  private RestTemplate restTemplate;

  public AlphavantageService(RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException, StockQuoteServiceException {
    try {
      AlphavantageDailyResponse response = getResponseFromApi(symbol, from, to);
      return mapToCandles(response, from, to);
  } catch (RuntimeException th) {
      // Wrap the RestClientException in a StockQuoteServiceException
      throw new StockQuoteServiceException("Error communicating with external service", th);
  }
  }

  private AlphavantageDailyResponse getResponseFromApi(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
      String apiUrl = buildUri(symbol, from, to);
     // String quotes = restTemplate.getForObject(apiUrl, String.class);
      AlphavantageDailyResponse res=restTemplate.getForObject(apiUrl, AlphavantageDailyResponse.class);
      Map<LocalDate,AlphavantageCandle> candles=res.getCandles();
      for (Entry<LocalDate, AlphavantageCandle> entry : candles.entrySet()) {
        LocalDate date = entry.getKey();
        AlphavantageCandle candle = entry.getValue();
        System.out.println("Date: " + date);
        System.out.println("Open: " + candle.getOpen());
        System.out.println("Close: " + candle.getClose());
        System.out.println("High: " + candle.getHigh());
        System.out.println("Low: " + candle.getLow());
      
      }
      return res;
  }
 
  private List<Candle> mapToCandles(AlphavantageDailyResponse response,LocalDate startDate, LocalDate endDate) {
      List<Candle> candlesList = new ArrayList<>();
      Map<LocalDate, AlphavantageCandle> candles = response.getCandles();
      for (Entry<LocalDate, AlphavantageCandle> entry : candles.entrySet()) {
        LocalDate date = entry.getKey();
        if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
          AlphavantageCandle alphavantageCandle = entry.getValue();
          AlphavantageCandle candle = new AlphavantageCandle();
          candle.setDate(entry.getKey());
          candle.setOpen(alphavantageCandle.getOpen());
          candle.setClose(alphavantageCandle.getClose());
          candle.setHigh(alphavantageCandle.getHigh());
          candle.setLow(alphavantageCandle.getLow());
          // Add more properties if needed
          candlesList.add(candle);
        }
        candlesList.sort(Comparator.comparing(Candle::getDate));
      }
      return candlesList;
  }
      // Method to build the URI
      private String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
          String apiKey = "60b551d23b82e0bc314a2286cc0f87f87bccad7a";
          String uriTemplate = "https://api.tiingo.com/tiingo/daily/{symbol}/prices?" +
                  "startDate={startDate}&endDate={endDate}&token={apiKey}";
  
          return uriTemplate.replace("{symbol}", symbol)
                  .replace("{startDate}", startDate.toString())
                  .replace("{endDate}", endDate.toString())
                  .replace("{apiKey}", apiKey);
      }
  
  
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
  //  to fetch daily adjusted data for last 20 years.
  //  Refer to documentation here: https://www.alphavantage.co/documentation/
  //  --
  //  The implementation of this functions will be doing following tasks:
  //    1. Build the appropriate url to communicate with third-party.
  //       The url should consider startDate and endDate if it is supported by the provider.
  //    2. Perform third-party communication with the url prepared in step#1
  //    3. Map the response and convert the same to List<Candle>
  //    4. If the provider does not support startDate and endDate, then the implementation
  //       should also filter the dates based on startDate and endDate. Make sure that
  //       result contains the records for for startDate and endDate after filtering.
  //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
  //  IMP: Do remember to write readable and maintainable code, There will be few functions like
  //    Checking if given date falls within provided date range, etc.
  //    Make sure that you write Unit tests for all such functions.
  //  Note:
  //  1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
  //  2. Run the tests using command below and make sure it passes:
  //    ./gradlew test --tests AlphavantageServiceTest
  //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.

}

