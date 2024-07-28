
package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;

  public PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl() {

  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  // ------------------------------------------------------------------------
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
    List<Candle> candles = stockQuotesService.getStockQuote(symbol, from, to);
    return candles;
  }

  // ---------------------------------------------------------------------------
  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token = "60b551d23b82e0bc314a2286cc0f87f87bccad7a";
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
        + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

    String url = uriTemplate.replace("$APIKEY", token).replace("$SYMBOL", symbol)
        .replace("$STARTDATE", startDate.toString()).replace("$ENDDATE", endDate.toString());
    return url;

  }

  // -----------------------------------------------------------------------
  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    for (PortfolioTrade trade : portfolioTrades) {
      try {
        // Fetch the stock quotes for the given symbol and dates
        System.out
            .println("hi" + getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate));
        List<Candle> candles =
            (List<Candle>) getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);

        // Extract buy and sell prices from the fetched candles
        Double buyPrice = getOpeningPriceOnDate(candles, trade.getPurchaseDate());
        Double sellPrice = getCandlePriceOnDate(candles, endDate, trade.getSymbol());

        // Calculate annualized return
        if (sellPrice != null) {
          AnnualizedReturn annualizedReturn =
              calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
          annualizedReturns.add(annualizedReturn);
        } else {
          // Throw an exception indicating no data available for the symbol on the end date
          throw new NullPointerException(
              "No data available for symbol " + trade.getSymbol() + " on end date " + endDate);
        }
      } catch (Exception e) {
        // Handle JSON processing exception
        throw new RuntimeException("error in json parsing" + e);

      }
    }
    Collections.sort(annualizedReturns,
        (a1, a2) -> Double.compare(a2.getAnnualizedReturn(), a1.getAnnualizedReturn()));
    return annualizedReturns;
  }

  // ----------------------------------------------------------------
  private Double getCandlePriceOnDate(List<Candle> candles, LocalDate date, String symbol) {
    Double close = null;
    for (Candle candle : candles) {
      close = candle.getClose();
    }
    // If no candle matches the specified date, return null or throw an exception
    return close; // You can handle this case based on your requirements
  }

  // -------------------------------------------------------------
  private Double getOpeningPriceOnDate(List<Candle> candles, LocalDate date) {
    for (Candle candle : candles) {
      return candle.getOpen();
    }
    // If no candle matches the specified date, return null or throw an exception
    return null; // You can handle this case based on your requirements
  }

  // ---------------------------------------------------------------------------------------
  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double totalReturn = (sellPrice - buyPrice) / buyPrice;

    long daysBetween = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS);
    double yearsElapsed = daysBetween / 365.0;

    double annualizedReturn = Math.pow(1 + totalReturn, 1 / yearsElapsed) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws InterruptedException, StockQuoteServiceException {
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<AnnualizedReturn>> futures = new ArrayList<>();

        for (PortfolioTrade trade : portfolioTrades) {
            Callable<AnnualizedReturn> task = () -> calculateAnnualizedReturnForTrade(trade, endDate);
            Future<AnnualizedReturn> future = executorService.submit(task);
            futures.add(future);
        }

        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        for (Future<AnnualizedReturn> future : futures) {
            try {
                AnnualizedReturn annualizedReturn = future.get();
                annualizedReturns.add(annualizedReturn);
            } catch (ExecutionException e) {
                // Handle exception if any task fails
                // You might want to log the exception or handle it accordingly
                throw new StockQuoteServiceException("Error fetching stock quotes: " + e.getMessage());
            }
        }

        executorService.shutdown();
        Collections.sort(annualizedReturns,
        (a1, a2) -> Double.compare(a2.getAnnualizedReturn(), a1.getAnnualizedReturn()));
        return annualizedReturns;
    }
    private AnnualizedReturn calculateAnnualizedReturnForTrade(PortfolioTrade trade, LocalDate endDate)
            throws StockQuoteServiceException {
        try {
            List<Candle> candles = stockQuotesService.getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
            Double buyPrice = getOpeningPriceOnDate(candles, trade.getPurchaseDate());
            Double sellPrice = getCandlePriceOnDate(candles, endDate, trade.getSymbol());

            if (sellPrice != null) {
                return calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
            } else {
                throw new NullPointerException("No data available for symbol " + trade.getSymbol() + " on end date " + endDate);
            }
        } catch (JsonProcessingException e) {
            throw new StockQuoteServiceException("Error fetching stock quotes: " + e.getMessage());
        }
    }



  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  // Modify the function #getStockQuote and start delegating to calls to
  // stockQuoteService provided via newly added constructor of the class.
  // You also have a liberty to completely get rid of that function itself, however, make sure
  // that you do not delete the #getStockQuote function.

}
