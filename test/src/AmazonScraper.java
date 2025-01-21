import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.JavascriptExecutor;

import java.io.*;
import java.util.*;

public class AmazonScraper {
    private static Map<String, Double> loadTargetedWords(String filename) throws IOException {
        Map<String, Double> targetedWords = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    targetedWords.put(parts[0].trim().toLowerCase(), Double.parseDouble(parts[1].trim()));
                }
            }
        }
        return targetedWords;
    }

    private static String loadAdvertisement(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder ad = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                ad.append(line).append("\n");
            }
            return ad.toString();
        }
    }

    private static double calculateReviewScore(String review, Map<String, Double> targetedWords) {
        double score = 0;
        String[] words = review.toLowerCase().split("\\s+");
        for (String word : words) {
            if (targetedWords.containsKey(word)) {
                score += targetedWords.get(word);
            }
        }
        return score;
    }

    private static void writeTargetMarket(String filename, Map<String, Double> reviewerScores, 
                                        double threshold) throws IOException {
        // Sort reviewers by score
        List<Map.Entry<String, Double>> sortedReviewers = new ArrayList<>(reviewerScores.entrySet());
        sortedReviewers.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        // Write to target market file
        try (FileWriter writer = new FileWriter(filename)) {
            for (Map.Entry<String, Double> entry : sortedReviewers) {
                if (entry.getValue() >= threshold) {
                    writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                }
            }
        }
    }

    public static void main(String[] args) {
        // Path to your ChromeDriver
        System.setProperty("webdriver.chrome.driver", "test\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        // Create a WebDriver instance
        WebDriver driver = new ChromeDriver();

        try {
            // Load targeted words with weights
            Map<String, Double> targetedWords = loadTargetedWords("test\\data\\targetedwords.txt");
            String advertisement = loadAdvertisement("test\\data\\advertisement.txt");

            // Navigate to the Amazon product page
            driver.get("https://www.amazon.com/Agilestic-Electric-Standing-Adjustable-Ergonomic/dp/B0CSPJR8W8/ref=sr_1_5?crid=3B28RDXT8H5MO&dib=eyJ2IjoiMSJ9.dbhsoETAYVs6DQEFCAECaJ2iNhL9sHNm4JDX33GsFvf1Qqezd29yHvlHMEsnKBhgoaS9IMM3j---5qmNqbXcn1U7gnfRmmmF1TfcGVDGMM7c5w6vZDsyN7axLFdP7lJsmAuRNZ7KvCw8ZxIsP1O4ks0esBr1GryXexn1VYyuxtoMLTIeVhTka1D3gWbKZtcie_CLJmT6KrZTOXGq4hgcdJul5_qPApfCm0Cxmw5NyhHy2uR_pmlcdLrXIwIE1ufutiR6pJ-LZ7TuUJ8_GLQYHDva2dWnBB5z6jFGnexFuMk43hZ_9IaA2-fP6Ynza1kjjBg37S6oJpmpoMuvF_bk4GobvxSslgJYRjbnzEeVPM-xVjc8zaUfoTH7IqXhLBKXvMAcJkiLy8neeEl7HYQ5E-hmMntSKujP86gbWbqLzqjSKPINNlkPNpapROFUHIvX.C922NMmUiNb-n1bkbWSB_3iVAg20EwYbh1fI2kqhUJs&dib_tag=se&keywords=desk&qid=1736874754&sprefix=desk%2Caps%2C138&sr=8-5&th=1");

            // Wait for reviews to load
            Thread.sleep(3000);

            // Scroll down to load more reviews (repeat as needed for all reviews)
            for (int i = 0; i < 5; i++) {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0,1000)");
                Thread.sleep(1000);
            }

            // Get the page source
            String pageSource = driver.getPageSource();

            // Parse the page source with Jsoup
            Document doc = Jsoup.parse(pageSource);

            // Extract the review data
            Elements reviewers = doc.select(".a-profile-name");
            Elements reviews = doc.select(".review-text-content span");
            Elements stars = doc.select(".review-rating");

            // Store reviews and calculate scores
            Map<String, Double> reviewerScores = new HashMap<>();
            Set<String> processedReviewers = new HashSet<>(); // For duplicate checking

            // Open a file writer to save the data
            try (FileWriter writer = new FileWriter("test\\data\\socialMediaPosts.txt")) {
                for (int i = 0; i < reviews.size(); i++) {
                    String reviewerName = reviewers.size() > i ? reviewers.get(i).text() : "N/A";
                    String reviewContent = reviews.size() > i ? reviews.get(i).text() : "N/A";
                    String starRating = stars.size() > i ? stars.get(i).text() : "N/A";

                    // Check for duplicates
                    if (!processedReviewers.contains(reviewerName)) {
                        processedReviewers.add(reviewerName);
                        double score = calculateReviewScore(reviewContent, targetedWords);
                        reviewerScores.put(reviewerName, score);

                        writer.write(String.format("\"%s\",\"%s\",\"%s\",%.2f\n", 
                            reviewerName, reviewContent, starRating, score));
                    }
                }
            }

            // Write target market file
            writeTargetMarket("test\\data\\targetmarket.txt", reviewerScores, 2.0); // threshold of 2.0

            System.out.println("Data processing completed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the WebDriver
            driver.quit();
        }
    }
}
