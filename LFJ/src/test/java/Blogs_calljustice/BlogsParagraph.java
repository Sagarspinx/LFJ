package Blogs_calljustice;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;

public class BlogsParagraph {
    public static void main(String[] args) throws InterruptedException {
        // Setup WebDriver with options
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        // Read URLs from Excel file
        List<String[]> urls = readExcelData(".//Excel//Blogs_calljustice.xlsx");
        List<String[]> reportData = new ArrayList<>();
        reportData.add(new String[]{"Live URL", "Dev URL", "Live Content", "Status"});

        for (String[] urlPair : urls) {
            String liveUrl = urlPair[0];
            String devUrl = urlPair[1];
            System.out.println("\n🔎 Checking content from Live: " + liveUrl + " on Dev: " + devUrl);

            List<String> liveContent = extractContent(driver, liveUrl);
            checkContentInDev(driver, devUrl, liveContent, liveUrl, reportData);
        }

        // Save results to Excel
        saveResultsToExcel(reportData);
        driver.quit();
    }

    public static List<String[]> readExcelData(String filePath) {
        List<String[]> urlPairs = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header
                Cell liveUrlCell = row.getCell(0);
                Cell devUrlCell = row.getCell(1);
                if (liveUrlCell != null && devUrlCell != null) {
                    urlPairs.add(new String[]{liveUrlCell.getStringCellValue(), devUrlCell.getStringCellValue()});
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error reading Excel file: " + e.getMessage());
        }
        return urlPairs;
    }

    public static List<String> extractContent(WebDriver driver, String url) throws InterruptedException {
        driver.get(url);
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollBy(0,950)");
        Thread.sleep(1000);

        List<String> content = new ArrayList<>();
        List<String> xpaths = Arrays.asList(
                "//*[@class='single-post__content aos-init aos-animate']/p",
                "//*[@class='single-post__content aos-init aos-animate']//li", "//*[@class='single-post__content aos-init aos-animate']//em",
                "//*[@class='single-post__content aos-init aos-animate']//h1", "//*[@class='single-post__content aos-init aos-animate']//h2",
                "//*[@class='single-post__content aos-init aos-animate']//h3", "//*[@class='single-post__content aos-init aos-animate']//h4"
        );

        for (String xpath : xpaths) {
            List<WebElement> elements = driver.findElements(By.xpath(xpath));
            for (WebElement element : elements) {
                content.add(cleanText(element.getText()));
            }
        }
        return content;
    }

    public static void checkContentInDev(WebDriver driver, String devUrl, List<String> liveContent, String liveUrl, List<String[]> reportData) throws InterruptedException {
        driver.get(devUrl);
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollBy(0,950)");
        Thread.sleep(1000);
        String devPageText = cleanText(driver.getPageSource());

        for (String liveItem : liveContent) {
            if (devPageText.contains(liveItem)) {
                System.out.println("✅ Found content in Dev: " + liveItem);
                reportData.add(new String[]{liveUrl, devUrl, liveItem, "Found"});
            } else {
                System.out.println("❌ Content from Live NOT found in Dev:");
                System.out.println("   Missing Content: " + liveItem);
                reportData.add(new String[]{liveUrl, devUrl, liveItem, "Not Found"});
            }
        }
    }

    public static String cleanText(String text) {
        if (text == null) return "";
        Document doc = Jsoup.parse(text);
        return doc.text().replaceAll("\\s+", " ").trim().toLowerCase();
    }

    public static void saveResultsToExcel(List<String[]> data) {
        if (data.size() <= 1) {
            System.out.println("⚠️ No data to write in the Excel file. Check if content is being found.");
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

            File file = new File(".//Excel//Blogs_calljusticeVerificationReport.xlsx");
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }

            System.out.println("📊 Results successfully saved to: " + file.getCanonicalPath());
        } catch (IOException e) {
            System.out.println("⚠️ Error saving Excel file: " + e.getMessage());
        }
    }
}
