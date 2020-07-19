import java.io.File;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javafx.util.Pair;
import lombok.Builder;
import lombok.Data;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class MainClass {

  public static DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
  public String lineSeparator = ";;;;;;;;;;;;;;;;;;";
  public String wordSeparator = "------------------";

  public static void main(String[] args) {

    new MainClass().doWork();
  }

  private void doWork() {

    String pathToFile = "D:\\card.pdf";
    LinkedList<ItemRecord> itemRecords = parsingFile(pathToFile);
    parsingRecords(itemRecords);
  }

  private LinkedList<ItemRecord> parsingFile(String pathToFile) {
    LinkedList<ItemRecord> itemRecords = new LinkedList<>();

    File file = new File(pathToFile);
    if (!file.exists()) {
      return itemRecords;
    }
    PDDocument pdDocument;
    PDFTextStripper pdfStripper;
    try {
      pdDocument = PDDocument.load(file);
      pdfStripper = new PDFTextStripper();
    } catch (IOException e) {
      e.printStackTrace();
      return itemRecords;
    }
    pdfStripper.setLineSeparator(lineSeparator);
    pdfStripper.setWordSeparator(wordSeparator);
    int pageStart = 0;
    int pageEnd = Math.min(pdDocument.getNumberOfPages(), Integer.MAX_VALUE);

    for (int page = pageStart; page <= pageEnd; page++) {
      System.out.println("page = " + page);
      LinkedList<ItemRecord> recordsByPage = getPossibleRecordsByPage(page, pdfStripper, pdDocument);
      itemRecords.addAll(recordsByPage);
    }
    return itemRecords;
  }

  private LinkedList<ItemRecord> getPossibleRecordsByPage(int pageNumber, PDFTextStripper pdfStripper,
      PDDocument pdDocument) {
    String documentText;
    pdfStripper.setEndPage(pageNumber);
    pdfStripper.setStartPage(pageNumber);
    try {
      documentText = pdfStripper.getText(pdDocument);
    } catch (IOException e) {
      e.printStackTrace();
      return new LinkedList<>();
    }
    String[] split = documentText.split("счета \\(RUR\\)");
    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(split));
    arrayList.remove(0);
    return getRecords(arrayList);
  }

  private void parsingRecords(LinkedList<ItemRecord> arrayList) {

//    Set<String> names = new HashSet<>();
    Map<String, Double> prices = new HashMap<>();

    for (ItemRecord itemRecord : arrayList) {
      System.out.println("itemRecord = " + itemRecord);
//      names.add(itemRecord.desk.toLowerCase());
      prices.putIfAbsent(itemRecord.desk.toLowerCase(), 0.0);
      prices.computeIfPresent(itemRecord.desk.toLowerCase(),
          (s, aDouble) -> aDouble + getDoubleFromString(itemRecord.price));
    }

    List<Pair<String, Double>> pairs = new ArrayList<>();
    for (String s : prices.keySet()) {
      Double price = prices.get(s);
      pairs.add(new Pair<>(s, price));
    }

    pairs.sort(Comparator.comparingDouble(Pair::getValue));

    for (Pair<String, Double> pair : pairs) {
      System.out.println("pair = " + pair);
    }

  }

  public LinkedList<ItemRecord> getRecords(ArrayList<String> arrayList) {

    List<String> sentences = new ArrayList<>();
    for (String s : arrayList) {
      String[] parts = s.split(lineSeparator);
      for (String part : parts) {
        if (!part.isEmpty() && !part.isBlank()) {
          sentences.add(part);
        }
      }
    }

    int badSentencePoint = sentences.size();
    for (int i = 0; i < sentences.size(); i++) {
      String sentence = sentences.get(i);
      sentence = sentence.strip();
      if (sentence.equals("ДОПОЛНИТЕЛЬНЫЕ КАРТЫ")) {
        badSentencePoint = i;
        break;
      }

      if (sentence.contains("XXXX")) {
        badSentencePoint = i;
        break;
      }

//      if (sentence.equals("По дополнительной карте: 2202 20XX XXXX 7859")) {
//        badSentencePoint = i;
//        break;
//      }
    }
    sentences = sentences.subList(0, badSentencePoint);

    Stack<Integer> integerStack = new Stack<>();

    for (int i = 0; i < sentences.size(); i++) {
      String sentence = sentences.get(i);
      String[] words = sentence.split(wordSeparator);
      if (words.length < 2) {
        continue;
      }

      if (isCorrectDate(words[0], formatter) && isCorrectDate(words[1], formatter)) {
        integerStack.push(i);
      }
    }
    integerStack.push(sentences.size());

    List<List<String>> goodSentences = new ArrayList<>();

    while (integerStack.size() > 1) {
      int secondPoint = integerStack.pop();
      int firstPoint = integerStack.peek();

      int size = secondPoint - firstPoint;
      if (size == 1) {
        goodSentences.add(new ArrayList<>(Arrays.asList(sentences.get(firstPoint).split(wordSeparator))));
      } else {
        List<String> localArrayListStrings = new ArrayList<>(
            Arrays.asList(sentences.get(firstPoint).split(wordSeparator)));
        for (int i = firstPoint + 1; i < secondPoint; i++) {
          localArrayListStrings.add(sentences.get(i));
        }
        goodSentences.add(localArrayListStrings);
      }
    }

    LinkedList<ItemRecord> result = new LinkedList<>();
    for (List<String> goodSentence : goodSentences) {

      StringBuilder descStringBuilder = new StringBuilder();

      for (int i = 3; i < goodSentence.size() - 1; i++) {
        descStringBuilder.append(goodSentence.get(i)).append(" ");
      }

      String price = goodSentence.get(goodSentence.size() - 1);
      price = price.strip();

      String desk = descStringBuilder.toString();

      ItemRecord currentRecord = ItemRecord.builder().firstDate(goodSentence.get(0)).secondDate(goodSentence.get(1))
          .idTransaction(goodSentence.get(2)).desk(desk).price(price).build();
      result.add(currentRecord);
    }

    for (ItemRecord itemRecord : result) {
      if (itemRecord.getFirstDate() != null && itemRecord.getSecondDate() != null && itemRecord.getPrice() != null
          && itemRecord.getIdTransaction() != null) {
        continue;
      }
      System.out.println("itemRecord = " + itemRecord);
    }
    return result;
  }

  @Data
  @Builder(toBuilder = true)
  public static class ItemRecord {

    String firstDate;
    String secondDate;
    String idTransaction;
    String desk;
    String price;
  }

  public boolean isCorrectDate(String date, DateTimeFormatter formatter) {
    try {
      formatter.parseDateTime(date);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public boolean isStringDouble(String date) {

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
    symbols.setDecimalSeparator(',');
    symbols.setGroupingSeparator(' ');
    date = date.replaceAll(" ", "");
    date = date.replaceAll(",", ".");
    try {
      Double.parseDouble(date);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public double getDoubleFromString(String date) {

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
    symbols.setDecimalSeparator(',');
    symbols.setGroupingSeparator(' ');
    date = date.replaceAll(" ", "");
    date = date.replaceAll(",", ".");
    if (!date.startsWith("+")) {
      date = "-" + date;
    }
    try {
      return Double.parseDouble(date);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

}
