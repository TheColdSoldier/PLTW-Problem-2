import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmazonScraper {
    public static void main(String[] args) {
        // Path to your ChromeDriver
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Ashish Bhogasamudram\\Downloads\\test\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        // Create a WebDriver instance
        WebDriver driver = new ChromeDriver();

        try {
            // Navigate to the Amazon product page
            driver.get("https://www.amazon.com/AMD-Ryzen-7800X3D-16-Thread-Processor/dp/B0BTZB7F88/ref=sr_1_1?sr=8-1");

            // Wait for reviews to load
            Thread.sleep(3000);

            // Scroll down to load more reviews (repeat as needed for all reviews)
            for (int i = 0; i < 5; i++) {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.scrollBy(0,1000)");
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

            // Open a file writer to save the data
            try (FileWriter writer = new FileWriter("data/socialMediaPosts.txt")) {
                for (int i = 0; i < reviews.size(); i++) {
                    String reviewerName = reviewers.size() > i ? reviewers.get(i).text() : "N/A";
                    String reviewContent = reviews.size() > i ? reviews.get(i).text() : "N/A";
                    String starRating = stars.size() > i ? stars.get(i).text() : "N/A";

                    writer.write("\"" + reviewerName + "\", \"" + reviewContent + "\", \"" + starRating + "\"\n");
                }
            }

            System.out.println("Data scraped successfully and saved to data/socialMediaPosts.txt.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the WebDriver
            driver.quit();
        }
    }
}
