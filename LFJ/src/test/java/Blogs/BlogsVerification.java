package Blogs;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class BlogsVerification {
    public static void main(String[] args) {
        // Setup WebDriver with options
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        // Define multiple URLs with XPaths for extracting content from Live site
        Map<String, String[]> urls = new HashMap<>();
        urls.put("https://staging.mrjustice.com/blog/t-bone-accident-whos-at-fault/", new String[]{"//*[@class='article-wrap']//p", "https://php2.spinxweb.net/lawyers-for-justice/t-bone-accident-whos-at-fault/"});
        urls.put("https://staging.mrjustice.com/blog/california-contributory-negligence-law/", new String[]{"//*[@class='article-wrap']//p", "https://php2.spinxweb.net/california-contributory-negligence-law/"});
        urls.put("https://staging.mrjustice.com/blog/car-accident-lawsuit-process/", new String[]{"//*[@class='article-wrap']//p", "https://php2.spinxweb.net/lawyers-for-justice/car-accident-lawsuit-process/"});

        List<String[]> reportData = new ArrayList<>();
        reportData.add(new String[]{"Live URL", "Dev URL", "Live Paragraph", "Status"});

        for (Map.Entry<String, String[]> entry : urls.entrySet()) {
            String liveUrl = entry.getKey();
            String liveXPath = entry.getValue()[0];
            String devUrl = entry.getValue()[1];

            System.out.println("\n🔎 Checking paragraphs from Live: " + liveUrl + " on Dev: " + devUrl);
            List<String> liveParagraphs = extractParagraphs(driver, liveUrl, liveXPath);
            checkParagraphsInDev(driver, devUrl, liveParagraphs, liveUrl, reportData);
        }

        // Save results to Excel
        saveResultsToExcel(reportData);

        // Close browser
        driver.quit();
    }

    // Extract all paragraphs from Live site based on the provided XPath
    public static List<String> extractParagraphs(WebDriver driver, String url, String xpath) {
        driver.get(url);
        List<WebElement> paragraphs = driver.findElements(By.xpath(xpath));
        return paragraphs.stream()
                .map(WebElement::getText)  // Get text content
                .map(BlogsVerification::cleanText)  // Normalize text
                .collect(java.util.stream.Collectors.toList());
    }

    // Check if each paragraph from Live exists anywhere on the Dev page and store results
    public static void checkParagraphsInDev(WebDriver driver, String devUrl, List<String> liveParagraphs, String liveUrl, List<String[]> reportData) {
        driver.get(devUrl);
        String devPageText = cleanText(driver.getPageSource()); // Get full page text

        for (String liveParagraph : liveParagraphs) {
            if (devPageText.contains(liveParagraph)) {
                System.out.println("✅ Found paragraph in Dev: " + liveParagraph);
                reportData.add(new String[]{liveUrl, devUrl, liveParagraph, "Found"});
            } else {
                System.out.println("❌ Paragraph from Live NOT found in Dev:");
                System.out.println("   Missing Paragraph: " + liveParagraph);
                reportData.add(new String[]{liveUrl, devUrl, liveParagraph, "Not Found"});
                System.out.println("🔹 Debug: Adding row - " + Arrays.toString(new String[]{liveUrl, devUrl, liveParagraph, "Not Found"}));
            }
        }
    }

    // Normalize text by removing extra spaces, HTML tags, and special characters
    public static String cleanText(String text) {
        if (text == null) return "";
        Document doc = Jsoup.parse(text);
        return doc.text().replaceAll("\\s+", " ").trim().toLowerCase();
    }

    // Save results to an Excel file
    public static void saveResultsToExcel(List<String[]> data) {
        if (data.size() <= 1) { // Only headers exist, no actual data
            System.out.println("⚠️ No data to write in the Excel file. Check if paragraphs are being found.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Blog Verification Report");
            int rowNum = 0;

            for (String[] rowData : data) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < rowData.length; i++) {
                    row.createCell(i).setCellValue(rowData[i]);
                }
            }

            // Ensure file exists before saving
            File file = new File(".//Excel//BlogVerificationReport.xlsx");
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }

            System.out.println("📊 Results successfully saved to: " + file.getCanonicalPath());
        } catch (IOException e) {
            System.out.println("⚠️ Error saving Excel file: " + e.getMessage());
        }
    }
}
