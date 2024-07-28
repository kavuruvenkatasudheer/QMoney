
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.portfolio.PortfolioManagerImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {


  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
      
       String apiUrl = buildApiUrl(symbol, from, to);

        // Make a GET request to the Tiingo API and retrieve the response
        String response = restTemplate.getForObject(apiUrl, String.class);

        // Parse the response and convert it into a list of Candle objects
        List<Candle> candles = parseResponse(response);

        return candles;
  }
  private String buildApiUrl(String symbol, LocalDate startDate, LocalDate endDate) {
    String apiKey = "60b551d23b82e0bc314a2286cc0f87f87bccad7a";
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/{symbol}/prices?" +
            "startDate={startDate}&endDate={endDate}&token={apiKey}";

    return uriTemplate.replace("{symbol}", symbol)
            .replace("{startDate}", startDate.toString())
            .replace("{endDate}", endDate.toString())
            .replace("{apiKey}", apiKey);
  }
  
            private List<Candle> parseResponse(String response) {
              ObjectMapper objectMapper = new ObjectMapper();
              // Register the JavaTimeModule to handle Java 8 date/time types
              objectMapper.registerModule(new JavaTimeModule());
              try {
                  // Parse the JSON response into a list of Candle objects
                  List<TiingoCandle> tiingoCandles = objectMapper.readValue(response, new TypeReference<List<TiingoCandle>>() {});
        
                  // Convert the list of TiingoCandle objects to a list of Candle objects
                  List<Candle> candles = new ArrayList<>(tiingoCandles);
                  return candles;
              } catch (IOException e) {
                  e.printStackTrace();
                  // Return an empty list or handle the exception as needed
                  return List.of();
              }       
            }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

}
